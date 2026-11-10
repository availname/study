# 01 · AMQP 协议与整体架构

Stage 1 你在用 RabbitMQ，现在拆开看它到底是什么。

## AMQP 是什么

**AMQP（Advanced Message Queuing Protocol）**是应用层协议，定义了消息的格式（帧）和收发的动作（publish/consume/ack…）。RabbitMQ 是它的经典实现。

就像 HTTP 定义了"请求行 + 头 + 体"，AMQP 定义了：

```
┌─ Method Frame（方法帧）────── basic.publish / basic.consume / basic.ack...
├─ Content Header（内容头）─── body 大小、contentType、deliveryMode...
├─ Content Body（内容体）───── 消息本体（分块传输，每块最大 128KB）
└─ Heartbeat（心跳帧）──────── 默认 60s，双向保活
```

抓包看，一次"发消息"= 3 个帧（方法帧 + 内容头 + 内容体）；一次"消费确认"= 1 个帧。这就是为什么 prefetch 太小吞吐低——每条消息 ack 都要一个网络来回。

## 核心概念层级

```
┌─ RabbitMQ Broker（一个服务器进程）───────────────┐
│  ┌─ Virtual Host: /order ──┐  ┌─ Virtual Host: /seckill ──┐
│  │   Exchange A            │  │                            │
│  │   Queue 1, Queue 2      │  │   ...                      │
│  │   Binding...            │  │                            │
│  └─────────────────────────┘  └────────────────────────────┘
└──────────────────────────────────────────────────────────┘
```

| 概念 | 说明 |
|------|------|
| Broker | RabbitMQ 服务实例。集群 = 多个 Broker |
| Virtual Host | 逻辑隔离的"小 RabbitMQ"。不同 vhost 的交换机/队列/用户完全隔离，A 项目看不到 B 项目的队列。公司多个项目共用一套 MQ 时按项目分 vhost |
| Exchange | 交换机。**生产者只发消息给交换机**，从不直接发队列 |
| Queue | 队列。消息真正的存储处，FIFO |
| Binding | 交换机 → 队列的绑定（可带 routing key 规则），是"路由表" |

## 消息完整流转过程（背下来）

```
① Producer 创建 Channel，basicPublish(exchange, routingKey, props, body)
② Exchange 收到消息，查自己的 Binding 表
③ 按 Binding 规则（direct 等值 / topic 通配 / fanout 全发）算出目标队列
④ 一个匹配多个队列 → 复制多份；一个都没匹配 → 默认丢弃（→ 2-03 章的 Return 保险）
⑤ Queue 接住消息，写入存储，状态为 Ready
⑥ Queue 按一定速率把消息推（push）给订阅中的消费者
⑦ 消息状态变为 Unacked（除非 autoAck=true，此时直接删除）
⑧ 消费者处理完 basicAck(deliveryTag) → Broker 真正删除消息
```

几个细节：

- **④ 的"没匹配就丢"** 是新手事故高发地：routing key 写错，消息无声无息消失。
- **⑥ 推模式**：消费者在线时 Broker 主动推，延迟极低（微秒级）；消费不过来时靠 prefetch 背压自保。
- **⑧ ACK 是消费级别的确认**，和"消息是否丢失"是两套机制（生产端还有自己的确认，2-03 细讲）。

## 为什么要有 Exchange 这一层？

生产者直连队列不是更简单吗？

**为了路由解耦。**没有交换机时，"发到哪个队列"硬编码在生产者代码里，加消费者就得改生产者。有了交换机：

- 生产者永远只说：把消息发给 `biz.topic` 交换机，key 是 `order.create`。
- 至于谁在听，是队列绑定的事，**生产者完全不知道、不需要知道**。
- 新增一个"对账服务"想听订单创建？绑一个新队列即可，生产者零改动。

## Connection 与 Channel

```
App ─═TCP═Connection═╗
     ╠═Channel-1（线程A用）
     ╠═Channel-2（线程B用）
     ╚═Channel-3（线程C用）      RabbitMQ Broker
```

- **Connection = TCP 连接**。三次握手 + TLS 握手 + 认证，建一次要几十毫秒，**必须长连接复用**。
- **Channel = 连接上的轻量虚拟信道**。每次 publish/consume 都在 Channel 上进行，创建成本约等于一次内存操作。
- 为什么不一条 TCP 只跑一个任务？网络连接昂贵；为什么不在所有线程共享一个 Channel？**Channel 不是线程安全的**，多线程共用会串帧。
- Spring Boot 的 `CachingConnectionFactory` 默认帮你缓存 Connection、按需开 Channel，还提供 Channel 池——所以你在业务里只管注入 `RabbitTemplate`。

## 心跳与自动恢复

- 心跳默认 **60 秒**，双向。Broker 判断连接死掉（如消费者机器断电）→ 关闭连接 → 该消费者未 ack 的消息**重新入队**。
- Spring AMQP 内置自动恢复：连接闪断后自动重连、自动重建 Channel、自动恢复 `@RabbitListener` 订阅。老代码里那种"网络闪断必须重启应用"的时代过去了，但**自己裸写 Connection 时要处理 `ShutdownListener`**。

## RabbitMQ 为什么快：Erlang

一句话背景：RabbitMQ 用 **Erlang/OTP** 写成。Erlang 是爱立信为电信交换机设计的语言：

- **Actor 模型**：万物皆进程（几百万个超轻量进程），每个队列就是一个 Erlang 进程，互不共享内存，一个崩了不影响别的。
- 天然支持热升级、分布式（Erlang 节点互联），所以 RabbitMQ 集群是"天生"的。

副作用：**想在 RabbitMQ 里做深度定制/二次开发，得会 Erlang**，这也是它扩展生态不如 RocketMQ/Kafka（都是 JVM 系）活跃的原因之一。

## 面试问法与答案骨架

**Q: 生产者是把消息直接发到队列吗？**
A: 不是。发给 Exchange，Exchange 按 Binding 规则路由到 Queue。这样生产者与消费者通过路由规则解耦，新增消费者不用改生产者。没匹配到队列默认丢弃，可用 mandatory+Return 机制兜底。

**Q: Connection 和 Channel 的区别？为什么这么设计？**
A: Connection 是 TCP 长连接，建立成本高，全局复用；Channel 是连接上的轻量逻辑信道，线程不安全，每个线程/操作各用一个。设计上复用昂贵的 TCP、并发用廉价 Channel。

**Q: RabbitMQ 消息何时被删除？**
A: Broker 收到消费者对 deliveryTag 的 ack 后删除（非 autoAck）。autoAck=true 时投递即删——处理失败也无法找回，生产禁用。

---

**下一节**：[02-消息的存储与持久化机制](02-消息的存储与持久化机制.md)——消息存在哪，重启会不会丢。
