# 03 · List：Feed 流时间线（推模式 vs 拉模式）

微博/朋友圈的"关注时间线"是 List 最重量的场景，也是**大厂面试的高频系统设计题**。核心矛盾一句话：**我关注的人发了动态，怎么出现在我的时间线里？**

## 两种模式

### 推模式（写扩散 / Fan-out on write）

发布时，把动态 ID **主动塞进每个粉丝的时间线**：

```bash
# 大V发动态 id=9001，他有 3 个粉丝（真实是百万级）
LPUSH inbox:fan1 9001
LPUSH inbox:fan2 9001
LPUSH inbox:fan3 9001
LTRIM inbox:fan1 0 999    # 每人只留 1000 条（防大 Key）
```

粉丝刷时间线 = `LRANGE inbox:fan1 0 9`，纯读缓存，**极快**。

### 拉模式（读扩散 / Fan-out on read）

发布只写自己的"发件箱"；粉丝刷时间线时**现场聚合**：

```bash
# 大V发布只写一条
LPUSH outbox:vip1 9001

# 粉丝刷时间线：取关注列表 → 逐个 LRANGE 各人的 outbox → 按时间归并排序 → 前 10 条
```

### 对比（这道表的每格都是考点）

| | 推模式 | 拉模式 |
|--|--------|--------|
| 发布成本 | 高：粉丝百万 = 百万次 LPUSH（写放大） | 低：只写自己一条 |
| 读取成本 | 低：直接读自己的 inbox（O(1)） | 高：聚合 N 个关注者的 outbox（O(N×M) 归并） |
| 不活跃粉丝 | 百万僵尸粉的 inbox 白占内存 | 无浪费 |
| 历史动态 | 新粉丝看不到旧的（inbox 只有最近的） | 天然能拉到全部 |
| 典型用户 | 朋友圈（关系小、互动强） | 微博大V（粉丝百万） |

### 真实大厂方案：推拉结合

```
普通用户发布（粉丝 < 阈值，如 1万）→ 推模式：LPUSH 到所有粉丝 inbox
大V发布（粉丝百万）→ 拉模式：只写 outbox，粉丝刷时"自己的 inbox（来自普通人）+ 现场拉大V的 outbox"归并
```

阈值是动态运营参数。这个设计把发布成本从 O(粉丝数) 压到 O(普通粉丝数)，大V的百万写放大被消灭。

## 实现细节（推模式的完整工程账）

```java
// 发布（普通用户）
@Transactional
public void post(long userId, String content) {
    long feedId = feedDb.insert(userId, content);        // 动态正文落 DB（ inbox 只存 ID！）
    List<Long> fans = followService.getFans(userId);     // 真实实现：粉丝列表来自 MQ 异步扩散
    for (long fan : fans) {
        redis.opsForList().leftPush("inbox:" + fan, String.valueOf(feedId));
        redis.opsForList().trim("inbox:" + fan, 0, 999); // 每人留 1000 条
    }
}
```

**四个工程要点**：

1. **inbox 只存 feedId**（10 字节），不存正文——正文在 DB，inbox 是"目录"
2. **百万粉丝的扩散走 MQ 异步**：发布接口不等百万 LPUSH（这就是 [mq 教学](../../mq/README.md) 的 fanout 应用！），扩散任务进队列慢慢推，几秒内完成
3. **LTRIM 上限**：每人 1000 条，翻页翻到底走 DB 查（冷数据下沉）
4. **读时补全**：LRANGE 拿到 ID 列表 → 批量查 DB/缓存拿正文、作者、点赞数（MGET 合并）

### 分页的坑：LRANGE 的"翻页重叠"

刷到第 3 页时有人发了新动态 → LPUSH 导致所有下标 +1 → **翻页出现重复/漏看**。

解法：**用 feedId 做游标**（不是下标）：`LRANGE` 取到后记住最后一条的 id，下一页改用 `LRANGE`+过滤，或改用 ZSet（score=时间戳，`ZRANGEBYSCORE (lastId 精确续读）`。**Set/ZSet 版 Feed 流（微博实际用的）天然没有重叠问题**——这也是很多公司 inbox 用 ZSet 存的原因。

## 动手推演（redis-cli 跟做）

```bash
# 推模式最小复现
LPUSH inbox:fan1 9003 9002 9001
LRANGE inbox:fan1 0 9
LTRIM inbox:fan1 0 2            # 只留3条
LRANGE inbox:fan1 0 -1
# 拉模式最小复现
LPUSH outbox:vip 9004
# 粉丝读时：LRANGE outbox:vip 0 2 + LRANGE inbox:fan1 0 2 → 应用层按时间归并取前几条
```

## 面试问法

**Q: 微博 Feed 流怎么设计？推还是拉？**
A: 推拉结合。推模式（写扩散）：发布时 LPUSH 动态ID到每个粉丝的 inbox List（LTRIM 限1000条防大Key），读取 O(1) 极快，但百万粉大V写放大严重且僵尸粉占内存；拉模式（读扩散）：发布只写自己 outbox，读者聚合关注者归并，发布便宜读取贵。方案：普通用户推、大V拉（阈值运营可调），正文落 DB、inbox 只存 ID，百万粉扩散走 MQ 异步。翻页用 ID 游标不用下标防重叠；成熟方案 inbox 用 ZSet（score=时间戳）天然续读。

---

**下一篇**：[04-Set-抽奖与社交关系](04-Set-抽奖与社交关系.md)。
