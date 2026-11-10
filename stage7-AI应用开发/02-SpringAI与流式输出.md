# 02 · Spring AI 与流式输出

用 Spring AI 把 LLM 接进 learn-center，并实现**打字机效果的流式输出**。

## 一、接入：依赖 + 配置（可切 Mock）

```xml
<!-- pom.xml：dependencyManagement 加 BOM，dependencies 加 starter -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    openai:
      api-key: ${LLM_API_KEY:mock}          # 无 key 时用 mock（见下方 ChatPort 降级）
      base-url: ${LLM_BASE_URL:https://api.deepseek.com}
      chat:
        options:
          model: deepseek-chat
          temperature: 0.3

lc:
  ai:
    provider: ${LC_AI_PROVIDER:mock}        # mock / real —— 降级开关
```

## 二、ChatPort：可切换的模型访问层（设计模式实操）

**为什么抽象接口**：换模型/降级/测试都不动业务代码——策略模式（设计模式 03）+ Conditional 装配（Spring 核心 05）：

```java
public interface ChatPort {
    /** 同步问答 */
    String ask(String system, String user);
    /** 流式输出（逐 token 推给前端） */
    reactor.core.publisher.Flux<String> stream(String system, String user);
}
```

```java
// 真实实现：Spring AI ChatClient
@Component
@ConditionalOnProperty(name = "lc.ai.provider", havingValue = "real")
public class RealChatPort implements ChatPort {
    private final ChatClient chatClient;
    public RealChatPort(ChatModel model) {
        this.chatClient = ChatClient.builder(model).build();
    }
    public String ask(String system, String user) {
        return chatClient.prompt().system(system).user(user).call().content();
    }
    public Flux<String> stream(String system, String user) {
        return chatClient.prompt().system(system).user(user).stream().content();
    }
}

// Mock 实现（默认）：无 Key 也全流程可跑
@Component
@ConditionalOnProperty(name = "lc.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockChatPort implements ChatPort {
    public String ask(String system, String user) {
        return "【Mock 回复】已理解问题：" + user;
    }
    public Flux<String> stream(String system, String user) {
        return Flux.just("【Mock", " 流式", " 回复】", user).delayElements(Duration.ofMillis(300));
    }
}
```

> [!TIP]
> Conditional 装配 + 接口 = **零成本切换**：改一个配置项就在 Mock/真实模型之间切换（Spring 核心 05 条件装配的实战）。

## 三、流式输出：打字机效果（SSE）

LLM 生成是逐 token 的——**流式把生成过程实时推给前端**，用户 1 秒内看到第一个字（而非等 30 秒白屏）。传输协议用 **SSE（Server-Sent Events）**：单向服务端推送，比 WebSocket 简单。

```java
@GetMapping(value = "/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> chatStream(@RequestParam String q) {
    return chatPort.stream("你是学习助手", q)
            .map(token -> ServerSentEvent.builder(token).event("message").build())
            .concatWith(Flux.just(ServerSentEvent.builder("[DONE]").event("done").build()));
}
```

前端（EventSource 或 fetch stream）逐 token 拼接渲染——ChatGPT 的打字机效果就是这个原理。

**流式三个工程点**：

1. **超时与中断**：用户关页面要取消上游调用（Flux 的 cancel 信号自动传播到 WebFlux 客户端）
2. **SSE 格式**：`data: 内容\n\n`——前端 EventSource 按事件切分
3. **Nginx 代理要关缓冲**：`proxy_buffering off`，否则流被攒成一坨一次性到达（打字机变"卡顿炮"）

## 四、结构化输出：让 LLM 输出 JSON

自由文本不可靠——Spring AI 的 `entity()` 自动按 schema 解析：

```java
public record AuditResult(boolean pass, String reason) {}

AuditResult result = chatClient.prompt()
        .system("你是内容审核员。只输出 JSON：{\"pass\":布尔,\"reason\":\"原因\"}")
        .user(content)
        .call()
        .entity(AuditResult.class);        // ← 自动反序列化（内部走 JSON schema/转换+校验）
```

**兜底**：解析失败（LLM 输出非法 JSON）时——重试一次 → 仍失败转人工/降级，**不要无限重试**（stage7/04 详述）。

## 面试问法

**Q: 流式输出怎么实现？**
A: SSE（Server-Sent Events）：响应头 `text/event-stream`，服务端逐 token 推 `data:` 事件，前端 EventSource/fetch stream 拼接渲染。Spring AI 的 `.stream()` 返回 Flux<String>，逐 token 从上游模型 API 透传。工程点：Nginx 关 `proxy_buffering`、客户端断开要取消上游调用（防白烧 token）、[DONE] 事件标记结束。

**Q: LLM 输出不稳定怎么保证结构化？**
A: 三层——①提示词明确要求只输出 JSON + few-shot 示例；②Spring AI `entity()` 按类反序列化（内部自动生成 JSON schema 约束）；③解析失败兜底（重试一次→降级人工/默认值）。**永远不要信任 LLM 的输出格式，解析失败是常态路径**。

---

**下一篇**：[03-RAG与向量检索](03-RAG与向量检索.md)——让 AI"开卷考试"。
