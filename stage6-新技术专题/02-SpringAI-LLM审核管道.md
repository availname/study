# 02 · Spring AI：把"模拟审核"升级为真实 LLM 审核

community 项目的审核消费者一直用 `title.contains("BLOCK")` 模拟——现在是升级它的时候了。这一篇把 **LLM 真实接进 MQ 消费者**，并讲清 AI 时代 MQ/Redis 的新用法。

## 为什么 AI 审核天然需要 MQ（联动点）

LLM 调用的特征：**慢**（秒~几十秒）、**贵**（按 token 计费）、**限流**（上游 QPS 限制）。三个特征决定了它**必须异步化**：

```
用户发帖 → MQ 管道 → LLM 审核消费者（限速消费，保护配额）
                         │ 失败/超时 → 重试 → 死信 → 人工兜底
                         │ 结果 → Redis 缓存 + DB 状态
```

这正是 mq 教学的"消费限速保护下游"（stage3/04）的 AI 版：**下游从 MySQL 换成了 LLM API，方法论不变**。

## Spring AI：标准化的 LLM 接入层

**Spring AI 1.0**（2025 GA）之于 LLM，相当于 JDBC 之于数据库——统一的 ChatClient API，换模型商只改配置：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    openai:
      api-key: ${LLM_API_KEY}        # 任何 OpenAI 兼容端点都行
      base-url: https://api.deepseek.com   # 国产模型同理（DeepSeek/通义/Kimi 兼容 OpenAI 协议）
      chat:
        options:
          model: deepseek-chat
          temperature: 0.1           # 审核要稳定，temperature 调低
```

## 审核消费者升级版（结构化输出 + 降级）

```java
@Component
public class AiAuditConsumer {

    private final ChatClient chatClient;
    // Spring AI 自动装配 ChatModel，包一层成为可复用的客户端
    public AiAuditConsumer(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /** 结构化审核：不让 LLM 自由发挥，让它只输出 JSON */
    private record AuditResult(boolean pass, String reason) {}

    public AuditResult audit(String title, String content) {
        String system = """
                你是论坛内容审核员。判断内容是否违规（广告、色情、暴力、政治敏感）。
                只输出 JSON：{"pass": true/false, "reason": "原因"}
                """;
        String result = chatClient.prompt()
                .system(system)
                .user("标题: " + title + "\n内容: " + content)
                .call()
                .content();
        return parse(result);        // 容错解析（LLM 偶尔输出多余文字，见下文）
    }
}
```

**三个工程要点（比 API 调用本身更重要）**：

1. **temperature 调低（0.0~0.2）**：审核要确定性，不要"创造力"。
2. **结构化输出 + 容错解析**：LLM 可能返回 ```` ```json ```` 包裹或多余文字——解析失败**不放弃**，直接转人工审核（HUMAN_AUDIT），比反复重试 LLM 便宜可靠。
3. **两级降级**（AI 时代服务设计的模板）：

```
LLM API 正常   → AI 审核
LLM 超时/限流   → 重试 1 次（指数退避）→ 仍失败 → 转 HUMAN_AUDIT 人工队列
LLM 服务不可用  → 熔断（Resilience4j/CircuitBreaker）→ 全部转人工
```

——**AI 不稳定是常态，降级路径不是备胎而是主流程的一部分**。

## LLM 审核的 MQ/Redis 全套配套（联动集大成）

| 需求 | 方案 | 出处 |
|------|------|------|
| 审核慢（秒级），不能阻塞发帖 | MQ 异步管道（已有） | m-stage5 |
| LLM 限流保护 | 消费者 concurrency 低 + RateLimiter | m-stage3/04 |
| 相同内容重复审核浪费钱 | Redis 指纹去重（已有）+ **语义级去重见 03 篇** | r-stage5 |
| 审核结果缓存 | Redis：`ai:audit:{md5}` → 结果 JSON，TTL 7 天（相似内容直接复用判定，省钱） | r-stage1/03 |
| 调用失败可观测 | OTel Trace 记录每次 LLM 调用耗时/token（04 篇） | — |
| 成本治理 | 按天统计 token 消耗（Redis INCR），超预算自动降级人工 | — |

## 无 API Key 也能学（降级演示模式）

没有 LLM 密钥时，写一个 `MockChatModel` 实现 Spring AI 的接口，按关键词返回固定 JSON——**管道、降级、缓存、限流的代码全部真实可跑**，只换掉最后一公里的模型调用。等你有 key（DeepSeek 几块钱就能玩很久），改一行 base-url 立即变真。

## 大语言模型时代的 MQ 新形态（视野扩展）

- **流式响应 + 消息分片**：LLM 长文本生成走 SSE 流式；生成结果落库时按块投递
- **LLM 网关**：多模型路由/降级/计费统计——本质是个"智能代理层"，社区开源 LiteLLM
- **AI Agent 的任务队列**：Agent 的每个"思考步骤"作为消息入队，Worker 池消费——**MQ 是 Agent 工程化的基础设施**
- **Embedding 生成队列**：文本→向量也是 LLM 调用，批量异步生成（下一篇的向量检索原料）

## 面试问法

**Q: 系统里怎么接入 LLM 能力？要注意什么？**
A: 用 Spring AI 这类抽象层（ChatClient 统一多模型）。工程要点：①LLM 调用慢/贵/限流——必须异步化（MQ 管道+限速消费）与缓存（相同输入指纹去重）；②结构化输出（JSON+容错解析）防止自由发挥；③两级降级（重试→人工），AI 不稳定是常态；④成本治理（token 计量+预算熔断）。

---

**下一篇**：[03-Redis向量检索与语义缓存](03-Redis向量检索与语义缓存.md)——Embedding 是什么、Redis 为什么能当向量数据库。
