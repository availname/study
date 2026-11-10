# 04 · 大 Key 与热 Key：单线程架构下的两大定时炸弹

单线程模型（stage2/01）决定了：**任何慢操作都会阻塞全端**。大 Key 慢在"数据大"，热 Key 慢在"访问挤"。

## 一、大 Key（Big Key）

### 什么算大 Key

| 类型 | 危险阈值（经验） |
|------|----------------|
| String | value > 10KB（>1MB 严重） |
| Hash/List/Set/ZSet | 成员 > 5000 或总量 > 10MB |

### 危害链条

```
读写 10MB value → 主线程被占用几十 ms → 该期间所有命令排队 → 全端延迟飙升
删除 100w 成员的 Set → 阻塞更久（同步 DEL）→ 请求超时雪崩
过期删除同样阻塞 → 大 Key 惰性删除是隐形炸弹
内存不均（Cluster 单节点膨胀）/ 网卡打满 / 迁移卡死
```

### 怎么发现

```bash
redis-cli --bigkeys                      # 抽样扫描各类型最大的 key（生产可用，低峰跑）
MEMORY USAGE key SAMPLES 0               # 精确测单个 key
redis-rdb-tools / rdb 分析 dump.rdb      # 离线全量分析
```

### 怎么拆（对症下药）

| 大 Key 形态 | 拆法 |
|------------|------|
| 大 JSON 字符串 | 字段拆 Hash，按需读字段；超大对象只存 ID+热点字段 |
| 百万成员的 Hash/Set | **分桶**：`user:follow:{userId % 100}` 拆 100 个桶 |
| 无限增长的 List/消息堆积 | 上限控制（LTRIM）、过期清理、改用 Stream+XTRIM |
| 排行榜全量 | 按时间分 key（`rank:game:202611`），ZREMRANGEBYRANK 踢榜尾 |
| 删除 | **UNLINK 异步删**（stage1/04），绝不用 DEL 删大 Key |

**预防优于治理**：设计时就想好上限——"这个集合最多会长到多大？"没有答案的集合就是未来的大 Key。

## 二、热 Key（Hot Key）

### 什么是热 Key

单个 key 的 QPS 远超平均（如明星八卦的缓存、爆款商品的库存 key）。**单线程架构下**，这个 key 的访问都挤在一个 Redis 分片的一个核上——其它 key 再闲也没用。

危害：该分片 CPU 打满、延迟飙升；Cluster 里分片间冷热极度不均；极端时该 key 打挂节点（缓存失效 → DB 被击穿，连环反应）。

### 怎么发现

```bash
redis-cli --hotkeys                      # 需 LFU 淘汰策略支持
MONITOR                                  # 全命令流（生产慎用，本身很重）
客户端埋点上报 top key                    # 网关/应用层统计最实用
```

### 怎么治

| 手段 | 原理 |
|------|------|
| **本地缓存兜一层** | 应用进程内 Caffeine 缓存热 key（几百 ms~几 s TTL），大部分请求根本不出 JVM |
| **读写分离** | 从库扩容，读分摊到多个从库（stage2/05） |
| **热 key 打散（写场景）** | 库存这类"热写"单 key：拆成 N 个子 key（`stock:sku1:{0..9}` 各存 1/N），随机扣一个，最后合计（stage4/01 深化） |
| 限流降级 | 网关对该资源限流，宁可部分拒绝也不拖垮全局 |

## 三、两者共同的"架构级预防"

1. **Key 设计评审**：上限多大？会不会无限长？访问集中度如何？——写代码前问，别上线后救火
2. **监控三件套**：`--bigkeys` 定期跑、`evicted_keys`/`expired_keys` 盯水位、客户端 top-key 上报
3. **压测**：上大促前用真实数据形状压一遍，Redis 单实例经验水位：QPS 5~8 万、内存 70% 预警

## 面试问法

**Q: 什么是大 Key/热 Key，怎么发现和解决？**
A: 大 Key 是 value 过大的 key（String>10KB、集合>5000 成员），单线程读写阻塞全端、UNLINK 异步删、设计期按业务分桶防患，--bigkeys/MEMORY USAGE 发现。热 Key 是单 key QPS 极高，挤爆单分片单核，本地缓存兜一层+读写分离+热写拆子 key 限流。共同预防：Key 设计评审+监控（evicted_keys、top-key 上报）+ 大促压测。

---

**Stage 3 完成**。进入 [Stage 4-高并发实战](../stage4-高并发实战/01-秒杀库存与Lua原子操作.md)——把 stage3 的所有武器投入实战。
