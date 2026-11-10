# 06 · Bitmap：签到系统（月日历 + 连续签到 + 补签）

"每天打开 App 签到领积分"是 retention（留存）运营的标配功能。Bitmap 让 1 亿用户 × 每月签到只占约 150MB，且连签/补签都是位运算。

## 需求（真实签到产品的功能清单）

| 功能 | 技术形状 |
|------|---------|
| 每日签到，本月签了哪几天 | SETBIT 当天 |
| 月历视图（哪天签了打勾） | 读整段位串 |
| 本月签到 X 天 | BITCOUNT |
| **连续签到 N 天**（断签重计） | 位串区间分析 |
| 补签卡（补昨天） | SETBIT 回填 + 补签卡计数 |
| 签到送积分（防重复） | BIT 返回值天然幂等 |

## 数据设计与核心命令

```
key = sign:{userId}:{yyyyMM}     offset = 日期-1（1号=bit0）
```

```bash
# 11月1日签到
SETBIT sign:1001:202611 0 1        # 返回 0（之前没签过）
SETBIT sign:1001:202611 4 1        # 11月5日签到
SETBIT sign:1001:202611 5 1        # 11月6日

# 本月签到天数
BITCOUNT sign:1001:202611          # → 3

# 今天签了吗（BIT 返回旧值，天然幂等：重复签到返回1，不再发积分）
GETBIT sign:1001:202611 4

# 取本月 1~7 日的位作为一个无符号整数（一次拿一周的状态）
BITFIELD sign:1001:202611 GET u7 0
```

## 连续签到天数（本场景唯一的算法难点）

位串里"**从今天往回数连续的 1**"。没有现成命令，应用层算——把位串取成一个数，从今天的位置往下数：

```java
// BITFIELD GET u7 offset：一次取7天状态（也可整月 GET u31 0）
List<Long> bf = redis.opsForValue()
        .bitField("sign:1001:202611",
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(31))
                        .valueAt(0));
long bits = bf == null || bf.get(0) == null ? 0 : bf.get(0);

int continuous = 0;
int todayOffset = dayOfMonth - 1;
for (int i = todayOffset; i >= 0; i--) {      // 从今天往回数
    if (((bits >> i) & 1) == 1) continuous++;
    else break;                                // 断签即停——这正是"连续"的定义
}
```

跨月连续：昨天是上月最后一天时，取上月 bitmap 的最后一个 bit 接着数（两段拼接）。

**为什么不用 BITCOUNT 减法**：`本月签到7天` ≠ `连续7天`。位串扫描是唯一正解。

## 补签（用掉的补签卡）

```java
public boolean backfill(long userId, int dayOffset) {
    String key = "sign:" + userId + ":" + yyyyMM;
    if (redis.opsForValue().getBit(key, dayOffset)) return false;   // 已签，无需补

    // 补签卡余额（String DECR，发放时 SET/INCR）
    Long remain = redis.opsForValue().decrement("sign:card:" + userId);
    if (remain == null || remain < 0) {
        redis.opsForValue().increment("sign:card:" + userId);   // 回滚
        throw new BizException("补签卡不足");
    }
    redis.opsForValue().setBit(key, dayOffset, true);   // 回填位
    pointsService.add(userId, 5);                        // 补签发积分（幂等由业务层）
    return true;
}
```

补签卡的扣减与位回填是两步——极端并发下多补几张的代价极小（运营可接受），要严谨就 Lua 原子化。

## 内存账（这个方案值钱的地方）

| 用户量 | 每人每月 | 总量 |
|--------|---------|------|
| 1 亿 | 31 bit ≈ 4B | **~400MB** |

对比 DB 方案：1 亿 × 12 月 × 1 行（~50B+索引）≈ TB 级。**Bitmap 用 4B/人/月 达成同样功能**——这是它存在的全部理由。注意 offset 稀疏问题：`SETBIT key 100000000 1` 会立即分配 12MB，所以**按月分 key**（offset 最大 31，每 key 几字节）。

## 一致性与可靠性

- **签到幂等**：`SETBIT` 返回旧值，旧值=1 说明今天已签 → 不重复发积分（位运算自带的幂等）
- **积分与签到位非原子**：极端场景"位签了积分没发"——签到流水落 DB/MQ（同验证码审计思路），对账补偿
- **Redis 挂了**：签到记录可从签到流水重建；或接受丢失（用户重签）

## 面试问法

**Q: 亿级用户签到怎么设计？**
A: 按月分 key 的 Bitmap：key=sign:uid:yyyyMM，offset=日期-1，SETBIT 签到（返回旧值天然幂等防重复发积分）、BITCOUNT 月天数、BITFIELD 取整月位串后从今天往回数连续签到（跨月拼上月末位）。1 亿用户仅数百 MB。补签卡用 String DECR 限次 + SETBIT 回填。签到流水落 DB/MQ 保证积分发放可对账补偿。

---

**下一篇**：[07-HyperLogLog-UV统计大屏](07-HyperLogLog-UV统计大屏.md)。
