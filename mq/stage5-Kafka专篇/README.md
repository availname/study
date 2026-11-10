# Stage 5 · Kafka 专篇（5 篇）

主教学线是 RabbitMQ（mq/stage1-4），但 **Kafka 是面试出现率最高的 MQ**，且架构模型与 RabbitMQ 有本质差异（拉模式/分区/消费组/日志保留）。本阶段 5 篇补齐这个最大缺口。

| 篇 | 主题 | 核心考点 |
|----|------|---------|
| [01](01-Kafka架构与核心概念.md) | 架构与核心概念 | Broker/Topic/Partition/**副本 Leader-Follower**/Consumer Group/**KRaft 取代 ZooKeeper** |
| [02](02-生产者可靠性-acks与幂等事务.md) | 生产者可靠性 | **acks 三档**/重试/**幂等生产者**/事务消息 |
| [03](03-消费者组位移提交与Rebalance.md) | 消费者组与位移 | 位移提交/**Rebalance 风暴**/三种语义 |
| [04](04-Kafka与RabbitMQ选型对比.md) | 选型对比 | 架构本质差异/场景决策表 |
| [05](05-Kafka高性能读写原理.md) | 高性能原理 | **顺序写/页缓存/零拷贝/批量压缩/分区并行**——"Kafka 为什么快"标准答案 |

## 与 RabbitMQ 教学的对照学习法

| RabbitMQ 概念（你已学） | Kafka 对应 | 本质差异 |
|------------------------|-----------|---------|
| Exchange 路由 | Topic 分区（无路由概念，按 key 分区） | Kafka 无交换机，路由在 Producer 端 |
| 消费即删 | **日志保留（可回溯重放）** | Kafka 是"账本"，RabbitMQ 是"传送带" |
| ACK 手动确认 | 位移提交（auto/手动） | 语义相近，机制不同 |
| 镜像/仲裁队列 | **分区多副本**（Leader-Follower） | 思想同源：副本+选主 |
| 死信队列 | 无原生（自建 Topic） | RabbitMQ 有原生 DLX |

**学习心法**：每学一个 Kafka 概念就问"RabbitMQ 里对应什么？差异为什么？"——两套对照，面试双杀。
