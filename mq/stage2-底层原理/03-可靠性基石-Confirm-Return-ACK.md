# 03 · 可靠性基石：Confirm、Return、ACK

消息从生产到消费，要过**三道关**。这一章是 Stage 3 "防消息丢失"的原理地基，也是 RabbitMQ 面试的核心。

```
生产端                 Broker                        消费端
  │① basicPublish        │                             │
  │───────────────────→  │                             │
  │        ② Confirm（到达 Broker + 落盘了吗？）         │
  │←─────────────────────│                             │
  │        ③ Return（路由到队列了吗？没有就退回给你）     │
  │←─────────────────────│                             │
  │                      │④ push 消息                  │
  │                      │───────────────────────────→ │
  │                      │        ⑤ ACK（处理完了吗？） │
  │                      │←───────────────────────────│
```

## 一、生产端：Publisher Confirm（消息到 Broker 了吗）

**问题**：`basicPublish` 是异步的、不等待的。发送时网络抖动/Broker 挂了，你的代码以为发成功了，消息其实没到。**Confirm 机制让 Broker 收到并处理消息后，回执一个 ACK 给生产者。**

```java
spring:
  rabbitmq:
    publisher-confirm-type: correlated   # 开启 Confirm（异步回调模式）
```

```java
@Configuration
@Slf4j
public class ConfirmConfig {

    @Bean
    public CorrelationDataProcessor correlationDataProcessor(RabbitTemplate rabbitTemplate) {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String msgId = correlationData != null ? correlationData.getId() : "?";
            if (ack) {
                log.debug("消息到达 broker, id={}", msgId);
            } else {
                log.error("消息未到达 broker! id={}, cause={}", msgId, cause);
                // 关键动作：记录到失败表/重发。见 stage3/01 的"本地消息表"兜底
            }
        });
        return null;
    }
}
```

发送时携带 `CorrelationData`（唯一 ID），回调里按 ID 处理：

```java
rabbitTemplate.convertAndSend("biz.topic", "order.create", order,
        new CorrelationData("order-" + order.getId()));
```

**两种用法**：

| 用法 | API | 适用 |
|------|-----|------|
| 同步等待 | `channel.waitForConfirmsOrDie()`，每条/每批等回执 | 简单但慢，吞吐低 |
| **异步回调**（推荐） | `setConfirmCallback`，不阻塞发送 | 高吞吐，回调里做补偿 |

**注意**：Confirm 的 ACK 只代表"Broker 收到了"，**不代表消息进了队列**（可能路由失败被丢弃）——所以需要下面的 Return。

## 二、生产端：Publisher Return（路由到队列了吗）

**问题**：消息到了 Broker，但交换机路由不到任何队列（routing key 写错/忘了绑队列）→ **默认直接丢弃，Confirm 还返回成功**。`mandatory=true` 让 Broker 把"无法路由"的消息**退回**给生产者。

```java
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true               # 开启 Return
    template:
      mandatory: true                     # 路由失败必须退回
```

```java
rabbitTemplate.setReturnsCallback(returned ->
        log.error("消息被退回! exchange={}, routingKey={}, replyText={}, message={}",
                returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyText(), new String(returned.getMessage().getBody())));
// 回调里：告警 + 修正路由 + 重发
```

**Confirm 与 Return 的分工**：

- Confirm 管的是：**到没到 Broker**（网络/宕机层面）
- Return 管的是：**到 Broker 后有没有人接**（路由层面）
- 两个都绿了，消息才算"安全进入队列"。

**动手验证**：demo 的 `reliable` 包，往一个故意配错 routing key 的交换机发消息，看 Return 日志触发。

## 三、Broker 端：持久化 + 多副本

上一章讲过，一句话复习：持久化三件套（+必要时的 Quorum Queue）。Confirm 回调 `ack=true` 时，持久化消息**已经安全落盘**。

## 四、消费端：手动 ACK（处理完才算数）

**问题**：`autoAck=true` 时，Broker 把消息一推出去就删。消费者拿到消息后进程崩了/处理抛异常 → 消息永远没了。

**方案**：`autoAck=false`，Broker 推出消息后标记 Unacked，**收到消费者的 basicAck 才真正删除**。

```java
@RabbitListener(queues = "order.queue", ackMode = "MANUAL")
public void handle(OrderMsg msg, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag)
        throws IOException {
    try {
        doBusiness(msg);                                  // 1. 处理业务
        channel.basicAck(tag, false);                     // 2. 成功 → 确认
    } catch (Exception e) {
        // 3. 失败 → 拒绝。requeue=true 回队列重试（小心死循环！），false 进死信队列（stage3/05）
        channel.basicNack(tag, false, false);
    }
}
```

三个确认 API：

| API | 语义 | 参数 requeue |
|-----|------|-------------|
| `basicAck(tag, multiple)` | 成功 | — |
| `basicNack(tag, multiple, requeue)` | 失败（可批量，multiple=true 批量确认本 channel 上所有已收消息） | true 重回队列队头 |
| `basicReject(tag, requeue)` | 失败（单条） | 同上 |

**⚠ requeue=true 的死循环陷阱**：失败消息重回队列 → 立刻再投给同一个消费者 → 又失败 → 又重投……无限循环打满 CPU。所以：

- 简单做法：`requeue=false` + 死信队列（stage3/05）；
- Spring 默认 AUTO ack 模式自带**有限本地重试**，重试耗尽后走死信/丢弃，见下。

### Spring 的两套消费重试姿势

**姿势 A：AUTO ack + 本地重试（最常用，代码最少）**

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: auto            # 方法正常返回才确认，抛异常不确认
        default-requeue-rejected: false   # 重试耗尽后不要重回队列（进死信）
        retry:
          enabled: true                   # 本地重试（不回队列，应用内存里重试）
          max-attempts: 3
          initial-interval: 1000
          multiplier: 2                   # 1s → 2s → 4s
```

语义：抛异常 → 本地间隔重试 3 次 → 还失败 → 不 requeue → 交给死信/丢弃。**注意：本地重试是在同一个线程里 catch 重跑，不是重新从队列拿**，重试期间消息一直 Unacked。

**姿势 B：MANUAL ack（需要精细控制时）**

自己决定 ack/nack/requeue 的时机，配合"消费成功的幂等记录"使用（stage3/02）。

## 五、prefetch 与可靠性的关系

`prefetch=N` = 该消费者最多同时持有 N 条 Unacked。

- **prefetch 是可靠性的节流阀**：消费者处理慢，Broker 推满 N 条就停，不会把消费者内存压爆，也不会因为"发出去太多没 ack"而在消费者崩溃时一次性回灌海量消息。
- 消费者崩溃时，其 Unacked 的消息全部退回 Ready——**prefetch 越大，崩溃回滚的消息越多，重复消费的面越广**（幂等的重要来源之一）。

## 全链路总结表（本章 = Stage 3 第一章的理论底座）

| 环节 | 风险 | 保障机制 |
|------|------|---------|
| 生产 → Broker | 网络失败、进程崩 | Confirm 异步确认 + 失败补偿（本地消息表） |
| Broker 内路由 | 无人接收被丢弃 | mandatory + ReturnCallback |
| Broker 存储 | 宕机丢内存数据 | 持久化三件套 / Lazy Queue / Quorum Queue |
| Broker → 消费者 | 消费者崩溃 | Unacked + 断连重入队 + prefetch |
| 消费处理 | 处理失败 | 手动 ACK / 本地重试 / 死信队列 |

## 面试问法

**Q: 如何保证 RabbitMQ 消息不丢？**
A: 分三端答——生产端 Confirm 确认 + Return 兜底不可路由；Broker 端持久化三件套，高可用用 Quorum Queue；消费端关 autoAck，处理成功后手动 ACK，失败走重试+死信。极端情况（Confirm 未收到就宕机）用本地消息表/事务消息做最终一致。

**Q: autoAck=true 有什么问题？**
A: 投递即删。消费者处理失败或崩溃时消息已不存在，无法恢复。生产环境必须手动/ AUTO 模式确认。

---

**动手任务**：demo 的 `reliable` 包——① 发错 routing key 看 Return 触发；② 消费者方法里故意抛异常，观察本地重试 3 次后消息进死信队列。

**下一节**：[04-三大MQ底层对比](04-三大MQ底层对比.md)。
