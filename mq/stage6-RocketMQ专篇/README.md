# stage6 · RocketMQ 专篇

RabbitMQ 主线（stage1~4）+ Kafka 专篇（stage5）之外的第三块拼图。工作区 pmhub（SpringCloud Alibaba）用的就是 RocketMQ——业务面试常问的**事务消息、延迟消息**默认问它。

| 篇 | 内容 | 关键词 |
|----|------|--------|
| [01](01-RocketMQ架构与事务消息.md) | 架构（NameServer/CommitLog 混写存储）+ 事务消息（半消息+回查）+ 延迟消息 18 等级 | 半消息、checkLocalTransaction、SCHEDULE_TOPIC |
| [02](02-RocketMQ消费模式与重试.md) | 集群/广播消费 + 重试 16 级递增/死信 + **顺序消息**（QueueSelector+Orderly）+ 位点回溯 | %RETRY%、MessageListenerOrderly、位点重置 |

**前置**：[stage2/04 三大 MQ 底层对比](../stage2-底层原理/04-三大MQ底层对比.md)（先知道三者定位差异再看细节）· [stage3/01 本地消息表](../stage3-问题解决/01-消息丢失怎么防.md)（事务消息解决的就是它的痛点）。
