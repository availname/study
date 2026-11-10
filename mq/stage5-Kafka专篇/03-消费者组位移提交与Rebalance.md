# 03 · 消费者组：位移提交与 Rebalance

消费者端两大考点：**位移提交语义**（三种消息语义）与 **Rebalance 风暴**。

## 一、位移提交：三种消息语义

```java
// 自动提交（默认，每 5s 提交上次位置）
enable.auto.commit=true

// 手动提交（精准控制）
// ① 处理完再提交（至少一次：可能重复）
consumer.commitSync();                 // 阻塞提交

// ② 先提交再处理（至多一次：可能丢）
consumer.commitAsync();                // 异步非阻塞
```

| 语义 | 提交时机 | 后果 | 场景 |
|------|---------|------|------|
| **至少一次** | 处理**后**提交 | 可能重复 → 消费端幂等 | 默认选择（与 RabbitMQ 手动 ACK 相同） |
| 至多一次 | 处理**前**提交 | 可能丢失 | 可容忍丢失（指标类） |
| 精确一次 | 事务（消费+提交原子） | 无重复无丢失 | Kafka Streams / consume-transform-produce |

**对照 RabbitMQ**：手动 ACK + 幂等 = 至少一次，一模一样——**"处理后再确认 + 消费端幂等"是跨 MQ 的通用模式**。

## 二、Rebalance：分区重新分配（最大的坑）

**触发条件**：消费者加入/离开组、分区数变化、心跳超时。

```
Rebalance 期间：整组停止消费（Stop-the-world）！
  → 大组（几百消费者）Rebalance 可持续分钟级 → 消费中断+延迟飙升
```

**触发元凶**（生产高频）：

1. 消费者**处理太慢**（max.poll.interval.ms 超时）→ 被踢出组 → Rebalance → 重新加入 → 再超时 → **死亡螺旋**
2. 消费者频繁重启（发布/K8s 滚动更新）
3. 心跳丢失（网络/GC 停顿）

**参数治理**：

```properties
max.poll.interval.ms=600000      # 两次 poll 最大间隔（处理慢调大！）
session.timeout.ms=45000         # 心跳超时
heartbeat.interval.ms=3000
max.poll.records=500             # 单次 poll 拉取条数（处理慢调小）
```

**消费位移模式也有关**：`auto.offset.reset`（earliest/latest）只管**无位移**时的起点，不管 Rebalance。

## 三、Cooperative Rebalance（2.4+）：增量式

旧版 Eager Rebalance：全组停止→全量重分配（STW 式）。**Cooperative（协作式）**：只迁移必要分区，其余消费者继续工作——**升级到 2.4+ 并开启 cooperative-sticky 分配策略，Rebalance 停顿从分钟级降到秒级**：

```java
props.put("partition.assignment.strategy",
    "org.apache.kafka.clients.consumer.CooperativeStickyAssignor");
```

## 四、位移提交最佳实践

```java
// 高吞吐稳妥姿势：先批量处理 → 异步提交（失败不影响，下次周期性提交兜底）
consumer.poll(Duration.ofSeconds(1));
process(records);
consumer.commitAsync((offsets, e) -> { if (e != null) log.warn("提交失败", e); });
// 关闭前 commitSync 兜底
```

**重复消费兜底**：位移提交失败/滞后 → 重复消费 → **消费端幂等是终极保险**（与 RabbitMQ 教学完全同构）。

## 面试问法

**Q: Kafka 消费者 Rebalance 是什么？怎么避免频繁触发？**
A: 消费者加入/离开组或分区数变化时重新分配分区，**期间整组停止消费**。触发元凶：处理超时被踢（max.poll.interval）、频繁重启、心跳丢失。治理：调大 max.poll.interval、调小 max.poll.records（处理不过来少拉）、升级 2.4+ Cooperative 增量 Rebalance（只迁必要分区）、K8s 滚动更新配优雅停机（先 leave group 再停进程）。

**Q: Kafka 怎么保证不丢消息？**
A: 三端——生产端 acks=all + min.insync.replicas≥2 + 幂等重试；Broker 副本 + 刷盘策略；消费端先处理后手动提交位移（至少一次），配合业务幂等。与 RabbitMQ 的本地消息表模式完全同构。

---

**下一篇**：[04-Kafka与RabbitMQ选型对比](04-Kafka与RabbitMQ选型对比.md)。
