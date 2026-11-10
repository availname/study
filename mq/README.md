# 消息队列（MQ）完整教学 + 实战项目

> 🔗 姊妹教程：[Redis 完整教学](../redis/README.md)（同款四阶段结构 + 类型场景实战 + **总毕业项目 community**）；mq-mall 的秒杀/幂等正在用 Redis，两套知识互相印证。

从零开始，按 4 个阶段循序渐进学教程，最后在 **mq-mall 实战项目**里把所有知识落地。**每个阶段学完都有动手任务**，做完再进入下一阶段。

> 📌 强烈推荐先读 [踩坑实录-完整复盘.md](踩坑实录-完整复盘.md)——项目真实开发中踩到的 8+ 个坑（幂等误杀、双重序列化、Docker 崩溃修复等），每个都有现象/排查/根因/修复/教训，比教程本身更接近实战。

## 🚀 实战项目（毕业设计）

**[mq-mall/](mq-mall/README.md)** —— 教程知识点的完整落地工程：订单消息系统。

- 下单/支付/超时关单全链路（本地消息表 + 事务边界 + CAS 状态机）
- 秒杀削峰（Redis Lua 预扣 → MQ 匀速落库 → DB 兜底 → 对账端点）
- 可靠性基建（Confirm/Return、补偿重发 Job、消费幂等两层、死信落库）
- 一键环境：`docker compose up -d --wait`（MySQL + Redis + RabbitMQ）

```bash
cd mq/mq-mall
docker compose up -d --wait && mvn spring-boot:run
```

## 学习路线

> 🚀 进阶：两套教程毕业后，继续 [Stage 6 新技术专题](../stage6-新技术专题/README.md)——虚拟线程、Spring AI 审核升级、Redis 向量检索、OTel 全链路追踪、GitHub Actions CI/CD、Debezium CDC、Testcontainers。

| 阶段 | 目录 | 学什么 | 前置要求 |
|------|------|--------|----------|
| 1. 入门使用 | [stage1-入门使用](stage1-入门使用/) | MQ 是什么、为什么用、装起来、跑通第一个收发消息、五种工作模式 | JDK 17+、Maven、Docker |
| 2. 底层原理 | [stage2-底层原理](stage2-底层原理/) | AMQP 协议、消息怎么存储、怎么保证不丢、三大 MQ 架构对比 | 学完 stage1 |
| 3. 问题解决 | [stage3-问题解决](stage3-问题解决/) | 丢消息、重复消息、乱序、堆积、延迟消息——面试和实战的九九八十一难 | 学完 stage2 |
| 4. 高并发实战 | [stage4-高并发实战](stage4-高并发实战/) | 秒杀削峰、订单超时取消、大促订单链路与分布式事务 | 学完 stage3 |
| 5. 真实场景 | [stage5-真实场景-视频平台](stage5-真实场景-视频平台/) | 视频处理流水线：管道模式、审核状态机、fanout 广播、推送削峰、失败语义 | 学完 stage4 |
| 6. RocketMQ 专篇 | [stage6-RocketMQ专篇](stage6-RocketMQ专篇/) | 架构、事务消息（半消息+回查）、延迟消息 18 等级、集群/广播消费、重试/死信、**顺序消息**、位点回溯——pmhub 同款 | 学完 stage3（对照 stage2/04 三大对比） |

> 📚 **专题横读**：[延迟任务方案横向对比](延迟任务方案横向对比.md)——DelayQueue/扫表/ZSet/过期通知/TTL+DLX/RocketMQ/时间轮六方案一张表选型（配合 stage3/05 与 stage4/02 看，mq-mall 双保险是落地答案）。

> 📦 **两大专篇**：[stage5-Kafka 专篇](stage5-Kafka专篇/README.md)（5 篇：架构/生产者可靠性/消费者组/选型/**高性能原理**）· [stage6-RocketMQ 专篇](stage6-RocketMQ专篇/README.md)（2 篇：事务消息/消费模式重试顺序）——面试双杀套餐。

配套可运行的示例工程在 [demo/](demo/README.md)（Spring Boot 3 + RabbitMQ），教程里的所有代码片段都能在里面找到。

## 阶段任务（检验学习效果）

- [ ] **Stage 1**：Docker 启动 RabbitMQ，跑通 demo 里的 Hello World，在管理界面看到消息从 Ready 变成 0
- [ ] **Stage 1**：自己写一个 Topic 模式：`order.create` 能被 `order.#` 收到、`user.create` 不能
- [ ] **Stage 2**：故意 kill 掉 RabbitMQ 容器，验证：非持久化消息丢了、持久化三件套的消息还在
- [ ] **Stage 2**：打开生产者 Confirm，故意发到一个没有绑定的交换机，观察 Return 回调
- [ ] **Stage 3**：消费者抛异常，观察消息被反复重投；再配死信队列让它"体面地死掉"
- [ ] **Stage 3**：给消费者加幂等，连续投递 10 条相同 ID 的消息，确认只处理一次
- [ ] **Stage 4**：用 JMeter/ab 压 1000 QPS 打秒杀接口，观察队列削峰、数据库稳定在消费速率
- [ ] **Stage 4**：下单后 2 分钟不支付（把 30 分钟改成 2 分钟），观察订单自动取消、库存回滚

## 本教程的选择说明

- **主线用 RabbitMQ**：模型最直观（交换机-队列），最适合建立概念，你工作区的 `Demo/inventory-service` 也在用。
- **重点场景对比 RocketMQ/Kafka**：你工作区的 `pmhub` 用的是 RocketMQ（SpringCloud Alibaba 体系），高并发场景、事务消息部分会给出 RocketMQ 的对应写法；完整展开见 [stage6-RocketMQ专篇](stage6-RocketMQ专篇/README.md)。
- 代码为 Java 21 + Spring Boot 3.2。
