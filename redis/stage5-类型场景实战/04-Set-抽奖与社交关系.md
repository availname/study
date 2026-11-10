# 04 · Set：抽奖系统与社交关系

Set 的两个杀手锏：**天然去重**（抽奖防重复中奖）和**服务端交并集**（共同好友）。本篇一个电商抽奖 + 一个社交场景把它们讲透。

## 场景 A：抽奖系统（三种真实玩法）

### 玩法 1：报名抽 N 人（不放回抽奖）

运营活动"报名用户中抽 3 人送 iPhone"：

```bash
SADD lottery:20261105 u1 u2 u3 u4 u5      # 报名（重复 SADD 自动去重）
SCARD lottery:20261105                     # 报名人数
SPOP lottery:20261105 3                    # 随机弹 3 人（弹出即移除，绝不可能重复中奖）
SMEMBERS lottery:20261105 2 5              # 剩余报名者
```

`SPOP` 的"移除性"就是公平性——**抽中的人从池子里消失，物理上不可能重复中奖**。

### 玩法 2：放回抽奖（人人都能"再试一次"）

游戏里"每天 3 次抽奖机会"，每次独立有概率：

```bash
SRANDMEMBER lottery:pool 1                 # 随机看一人（不移除）
# 概率控制不在 Set——Set 只管"参与名单"，概率逻辑在应用层：
# 一等奖2%/二等奖10%/谢谢观看88% → random.nextInt(100) 分段判断
```

**分工要清晰**：Set 管"谁参与了"（去重、人数、防薅），概率引擎在应用层。把概率塞进 Redis 是设计错误。

### 玩法 3：防薅羊毛的叠加校验

```bash
SADD activity:joined:20261105 u1           # 全活动参与名单（一人一活动）
SADD activity:prized:20261105 u1           # 本活动中奖名单（一人一中一次）
# 中奖逻辑前校验：
SISMEMBER activity:prized:20261105 u1      # 中过没有？
SISMEMBER activity:blocked:20261105 u1     # 黑名单？（风控标记）
```

三个 Set + 两个 `SISMEMBER`（O(1)）= 一套防重复中奖、防黑名单的闸门。中奖名单同时是**风控与审计数据**，最终落 DB。

### 大 Set 意识

百万人的活动池：SCARD O(1) 无碍；但 `SMEMBERS` 全量拉取是 O(N) 大 Key 操作（stage3/04）——**全量名单导出走 SSCAN 分批**。活动结束 `UNLINK` 整个池子或按活动 ID 过期（key 带日期天然可 EXPIRE）。

## 场景 B：社交关系（共同好友 / 可能认识的人）

### 数据设计

```bash
SADD follow:u1 u2 u3 u5 u9      # u1 关注的人
SADD follow:u2 u3 u5 u7         # u2 关注的人
```

### 三个经典查询（全部是服务端交并集）

```bash
SINTER follow:u1 follow:u2       # 共同关注（u3 u5）
SUNION  follow:u1 follow:u2      # u1 和 u2 关注过的所有人
SDIFF   follow:u1 follow:u2      # u1 关注但 u2 没关注（"我关注他，快去关注"推荐）
```

**"可能认识的人"推荐**：`SDIFF friends:朋友的朋友 - friends:我`——朋友的朋友中我还没关注的。

### 两个工程坑

1. **SINTER/SDIFF 是 O(N) 全量计算**——百万粉大V的集合做交集会阻塞（stage2/01 纪律）。大集合的复杂社交计算：
   - `SINTERSTORE dst a b` 把结果存目标 key（仍是同步计算，慎用）
   - 真实大厂：关注关系在 DB/图数据库，Set 只做**小规模**在线判断（是否关注 SISMEMBER O(1)）；共同好友这类重计算走离线任务
2. **取交集的分页**：SINTER 不支持分页——大集合求交集后 SRANDMEMBER 采样展示即可（"共同好友 128 位，展示几个"的 UI 语义）

### 关注/取关的操作原子性

```java
public void follow(long me, long target) {
    redis.opsForSet().add("follow:" + me, String.valueOf(target));   // 我关注的人
    redis.opsForSet().add("fans:" + target, String.valueOf(me));     // 他的粉丝
    db.insertFollow(me, target);                                     // DB 落关系（真主存）
}
```

两个 Set + DB 三处写不是原子——中断会有不一致，用"DB 主 + 定时对账修复 Set"兜底（与缓存一致性同思路：Redis 是加速层，DB 是真相）。

## 类型对比彩蛋：为什么关注列表不用 ZSet？

ZSet 也能存（score=关注时间，可按时间排序展示"最新关注"）——**产品要按时间排序就用 ZSet，只要"是否存在+去重"就用 Set**。类型选择永远由产品交互倒推，不由技术偏好决定。

## 面试问法

**Q: 用 Redis 设计一个抽奖系统。**
A: Set 存报名池：SADD 天然去重防重复报名，SCARD 实时人数；不放回抽奖用 SPOP（弹出即移除，物理防重复中奖），概率型抽奖 Set 只管名单、概率在应用层；叠加"活动中奖名单"Set + SISMEMBER 防一人多中，黑名单 Set 做风控闸门。大池子导出用 SSCAN 分批，活动结束 key 带日期可 EXPIRE 清理。中奖结果落 DB 供审计。

**Q: 共同好友怎么实现？**
A: 每人一个关注 Set，SINTER 求 O(N) 交集——小集合在线算；百万级大V的集合在线交集会阻塞，真实系统把重社交计算放离线任务，在线只做 SISMEMBER 判断。"可能认识的人"用 SDIFF 朋友的朋友。

---

**下一篇**：[05-ZSet-热搜榜](05-ZSet-热搜榜.md)。
