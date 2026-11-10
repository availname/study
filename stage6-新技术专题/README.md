# Stage 6 · 新技术专题：把 2026 年的现代技术揉进 MQ + Redis

> 🔗 AI 深入篇：[stage7-AI应用开发](../stage7-AI应用开发/README.md)（Spring AI/RAG/流式/工程化，已落地 learn-center AI 模块）。

前五个阶段的底层功力（MQ 可靠性、Redis 结构）**不会过时**；但工具链在进化。这一阶段把当前业界主流与上升期的技术逐一接进来——每一篇都落到你已有的项目上（community / mq-mall / redis-demo），学完即用。

| 篇 | 新技术 | 解决什么老问题 | 落地物 |
|----|--------|---------------|--------|
| [01](01-VirtualThreads虚拟线程.md) | Java 21 虚拟线程（Loom） | IO 密集并发不再需要响应式的复杂度 | community 一行配置开启 + 消费者实测 |
| [02](02-SpringAI-LLM审核管道.md) | Spring AI + LLM | "模拟审核"升级为真实智能审核 | community 审核消费者接入 LLM（含降级） |
| [03](03-Redis向量检索与语义缓存.md) | Redis 向量检索 / Embedding | LLM 时代的 Redis 新角色：向量库 + 语义缓存 | redis-stack 安装 + 语义缓存原理实现 |
| [04](04-OpenTelemetry全链路追踪.md) | OTel / Micrometer Tracing | "消息丢了在哪一环"从猜变成看 | community 全链路 Trace + Zipkin |
| [05](05-GitHubActions-CICD.md) | GitHub Actions | 手工 mvn compile → push 自动构建部署 | community / mq-mall 各一个真实 workflow |
| [06](06-Debezium-CDC与Outbox模式.md) | Debezium CDC | 手写本地消息表的工业级替代 | 原理 + 本地消息表对比 + 落地路线 |
| [07](07-Testcontainers现代集成测试.md) | Testcontainers | mock 测试测不出真实集成问题 | 真实 Redis/MQ 容器里跑测试 |
| [08](08-响应式与虚拟线程路线之争.md) | WebFlux/Reactor vs Loom | 并发编程的路线选择（2026 观点） | 决策表 |

## 学习建议

- 每篇独立成章，**顺序无强依赖**，按兴趣跳读。
- 涉及外部 API（LLM）和额外容器（Zipkin、redis-stack）的篇目，都给了**无外部依赖的降级方案**，不花钱也能学。
- 与旧阶段的连接点在各篇"回望"小节：新技术解决的是旧行当里的什么痛点。

## 版本基线（2026 年主流）

```
Java 21 LTS（虚拟线程正式）· Spring Boot 3.2+ · Spring AI 1.0 GA
Redis 7.x/8.x · RabbitMQ 3.13 · GitHub Actions · OpenTelemetry 事实标准
```

**前置**：学完 mq 四阶段 + redis 四阶段 + 跑过 community（两边知识不懂，新技术只是空中楼阁）。
