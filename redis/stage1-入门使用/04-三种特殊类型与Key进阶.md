# 04 · 三种特殊类型与 Key 进阶

## 1. Bitmap：一个 bit 一件事 → 签到 / 在线状态

亿级用户的"今天是否签到"，每人每天只占 **1 bit**。1 亿人签到表 ≈ 12MB。

```bash
# 用户 1001 在 2026 年第 11 月签到（offset = 天数）
SETBIT sign:1001:202611 0 1     # 1 号签了
SETBIT sign:1001:202611 3 1     # 4 号签了
GETBIT sign:1001:202611 3       # 4 号签了吗 → 1
BITCOUNT sign:1001:202611       # 本月签到天数 → 2
BITPOS sign:1001:202611 1       # 第一次签到是第几天
```

**连续签到 N 天怎么算**：取本 bitmap 与上一月的"尾部拼接"后 `BITCOUNT` 指定区间，或应用层算。登录态判断、活跃用户标记同理。

**内存公式**：`SETBIT key 100000000 1`（1 亿位）≈ 12MB——前提是 offset 分布不太稀疏；只 SETBIT 一个亿的 offset 也会分配 12MB，所以按月分 key。

## 2. HyperLogLog：亿级 UV 只用 12KB

统计"某页面今日有多少**不同**用户访问"。Set 精确但贵（1 亿 UV ≈ 数 GB）；HLL 用 0.81% 标准误差换**固定 ~12KB**：

```bash
PFADD uv:index:20261104 u1 u2 u1 u3     # 访客（重复 u1 自动去重）
PFCOUNT uv:index:20261104               # → 3（误差 0.81% 内）
PFMERGE uv:index:202611 uv:index:20261104 uv:index:20261103   # 合并出月 UV
```

- 能回答"今天 103,482 个 UV 吗"（差千八百个无所谓）
- 不能回答"用户 u1 今天来了吗"——**要去重精确判断用 Set 或 Bitmap**
- 选型口诀：**亿级计数要省内存用 HLL；要去重判断用 Set；只有 0/1 用 Bitmap**

## 3. GEO：附近的人 / 附近的车

底层是 ZSet（score 是地理位置编码），提供经纬度半径查询：

```bash
GEOADD drivers:hangzhou 120.15 30.28 司机A
GEOADD drivers:hangzhou 120.21 30.31 司机B
GEOADD drivers:hangzhou 120.16 30.29 司机C

GEOSEARCH drivers:hangzhou FROMLONLAT 120.15 30.28 BYRADIUS 3 km ASC COUNT 2
# 距离 3km 内最近的 2 个司机
GEODIST drivers:hangzhou 司机A 司机B km    # 两点距离
```

## 4. Stream：Redis 自己的消息队列（stage4/03 详解）

```bash
XADD mystream * order 1001 user u1     # * 自动生成消息 ID
XLEN mystream
XRANGE mystream - + COUNT 10
```

带消费者组、ACK、pending 列表——轻量 MQ（区别于 List 的"弹出即消失，断了就丢"）。stage4/03 与 RabbitMQ 对比细讲。

## Key 进阶四件事

### ① TTL 与三种过期行为

```bash
SET code 8848 EX 300
TTL code              # → 剩余秒数
PERSIST code          # 取消过期（变永久）
EXPIRE code 600       # 改 TTL（注意：SET 会清掉原 TTL！）
```

- **惰性删除**：访问时发现过期才删——省 CPU，可能留垃圾
- **定期删除**：后台每 100ms 随机抽一批带 TTL 的 key 检查删除——兜底
- **内存淘汰**：内存满时按策略强杀（stage2/04 详解）
- 坑：`SET key newValue` 会**清掉已有 TTL**！需要保 TTL 用 `SET key val EX s` 重设

### ② KEYS 是生产炸弹，用 SCAN

```bash
KEYS *            # O(N) 全量遍历并阻塞——百万 key 卡死整个 Redis几秒
SCAN 0 MATCH session:* COUNT 100   # 游标分批扫，不阻塞（可能返回重复，客户端去重）
```

### ③ EXISTS / DEL / UNLINK

```bash
EXISTS key        # 1 存在
DEL bigkey        # 同步删——删百万成员的大 Key 会阻塞主线程！
UNLINK bigkey     # 异步删（后台线程回收），大 Key 用它
```

### ④ 大 Key 意识（stage3/04 专章）

一个 key 的 value 过大（String > 10KB、集合 > 5000 成员）就是大 Key：读写慢、删除阻塞、迁移困难。**设计期就拆**：Hash 按用户分、List 用 LTRIM 截断、排行榜按月分 key。

## 类型命令备忘（redis-cli 里随时查）

```bash
TYPE key           # 先看类型
HELP @string       # 该类型所有命令清单（@hash @list @set @zset @stream...）
OBJECT ENCODING key # 值的底层编码（stage2/02 揭秘）
```

---

**动手任务**：① Bitmap 签到一周，BITCOUNT 出勤天数；② HLL 统计"今天访问过的不同学号"；③ SCAN 替代 KEYS 找 session:*；④ 观察 `SET key val` 后 TTL 变 -1 的坑。

**Stage 1 最后一节**：[05-SpringBoot整合Redis](05-SpringBoot整合Redis.md)。
