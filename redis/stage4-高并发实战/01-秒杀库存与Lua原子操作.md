# 01 · 秒杀库存与 Lua 原子操作

mq-mall 的秒杀（[mq 教学坑二/秒杀篇](../../mq/stage4-高并发实战/01-削峰填谷-秒杀系统.md)）已经用过这套方案——本章把它讲透，它是 Redis 高并发的集大成案例。

## 问题：并发扣库存为什么会超卖

"查库存 → 判断 → 扣减"三步在多线程下不是原子的：

```
库存=1
线程A：GET stock → 1 → 判断 ok → DECR → 0
线程B：GET stock → 1 → 判断 ok → DECR → -1   ← 超卖！
```

## 解法一：先扣后判（版本一，够用但不优雅）

```java
Long remain = redis.opsForValue().decrement("stock:sku1");   // DECR 是原子的
if (remain == null || remain < 0) {
    redis.opsForValue().increment("stock:sku1");   // 扣过头了，还回去
    return "已售罄";
}
```

DECR 原子所以不会少扣，但**每笔失败的请求都产生"扣了又还"的双倍写**，洪峰下浪费一半流量。

## 解法二（标准答案）：Lua 脚本"判断+扣减"原子化

```lua
-- seckill.lua
if redis.call('GET', KEYS[1]) == false then return -1 end
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock <= 0 then return 0 end
redis.call('DECR', KEYS[1])
return 1
```

```java
DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaText, Long.class);
Long r = redis.execute(script, List.of("stock:sku1"));
// r==1 抢到，r==0 售罄——只有一次网络往返，且全程原子
```

**Lua 的本质**：把多条命令打包成**一个原子单元**在 Redis 服务端执行，执行期间不插入其它命令——等于免费获得了一把"服务端互斥锁"。所有"检查+操作"复合逻辑的标准答案（限流、锁、秒杀）。

升级版（含一人一单，mq-mall 同款）：

```lua
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then return -2 end  -- 已抢过
local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
if stock <= 0 then return 0 end
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 1
```

## 完整漏斗：Redis 只是其中一层

单靠 Redis 判断不够，生产的秒杀是四层漏斗（与 stage4/01 呼应）：

```
前端限流(按钮置灰/验证码) → 网关限流(Sentinel) → Redis Lua 预扣 → MQ 异步落库
                                                    │
                                              DB 层 WHERE stock>0 兜底（防超卖的最后防线）
```

**双保险原则（背下来）**：Redis 挡 99% 并发（效率层），DB 约束保 100% 正确（正确层）。`UPDATE stock SET stock=stock-1 WHERE sku_id=? AND stock>0` 影响行数=0 说明 Redis 与 DB 漂移 → 补偿修正。

## 附加工程细节

| 细节 | 做法 |
|------|------|
| 热 Key（单 SKU 库存是热写 key） | 拆 10 个子 key 各存 1/10 库存，随机扣，最后合计（stage3/04） |
| 失败回滚 | 消费端落库失败时 Redis `INCR` 归还 + `SREM` 用户（幂等归还用 Lua 防重复还） |
| 预热 | 活动开始前把库存 `SET` 进 Redis，别等活动开始时打 DB |
| 对账 | 活动结束对账 job：Redis 扣减数 == DB 订单数 == DB 扣减数，差异补偿 |
| 脚本管理 | Lua 文件放 resources，`DefaultRedisScript` 加载；改逻辑不用改代码 |

## 压测预期（redis-demo 的 `/seckill` 接口）

1000 库存、3000 并发请求：接口耗时 < 100ms，最终库存恰好 0、订单 1000、零超卖、售罄拒绝 2000。demo 用 JMeter 或简单并发脚本即可复现。

## 面试问法

**Q: Redis 秒杀怎么设计？**
A: Lua 脚本把"查库存+判断+扣减+一人一单"打包原子执行（服务端互斥），抢到发 MQ 异步建单削峰；DB `WHERE stock>0` 兜底防超卖，Redis/DB 漂移用对账+补偿；热 SKU 拆子 key 分散热写；双保险原则——Redis 管效率，DB 管正确。

---

**下一篇**：[02-排行榜签到与UV统计](02-排行榜签到与UV统计.md)。
