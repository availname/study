# 01 · Kafka 架构与核心概念

## 一、架构总览（KRaft 模式，4.0 起无 ZooKeeper）

```
                    ┌────────── Kafka 集群 ──────────┐
Producer ──写入──→  │ Broker1    Broker2    Broker3  │  ←── Consumer 拉取
（生产者）          │  [Controller/元数据主]          │      （消费者组）
                    └──────────── KRaft 元数据共识 ───┘
                         （3.x 起取代 ZooKeeper，4.0 彻底移除）
```

| 角色 | 职责 |
|------|------|
| **Broker** | Kafka 服务实例（一个集群多台 Broker） |
| **Controller** | 集群元数据主（分区 Leader 选举等）——KRaft 模式下由 Broker 内 KRaft 仲裁选出 |
| **Producer/Consumer** | 生产者/消费者 |
| **KRaft** | 内置共识协议（Raft 实现）管理元数据——**取代 ZooKeeper**（3.3+ 生产可用，4.0 移除 ZK 支持） |

**部署视角的演进**（面试高频）：ZooKeeper 架构（元数据在 ZK）→ KRaft 架构（元数据自管理，故障恢复更快、支持分区百万级）。

## 二、Topic 与 Partition：并行与顺序的平衡

```
Topic: order-create（逻辑主题）
  ├─ Partition0: [msg0][msg1][msg2]...  每个分区 = 有序、不可变的追加日志
  ├─ Partition1: [msg0][msg1]...
  └─ Partition2: ...
每个 Partition 分布在不同 Broker，有 Leader + Follower 副本
```

- **分区数 = 并行度上限**（消费组内消费者数最多=分区数，多的空闲）
- **分区内有序，跨分区无序**——需要顺序的消息发同一分区（按 key 路由：`hash(key) % 分区数`）
- 副本机制：每个分区一个 **Leader**（处理读写）+ N 个 **Follower**（同步）；Leader 挂了从 ISR（同步副本集合）中选新 Leader

**对照 RabbitMQ**：RabbitMQ 的"队列"≈ Kafka 的"分区"；RabbitMQ 的 Exchange 路由能力 Kafka 没有——路由在 Producer 按 key 分区。

## 三、Consumer Group：消费组（Kafka 的灵魂）

```
Topic（6 分区）
  ├─ 消费组A: consumer1 ← P0,P1    consumer2 ← P2,P3    consumer3 ← P4,P5
  └─ 消费组B: consumerX ← P0..P5（独立全量消费，互不影响）
```

- **同组竞争**：一个分区同一时刻只被组内**一个**消费者消费（负载均衡）
- **跨组独立**：不同组各自消费全量（广播语义）
- **消费者数 > 分区数 → 多余的消费者空闲**

**对照 RabbitMQ**：RabbitMQ 的消费者组竞争≈多个消费者抢同一队列；Kafka 的组语义相同，但**多组独立消费全量**是 RabbitMQ 要用多条队列或 fanout 复制才能实现的。

## 四、日志保留：可回溯重放（与 RabbitMQ 最大差异）

消息写入分区后**保留而不删除**（按时间/大小策略清理）：

- 消费位移（offset）由消费者自行管理——**重置 offset 就能重放历史**（数据修复神器，对比 RabbitMQ 消费即删）
- 生产实践：数据修复（错误消费后重放）、新服务上线从头消费历史

## 五、核心配置速查

```properties
# Broker
num.partitions=6                      # 新 Topic 默认分区数
default.replication.factor=3          # 副本数
log.retention.hours=168               # 日志保留 7 天

# Producer
acks=all                              # 等待所有 ISR 副本确认（stage5/02 详解）
enable.idempotence=true               # 幂等生产者（3.0+ 默认开）

# Consumer
group.id=my-group
auto.offset.reset=earliest            # 无位移时从头消费（latest 默认）
enable.auto.commit=true               # 自动位移提交（5s 间隔）
```

## 面试问法

**Q: Kafka 为什么快？**
A: 顺序写（分区日志追加，磁盘顺序写≈内存随机写速度）+ 页缓存（读写走 OS page cache）+ 零拷贝（sendfile，数据不进用户态）+ 批量与压缩（攒批发送）+ 分区并行。对比 RabbitMQ：RabbitMQ 复杂路由+低延迟单条语义，Kafka 牺牲单条灵活性换极限吞吐。（五支柱逐个拆解见 [05 篇](05-Kafka高性能读写原理.md)）

**Q: KRaft 是什么？为什么要去掉 ZooKeeper？**
A: Kafka 内置的 Raft 共识协议，管理元数据（分区映射/Controller 选举）。去掉 ZK 的原因：元数据规模受限（ZK watch 百万分区吃力）、双系统运维复杂、故障恢复慢。KRaft 让元数据作为内部主题由 KRaft 仲裁管理，故障恢复快、支持百万级分区。

---

**下一篇**：[02-生产者可靠性-acks与幂等事务](02-生产者可靠性-acks与幂等事务.md)。
