# 04 · AI 工程化：成本、降级、评测与 learn-center 三功能落地

把 LLM 从"能调通"变成"能上生产"——本章是 stage7 的落地篇：learn-center 新增三个 AI 功能（AI 答疑 / AI 出题 / 学习报告），全部代码在 `learn-center/src/main/java/com/example/learn/ai/`。

## learn-center 的三个 AI 功能

| 功能 | 输入 | 输出 | 模式 |
|------|------|------|------|
| **AI 答疑** | 题目 ID + 追问 | SSE 流式回答（带题目上下文） | Chat 流式 |
| **AI 出题** | 分类 + 难度 | 结构化 JSON → **入库题库**（自动扩充题库！） | 结构化输出 |
| **学习报告** | 用户学习数据 | 个性化 Markdown 报告 | 数据聚合 + 总结 |

```
用户追问 "这道题能举个生产例子吗"
  → AiService 检索题目上下文（题干+答题要点）
  → 拼 RAG 提示词 → ChatPort（mock/real 可切换）
  → SSE 流式返回给前端
```

## ChatPort：Mock / Real 双实现（零 Key 全流程可跑）

```java
public interface ChatPort {
    String ask(String system, String user);
    reactor.core.publisher.Flux<String> stream(String system, String user);
}

// mock（默认）：固定文本分片流式返回——管道/SSE/前端全流程可验证
// real：Spring AI ChatClient 走配置的模型端点
```

配置：`lc.ai.provider = mock|real`（Conditional 装配，Spring 核心 05）。

## AI 答疑：RAG 简化版（题目上下文检索）

```java
@GetMapping(value = "/answer/{questionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> answer(@PathVariable long questionId,
                                            @RequestParam(defaultValue = "") String followUp) {
    Question q = questionMapper.selectById(questionId);
    if (q == null) return errorStream("题目不存在");

    String system = """
            你是 Java 后端学习助教。基于【题目】和【参考要点】回答学生追问；
            要点之外的扩展可以讲，但不要编造不存在的 API 或结论。
            """;
    String user = """
            【题目】%s
            【参考要点】%s
            【学生追问】%s
            """.formatted(q.title(), q.answerMd, followUp);
    return chatPort.stream(system, user)
            .map(token -> ServerSentEvent.builder(token).event("message").build());
}
```

## AI 出题：结构化输出自动扩题库（元闭环）

```java
public record GeneratedQuestion(String category, String title, String answerMd, int difficulty) {}

public List<GeneratedQuestion> generate(String category, int count) {
    String result = chatClient.prompt()
        .system("""
            你是 Java 后端面试官。生成 %d 道「%s」分类的面试题。
            只输出 JSON 数组，元素结构：{"category":"...","title":"...","answerMd":"要点"}
            """.formatted(count, category))
        .user("开始生成")
        .call()
        .content();
    // 解析 JSON 数组 → 逐条去重入库（title+category 唯一判断）
    // ⚠ 解析失败兜底：重试一次 → 失败返回错误（LLM 输出格式不可全信，stage7/02）
}
```

**元闭环价值**：题库自动扩充 → 刷题系统永不过时 → AI 消费自己生成的题继续学——**学习平台 + AI 的飞轮**。

## 学习报告：数据聚合 + LLM 总结

```java
public String report(long userId) {
    // 聚合真实学习数据（Redis + MySQL）
    long signDays = 签到天数(Bitmap);
    double studyMinutes = 学习时长(INCR 累计);
    long wrongCount = 错题数;
    long enrollCount = 抢课成功数;
    // 数据 → 提示词（数字是事实，LLM 只做"分析表述"——幻觉风险低）
    String prompt = """
            根据以下学习数据写一段 150 字的个性化学习建议：
            签到 %d 天，学习 %d 分钟，错题 %d 道，已抢课 %d 门。
            """.formatted(signDays, studyMinutes, wrongCount, enrollCount);
    return chatPort.ask("你是学习顾问", prompt);
}
```

**为什么这个模式幻觉风险低**：数字全部来自真实数据（代码聚合），LLM 只做表述——**"事实由系统提供，措辞由 AI 生成"是 AI 功能设计的黄金分工**。

## AI 工程化四支柱（生产必读）

### ① 成本治理

```
Token 计量：每次调用 INCR lc:ai:tokens:{date}（从 usage 字段读）
预算熔断：当日 token 超预算 → 自动降级 mock/短回复
语义缓存：相似问题命中缓存不调 LLM（r-stage6/03——高并发答疑必备）
```

### ② 降级与熔断

```
LLM API 超时/限流 → Resilience4j 熔断 → 降级返回"AI 服务繁忙"或缓存答案
——与 MQ 消费者降级（stage3/04）、DB 熔断同一套思路
```

### ③ 限流：按用户限 AI 调用

AI 接口必须按用户限流（INCR 每日次数——stage5/01 同款 Lua），**否则一个用户循环提问能把你的 API 余额刷爆**。

### ④ 评测：AI 输出质量的回归测试

```
评测集：20~50 个固定问题 + 人工标注的理想答案要点
每次改提示词 → 跑评测集 → 关键词命中率/人工评分对比
——提示词的每次修改都是"发布"，要有回归手段
```

## learn-center AI 模块代码结构

```
ai/
├── ChatPort.java           接口（ask/stream）
├── MockChatPort.java       Mock 实现（默认，无 Key 可跑）
├── RealChatPort.java       Spring AI 实现（@ConditionalOnProperty real）
├── AiConfig.java           按 lc.ai.provider 装配
├── AiService.java          答疑(RAG 上下文)/出题(结构化→入库)/学习报告
└── AiController.java       SSE 流式答疑 / 生成题库 / 学习报告接口
```

## 面试问法

**Q: 你们系统的 AI 功能怎么做的？怎么保证稳定性？**
A: 三功能（答疑/出题/报告）统一走 ChatPort 抽象层（策略模式，mock/real 可切换）。稳定性四支柱：①成本——token 计量+预算熔断+语义缓存；②降级——Resilience4j 熔断 LLM API，失败转人工/默认回复；③幻觉抑制——RAG 限定资料来源+结构化输出+解析失败兜底；④评测——固定评测集回归，提示词改动当发布管理。**核心理念：事实由系统提供，措辞由 AI 生成，失败有降级路径。**

---

**Stage 7 完成** ✅。learn-center 已具备 AI 三功能（代码在 `ai/` 包），从 [README](../learn-center/README.md) 查看启用方式。
