# 02 · 生产者可靠性：acks、幂等生产者与事务

三条可靠性等级递进：**acks 确认 → 幂等去重 → 事务多分区原子**。

## 一、acks：可靠性三档（必考）

| acks | 语义 | 丢数据窗口 | 性能 |
|------|------|-----------|------|
| `acks=0` | 发了就不管（不等确认） | 网络抖动即丢 | 最快 |
| `acks=1` | Leader 写入即确认 | Leader 确认后挂、Follower 未同步 → 丢 | 折中 |
| **`acks=all`** | 等待 **min.insync.replicas** 个同步副本全部写入 | 副本全挂才丢（配 min=2 则极难丢） | 最慢 |

```properties
acks=all
min.insync.replicas=2      # 至少 2 个同步副本确认（配合 replication.factor=3）
retries=Integer.MAX_VALUE  # 失败自动重试（3.0+ 默认无限重试）
delivery.timeout.ms=120000 # 总投递超时（含重试）
```

**考点**：`acks=all` 不是"所有副本"，是"所有 **ISR**（同步副本）"——ISR 缩水时 all 也变弱，所以要配 `min.insync.replicas`。

## 二、幂等生产者：解决重试重复

问题：acks 重试导致**同一消息可能写多次**（broker 写成功但 ack 丢了 → producer 重试 → 重复）。

```properties
enable.idempotence=true   # 3.0+ 默认已开
```

原理：Producer 为每条消息带（PID + 序列号），Broker 校验序列号连续性——**重复自动去重**。限制：只保证**单分区、单会话**内幂等。

## 三、事务：跨分区/跨 Topic 原子写入

```java
producer.initTransactions();
try {
    producer.beginTransaction();
    producer.send(record1);
    producer.send(record2);
    producer.commitTransaction();
} catch (Exception e) {
    producer.abortTransaction();
}
```

- 消费端要 `isolation.level=read_committed` 才能看到已提交消息
- 典型场景：**Kafka Streams 的"消费-处理-产出"原子性**（consume-transform-produce 循环）
- 注意：Kafka 事务 ≠ 数据库事务（不回滚业务库）——它保证"一组消息原子写入 Kafka"

## 四、消息顺序（呼应 mq 教学顺序篇）

- **分区内天然有序**——需要顺序的消息发同一分区（相同 key）
- 陷阱：`max.in.flight.requests.per.connection > 1` 时重试会乱序——**开启幂等生产者后**（3.0+）框架保证 in-flight 内有序

## 五、三档可靠性配置模板（背）

```properties
# 最高可靠（金融级）
acks=all + min.insync.replicas=2 + enable.idempotence=true + retries=MAX

# 高吞吐（日志类，可容忍丢失）
acks=1 + enable.idempotence=false + linger.ms=20 批量压缩
```

## 面试问法

**Q: Kafka 生产者如何保证消息不丢不重？**
A: 不丢：`acks=all` + `min.insync.replicas≥2` + `retries=MAX` + `delivery.timeout` 兜底；不重：`enable.idempotence=true`（PID+序列号，Broker 去重）。跨分区原子性用事务（transactional.id + read_committed 消费）。终极兜底仍是消费端幂等（业务键去重）——与 RabbitMQ 体系思想完全一致。

---

**下一篇**：[03-消费者组位移提交与Rebalance](03-消费者组位移提交与Rebalance.md)。
