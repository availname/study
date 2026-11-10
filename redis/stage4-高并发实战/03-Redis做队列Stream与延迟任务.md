# 03 · Redis 做队列：List、Stream 与延迟任务

学了 [mq 教学](../../mq/README.md)再看本章会非常快——Redis 也能当轻量 MQ 用，但要认清它的能力边界。

## 一、List：最原始的队列（了解其缺陷）

```
生产者 LPUSH order:queue msg      消费者 BRPOP order:queue 5（阻塞弹出）
```

BRPOP 解决了轮询，但**消息弹出即从队列消失**：

| 缺陷 | 后果 |
|------|------|
| 无 ACK | 消费者取出后崩溃 → 消息丢失 |
| 无消费者组 | 多消费者抢消息靠弹出竞争，无负载管理 |
| 无堆积回溯 | 消费失败消息已不在，无法重投 |

适合"丢一两条无所谓"的轻任务（发个通知、清个缓存）。

## 二、Stream：Redis 5.0 的"正经"消息队列

对标 Kafka 的设计：消息**追加在流里不删除**、有消费者组、有 ACK、有 pending 状态。

```
XADD orders * order_no 1001                 # 生产（* 自动 ID：时间戳-序号）
XLEN orders

# 消费者组：一组消费者分摊消息，各自独立 offset
XGROUP CREATE orders mygroup 0              # 建组（0=从头消费）
XREADGROUP GROUP mygroup consumerA COUNT 10 BLOCK 5000 STREAMS orders ">"
# ">"=只取没消费过的新消息；consumerA/B/C 分摊

# 处理完必须 ACK，否则进入 pending 列表
XACK orders mygroup <messageId>

# 查"拿了没 ACK"的消息（消费者崩溃的场景）→ 重新分配处理（XCLAIM/XAUTOCLAIM）
XPENDING orders mygroup
```

| 能力 | List | Stream | 对比 RabbitMQ/Kafka |
|------|------|--------|---------------------|
| 消息持久化 | 弹出即删 | 流内保留（需 XTRIM 控制） | 保留可回溯 ✓ |
| ACK/重投 | ❌ | ✓（pending+XCLAIM） | ✓ |
| 消费者组 | ❌ | ✓ | ✓ |
| 广播 | ❌ | ✓（多组各自消费） | ✓ |
| 可靠性 | 弱 | 中（受 Redis 持久化/主从异步限制） | 强（专门为消息设计） |

## 三、延迟任务：三种实现

订单超时取消、预约提醒……[mq 教学 stage4/02](../../mq/stage4-高并发实战/02-订单超时自动取消.md) 讲过 MQ 方案，Redis 版：

### 方案 1：ZSet 轮询（经典）

```java
// 存：score=到期时间戳
redis.opsForZSet().add("delay:queue", JSON.toJSONString(task), dueTimestamp);
```

```java
// 扫描线程（或定时任务）：
Set<String> due = redis.opsForZSet().rangeByScore("delay:queue", 0, System.currentTimeMillis());
due.forEach(this::handle);
redis.opsForZSet().remove("delay:queue", due.toArray());   // 处理后移除（多实例要 Lua 原子取+删防重复）
```

要点：多实例并发扫描用 **Lua 原子"取到期+删除"** 防重复消费；扫描间隔决定精度（秒级）。

### 方案 2：key 过期通知（不推荐当主方案）

key 过期时 Redis 发 `expired` 事件（需开 `notify-keyspace-events Ex`），监听频道触发任务。**缺陷**：惰性/定期删除让过期事件**不保证及时甚至不保证发生**——只能当辅助，别当主方案。

### 方案 3：生产正经方案

延迟量大、精度要求高 → RabbitMQ 延迟插件 / RocketMQ 延迟消息（mq 教学坑五）；Redis ZSet 适合中小量级、已有 Redis 不想加组件的场景。

## 四、选型结论

| 场景 | 选择 |
|------|------|
| 项目已有 MQ，消息要可靠 | 用 MQ（永远优先） |
| 小系统不想加组件、任务可容忍极小概率丢失 | Redis Stream |
| 轻量"干了就丢"任务 | List |
| 延迟任务中小量级 | ZSet 轮询 |
| 大规模、高可靠、事务消息 | Kafka/RocketMQ |

**一句话**：Redis 是"能客串 MQ 的缓存"，不是专业 MQ——数据可靠性（持久化/主从异步复制）决定了它扛不住"消息绝不能丢"的场景。

## 面试问法

**Q: 用 Redis 实现消息队列有哪些方案？**
A: List 的 LPUSH+BRPOP 最简单但弹出即删无 ACK，适合可丢任务；Stream 是 5.0 正解：流内保留+消费者组分摊+ACK+pending 列表 XCLAIM 重投，对标 Kafka；延迟任务用 ZSet score=到期时间戳轮询（Lua 原子取删防重复）。但 Redis 持久化和主从复制是异步的，消息可靠性弱于专业 MQ，生产消息必达场景仍选 RabbitMQ/RocketMQ。

---

**下一篇**：[04-缓存设计模式与选型总结](04-缓存设计模式与选型总结.md)。
