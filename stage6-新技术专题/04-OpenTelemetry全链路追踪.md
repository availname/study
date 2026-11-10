# 04 · OpenTelemetry 全链路追踪：一条消息的"定位系统"

分布式系统排障的终极痛点：**一个请求跨了 5 个服务 + 2 次消息投递，日志分散在 7 个地方**。"消息到底丢在哪一环"靠猜。链路追踪把它变成**一张可视化调用图**。

## 三个核心概念（一分钟建立心智模型）

```
一个 Trace = 一条请求的完整旅程（唯一 traceId 贯穿全程）
  ├── Span: 发帖接口            [====]      每个 Span = 一个工作单元
  │    ├── Span: 写DB            [==]       Span 之间有父子/兄弟关系
  │    ├── Span: 发MQ            [==]
  │    │      └── Span: 审核消费者 [====]   ← 跨进程！traceId 继续传递
  │    │            └── Span: LLM调用  [==]
  │    └── Span: fanout→Feed推送  [==]
```

**魔法机制：traceId 如何跨进程传播？** 放进消息头！MQ 教学里你学过消息属性（messageId），追踪的 `traceparent` 头同理——生产者写入，消费者读出继续，这就是"上下文传播"（W3C Trace Context 标准）。

## 落地：community 加三样东西（真实可跑）

### 1. 依赖（Spring Boot 3.2 原生支持 Micrometer Tracing，即 OTel API 的 Spring 实现）

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

### 2. 配置 + Zipkin 收集器（一个容器）

```yaml
management:
  tracing:
    sampling:
      probability: 1.0      # 开发环境 100% 采样（生产 1~10%）
spring:
  rabbitmq:
    listener:
      observe: enabled      # 消息收发自动产生 Span
```

```bash
docker run -d --name zipkin -p 9411:9411 openzipkin/zipkin:latest
```

### 3. 打开 http://localhost:9411 → 搜一个接口

你会看到 community 的**发帖全链路瀑布图**：

```
POST /api/posts ── 120ms
 ├─ jdbc INSERT t_post ── 15ms
 ├─ INSERT t_mq_send_record ── 8ms
 ├─ rabbitmq publish post.submitted ── 3ms
 └─ rabbitmq process post.submitted（消费者 Span，跨进程自动衔接）
      ├─ redis dedup ── 1ms
      ├─ sleep AI 审核 ── 800ms  ← 火焰图上最长的一截，瓶颈一目了然
      ├─ jdbc 积分流水 ── 6ms
      └─ fanout 发布 ── 4ms
```

**MQ 的 Span 自动父子衔接**是亮点：Spring AMQP 3.x 内置了 Observation 支持，发送时把 trace 上下文写进消息头，消费时自动接续——你不用写一行传播代码，就得到了"一条消息从生产到消费的完整时间线"。

## 追踪在 MQ/Redis 场景的三个杀手级用法

### 1. 消息延迟定位

热榜消费者处理某帖花了 45 秒？展开 Span 树直接看哪一段：Redis 卡了、DB 锁等待了、还是消费前在队列排队（队列排队 Span = 堆积告警的前兆）。

### 2. 按 traceId 跨服务查日志

排查规范：**把 traceId 写进每条日志**（MDC 自动带出），7 个服务的日志用同一个 traceId 一搜全串起来。用户报障给个时间点，先查 trace 再查日志。

```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### 3. Redis 慢命令的业务归因

`--bigkeys` 告诉你哪个 key 大，**Trace 告诉你哪个业务路径在打它**——Span 上的 Redis 命令耗时与业务 Span 并排看，慢命令瞬间归因。

## 生产采样的取舍

全量采样的存储成本巨大（每请求几十个 Span）。策略：**头部采样**（按概率 1~10% 保留完整 trace）+ **错误全采**（失败的 100% 保留）+ **尾部采样**（进阶：慢请求/特殊用户事后补采）。开发 100%、生产 5% 起步按排障需求调。

## 与你已有工具的分工

| 问题类型 | 用什么 |
|---------|--------|
| "这一步花了多久？瓶颈在哪跳？" | **Trace**（Zipkin） |
| "系统整体水位/吞吐/错误率？" | **Metrics**（Prometheus + Grafana，management endpoints 已内置 Micrometer） |
| "出错的详细上下文？" | **Logs**（traceId 串联） |

三者合称可观测性三支柱，OTel 是统一标准（Traces/Metrics/Logs 一个协议打包上报）。

## 面试问法

**Q: 微服务链路追踪的原理？MQ 消息的 trace 怎么传播？**
A: OTel/Micrometer 为每个请求生成全局唯一 traceId，各工作单元为 Span（含父子关系与耗时）。跨进程传播靠协议头：HTTP 放 header，**MQ 放消息属性（W3C traceparent）**，生产者注入、消费者提取续接——Spring AMQP 3.x 原生集成自动完成。落地三件套：Micrometer Tracing + Brave + Zipkin，配置采样率，日志 pattern 注入 traceId 实现跨服务日志串联。生产用头/尾部采样控制成本。

---

**下一篇**：[05-GitHubActions-CICD](05-GitHubActions-CICD.md)。
