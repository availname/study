# 08 · GEO：网约车的"附近司机"

打开网约车 App，地图上冒出周边的司机头像；你下单后系统从附近挑一个人派单。GEO 负责"筛选附近"，派单决策交给 MQ（呼应 mq 教学）。

## 需求拆解（网约车业务视角）

| 功能点 | 技术形状 |
|--------|---------|
| 司机上线/下线（进入/退出接单池） | 加入/移除 GEO |
| 司机位置每 5 秒上报 | **频繁更新同一成员** |
| 乘客呼叫：找 3km 内空车 | 半径查询 |
| 展示距离 | 两点距离计算 |
| 多城市数据隔离 | 按城市分 key |

## 命令全流程（跟做）

```bash
# 司机A上线/位置更新（GEOADD 对同一 member 是覆盖式更新——天然适合"报点"）
GEOADD drivers:hangzhou 120.155 30.274 司机A
GEOADD drivers:hangzhou 120.210 30.315 司机B
GEOADD drivers:hangzhou 120.161 30.281 司机C

# 乘客在 (120.15, 30.28) 呼叫：3km 内最近 2 个司机（带距离）
GEOSEARCH drivers:hangzhou FROMLONLAT 120.15 30.28 BYRADIUS 3 km ASC COUNT 2 WITHCOORD WITHDIST

# 两点距离（预估接驾路程）
GEODIST drivers:hangzhou 司机A 司机C km

# 司机下线（退出接单池）：GEO 底层是 ZSet，移除用 ZREM
ZREM drivers:hangzhou 司机A

# 顺带：某成员的位置
GEOPOS drivers:hangzhou 司机B
# 哈希编码（调试用）
GEOHASH drivers:hangzhou 司机A
```

## 四个工程要点

### 1. 底层是 ZSet：想通这一点，一切都通

GEOADD = 把经纬度编码成 52bit 的 **Geohash 值塞进 ZSet 的 score**（编码相邻位置的 score 也相邻 → 半径查询=score 范围查询）。所以：

- `ZREM` 就是"下线"
- `ZCARD drivers:hangzhou` = 在线司机数
- 大 Key 警惕同样适用：单城百万司机没问题（ZSet 百万成员 OK），但**按城市分 key** 是必须的（`drivers:{cityCode}`），不能全国一个 key

### 2. 写放大：司机 5 秒报一次点

10 万在线司机 × 每 5 秒 GEOADD = 2 万写 QPS——单实例可扛，但要：

- 上报先落 Kafka → 消费者批量 GEOADD（削峰，与 UV 埋点同款链路）
- 报点频率动态调整：空驶司机 5s，接单中的 30s（业务降噪）

### 3. GEO 只回答"谁在附近"，不回答"派给谁"

筛选出 3km 内 8 个司机后，派单逻辑（就近优先+评分+顺路度+公平性）是业务层的事，且要用**MQ 派单**：

```
乘客呼叫 → GEOSEARCH 候选司机 → 生成派单任务 → 队列依次尝试
   → 司机1 15秒未应答 → XCLAIM/超时 → 转派司机2（延迟消息驱动）
```

如果直接同步找"最近的司机"推送，会遇到并发竞争（多个乘客同时选中同一个司机）。派单队列化（每个候选司机排队处理）才能做公平与防竞争——**GEO 是空间索引，MQ 是秩序保障**，各司其职。

### 4. 漂移数据清理

司机 App 杀掉没发"下线"→ 永远留在池子里（幽灵车）。解法：**双结构**——GEO 池 + `last_report:{driverId}` 时间戳，定时任务扫描超时未报点的司机 ZREM 踢出（与 mq-mall 秒杀对账 job 同款思路：Redis 状态可漂移，靠周期性校正）。

## 与 MySQL 的对比（为什么不用 SQL 算附近）

```sql
-- DB 方案：全表算距离再排序 —— 十万司机每请求全表扫描
SELECT * FROM driver
WHERE ACOS(...) < 3   -- 三角函数全表计算，无法走索引
```

GeoHash 把二维位置编码成一维数值 → ZSet 范围查询 O(logN + M)。**这就是 stage2/02 "数据结构决定性能"的活教材**：同一问题换结构，全表扫描变索引查询。

## 面试问法

**Q: 附近的人/网约车怎么实现？**
A: Redis GEO（底层 ZSet 存 Geohash 编码的 score）：GEOADD 按城市分 key 维护在线司机池，覆盖更新天然支持 5 秒报点；GEOSEARCH FROMLONLAT BYRADIUS 找候选，GEODIST 算距离。工程要点：位置上报过 Kafka 削峰批量写入；GEO 只做空间筛选，派单决策用 MQ 队列防并发竞争；定时任务踢超时未报点的幽灵车（Redis 状态周期校正）。

---

**Stage 5 完成**。8 个场景全部走完"需求→选型→设计→坑→面试"链路，回到 [README](../README.md) 完成总任务清单。
