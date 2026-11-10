# 05 · RedLock 争议与 Fencing Token：一场关于"锁的正确性"的世纪之争

> 🎬 **配套动画**：[diagrams/redlock-debate.html](../../diagrams/redlock-debate.html)——五剧本四角色推演：主从丢锁 → RedLock 抢锁 → GC 停顿 → 时钟跳变 → fencing token 拒收旧凭证，Martin/antirez 双辩手立场面板。

> 🎯 **定位**：[03-分布式锁](03-分布式锁.md) 留了个尾巴："RedLock 争议很大"——这篇把这场 2016 年的著名论战（Martin Kleppmann vs Redis 作者 antirez）讲透。它是分布式系统面试的**深水区考题**：能讲清"争的是什么"，说明你真的理解了分布式锁，而不是背了 SETNX。

## 场景引入：RedLock 是什么、为什么被发明

[03 篇](03-分布式锁.md) 讲过 Redis 锁的固有弱点：**主从切换丢锁**——

```
A 在主库 SET NX 成功 → 主库宕机（锁 key 还没异步复制到从库）
→ 哨兵提升从库为新主 → B 在新主上 SET NX 也成功
→ A、B 双持锁，同时改同一行库存 → 互斥性破灭
```

antirez 的补丁就是 **RedLock**：不用主从，起 **5 个完全独立的 Redis master**（互不复制）：

```
1. 记下开始时间 t0
2. 依次向 5 个实例抢锁（SET NX，TTL 相同，如 10s）
3. 拿到 ≥3 个（过半）且 耗时 < 锁 TTL → 认定抢锁成功
4. 锁的有效期 = TTL − (t1 − t0)          ← 扣掉抢锁耗时
5. 失败：向所有实例发解锁
```

思想：过半写入 = 多数派共识（[11-分布式基础](../../java-core/11-分布式系统基础.md) 的 Quorum 思想，Raft 同款）——任何一个实例挂了都不影响锁存在。

## Martin Kleppmann 的两记重拳

Martin（《DDIA》作者）2016 年发文《How to do distributed locking》，指出 RedLock **既不够安全、又不够高效**：

### 重拳一：进程暂停（GC 停顿）让锁失效——与算法无关

```
t1: 客户端 A 抢到锁，准备写存储
t2: A 进程 GC 停顿 10 秒（STW，全宇宙都拦不住）
t3: 锁过期 → B 抢到锁 → B 写存储
t4: A GC 结束，浑然不知，继续写存储
→ 双持锁事故照常发生 —— RedLock 的过半共识对此毫无办法
```

**这不是 Redis 锁独有的问题**——单机锁在 GC 停顿后也会"释放了锁但线程还活着"。分布式环境只是让窗口更容易撞上。

### 重拳二：时钟跳变破坏算法根基

RedLock 的"有效期 = TTL − 耗时"计算**依赖各机器时钟基本准确**。若某实例 NTP 校时向前跳 30s：它上面的锁瞬间"过期"被别人抢走 → 多数派被绕过。antirez 用"要求时钟单调（CLOCK_MONOTONIC）+ 运维保证不跳变"回应——Martin 的反驳：**你的安全性不能建立在运维纪律上**。

### 结论对撞（面试原题）

| 立场 | 主张 |
|------|------|
| Martin | RedLock 不安全也不必要；要正确性就用**支持 fencing 的共识系统**（ZooKeeper/etcd），或直接在存储层做 |
| antirez | 停顿问题任何锁方案都有；时钟可控前提下 RedLock 可用；"绝对安全"不存在，工程是权衡 |
| **工程界的实际共识** | 双方都对——所以 RedLock 在 Redisson 中已被标记废弃，**生产几乎没人用它**；正确性问题用下面的方案解 |

## Fencing Token：Martin 给出的正解（比锁更重要）

锁不可靠，那就**让"持锁凭证"本身可验证新旧**：

```
1. 每次抢锁成功，锁服务返回一个【单调递增】的 fencing token（#33）
2. 客户端带着 token 去写存储
3. 存储层记下见过的最大 token（#34）——收到 #33 直接拒绝（旧凭证）
→ 即使 GC 停顿后 B 用 #34 先写，A 醒来拿 #33 也写不进去
```

**你早就用过 fencing token 了，只是没意识到**：

| 场景 | fencing token 形态 |
|------|-------------------|
| 乐观锁 | `UPDATE ... SET version=version+1 WHERE version=#{v}` 的 **version** |
| 订单状态机 | `UPDATE ... WHERE status=10` 的 **status 前置条件**（CAS） |
| Redis 预扣+DB 兜底 | DB `WHERE stock>=count` 的 **affectedRows 检查** |

——**正确性从来不是锁给的，是存储层 CAS 给的**。锁只是让 99.9% 的并发在到达 CAS 之前就被挡掉，省资源而已。

## 该用什么：决策表（背这张就够）

| 场景 | 正确姿势 | 理由 |
|------|---------|------|
| 效率锁（防重复干活：定时任务/缓存重建） | Redisson 单实例/哨兵（[03 篇](03-分布式锁.md)） | 偶发双持锁 = 重复干一次活，无损失，不值得为它上复杂方案 |
| 正确性锁（绝不允许并发：扣款/库存） | **Redis 锁只做第一道过滤，DB 层 CAS/唯一索引做最终裁决** | 锁失效顶多多一层 CAS 竞争，数据不会错 |
| 跨服务协调（选主/分布式任务分配） | ZooKeeper / etcd（session+watch 语义原生） | 共识协议保证，fencing 原生支持 |
| RedLock | ❌ 不用（Redisson 已废弃，争议未决） | 复杂度高、安全论证有缺口、收益小 |

**mq-mall 的答案**（对照记忆）：秒杀用 Redis Lua 预扣挡住 99% 流量（效率层），DB `WHERE stock>=count` 兜住 100% 正确性——**这就是 fencing token 思想的项目落地**，与支付 CAS 状态机（[15-支付系统](../../java-core/15-系统设计·支付系统.md)）同构。

## 面试问法

**Q: RedLock 了解吗？为什么生产不用？**
A: RedLock 是向 5 个独立 Redis 过半抢锁的多数派方案，试图解决主从切换丢锁。Martin Kleppmann 指出两个缺口：①GC 停顿/进程暂停下，锁过期后客户端恢复继续写，过半共识拦不住——任何锁都有此问题；②算法依赖时钟基本准确，NTP 跳变可绕过多数派。工程结论：效率场景用 Redisson 单实例足够；正确性场景锁只做过滤，靠存储层 CAS/唯一索引（fencing token 思想）兜底；跨服务协调用 ZooKeeper/etcd。所以 Redisson 已废弃 RedLock，生产也不采用。

---

回 [redis README](../README.md) · 前作：[03-分布式锁](03-分布式锁.md) · 配套动画：[distributed-lock.html](../../diagrams/distributed-lock.html) · 深入阅读：Martin 原文《How to do distributed locking》与 antirez 的回应《Is Redlock safe?》
