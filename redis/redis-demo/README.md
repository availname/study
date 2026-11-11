# redis-demo：Redis 教学配套可运行工程

Spring Boot 3.2 + Redis 7，对应教程四个阶段的全部知识点。无其它依赖（不需要 MySQL/MQ），起一个 Redis 容器即可跑。

## 运行

```bash
cd redis/redis-demo
docker compose up -d            # Redis 7 @ localhost:6381（与 mq-mall 的 6380 独立）
mvn spring-boot:run             # 应用 @ localhost:8081
```

对照观察工具：`docker exec -it redis-learn redis-cli`（每打一个接口就去 cli 里看数据）。

## 接口清单（按学习顺序）

| 接口 | 验证什么 | 对应教程 | 玩法 |
|------|---------|---------|------|
| `GET /types/string` | String+TTL | stage1/03 | cli 里 `TTL demo:captcha:13800001111` 观察 |
| `GET /types/counter` | 原子计数 | stage1/03 | 连点几次看自增 |
| `GET /types/hash?sku=sku1` | Hash 购物车 | stage1/03 | `HGETALL demo:cart:1001` |
| `GET /types/list?item=x` | List 最新动态 | stage1/03 | LTRIM 自动留 10 条（防大 Key） |
| `GET /types/set?user=u2` | Set 去重抽奖 | stage1/03 | u1 再提交一次→"重复报名被拦" |
| `GET /types/zset?player=玩家A&delta=100` | ZSet 排行榜 | stage1/03 | `ZREVRANGE demo:rank:game 0 -1 WITHSCORES` |
| `POST /captcha/send/{phone}` | 验证码 | stage1/05 | 返回验证码，5 分钟过期 |
| `POST /captcha/verify/{phone}/{code}` | Lua 原子校验 | stage1/05 | 重复校验第二次必然 false（一次性） |
| `POST /rank/{player}/{delta}` | 加分 | stage4/02 | `POST /rank/玩家B/500` |
| `GET /rank/top` · `GET /rank/{player}/rank` | 前三/名次 | stage4/02 | |
| `POST /lock/execute/{res}` | 分布式锁+3秒业务 | stage3/03 | 执行期间另开终端再打会"锁被占用" |
| `POST /lock/try → /release` | 抢锁/按 requestId 释放 | stage3/03 | try 返回 requestId，release 带上它；带错 ID 释放失败 |
| `POST /cache/reset` → 并发打 `GET /cache/product/1001` | **击穿防护** | stage3/01 | 50 并发后 `GET /cache/dbcalls` 应≈2~3 而非 50 |
| `POST /seckill/reset?stock=100` → 并发 `POST /seckill/{userId}` | **Lua 秒杀** | stage4/01 | 100 库存 300 并发：恰好 100 成功 0 超卖，同 userId 第二次被一人一单拦 |
| `POST /stream/send?orderNo=x` → `GET /stream/len` `GET /stream/pending` | Stream 消费者组 | stage4/03 | 控制台看 `[Stream消费]` 日志（后台 XREADGROUP+ACK） |

## 与 mq-mall 的呼应

- `/seckill` 就是 mq-mall 秒杀 Lua 的独立演示版；真实落地（MQ 异步落库+DB 兜底）看 [mq-mall seckill 包](../../mq/mq-mall/README.md)
- `/cache/product` 互斥锁重建 = stage3/01 击穿方案 1 的可运行实现
- `/lock` 手写版的三要点（NX EX / Lua 校验释放 / 续期）在生产中用 Redisson 的 `lock()/tryLock()` 一行替代

## 单元测试（无 Redis 也能跑）

```bash
mvn test    # 6 个用例，纯 Mockito 单测，不依赖任何容器
```

`SeckillLogicTest`（stage4/01）：用 Mock 模拟 Redis 键值/集合行为，把 **Lua 脚本的决策语义**固定下来——正常扣减、售罄拦截、未初始化兜底、库存 1 时恰好一人成功（不超卖）、一人一单拦截、边界 5 人抢 3 库存。真实原子性由 Redis 单线程保证（教学演示接口压测验证），单测钉住的是"返回码语义"不被改坏。

**测试技巧沉淀**：`Map<Long,String>.get(1001)` 与 `get(1001L)` 是两个不同的 key——`Long` 装箱必须显式 `L`，这是 [踩坑实录坑二](../../mq/踩坑实录-完整复盘.md)（幂等键撞号）的"箱内陷阱"近亲。

## 教学简化点

| demo 里 | 生产里 |
|---------|--------|
| 手写分布式锁 | Redisson（看门狗自动续期/可重入） |
| Stream 消费者手写线程 | StreamMessageListenerContainer / Redisson RStream |
| 秒杀订单内存 Map | 发 MQ → DB 落库（mq-mall 完整版） |
| 单机 Redis | 主从+哨兵 / Cluster（stage2/05） |
