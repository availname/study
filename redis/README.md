# Redis 完整教学

从零开始，按 4 个阶段循序渐进。**每个阶段学完都有动手任务**，做完再进入下一阶段。配套可运行工程在 [redis-demo/](redis-demo/README.md)。

## 学习路线

| 阶段 | 目录 | 学什么 | 前置要求 |
|------|------|--------|----------|
| 1. 入门使用 | [stage1-入门使用](stage1-入门使用/) | Redis 是什么为什么快、安装、五大类型+三种特殊类型、Key 管理、Spring Boot 整合 | JDK 17+、Docker |
| 2. 底层原理 | [stage2-底层原理](stage2-底层原理/) | 单线程与 IO 多路复用、数据结构底层、RDB/AOF 持久化、过期与内存淘汰、主从哨兵集群 | 学完 stage1 |
| 3. 问题解决 | [stage3-问题解决](stage3-问题解决/) | 缓存穿透/击穿/雪崩、缓存与 DB 一致性、分布式锁、大 Key/热 Key、**RedLock 争议与 Fencing Token** | 学完 stage2 |
| 4. 高并发实战 | [stage4-高并发实战](stage4-高并发实战/) | 秒杀库存（Lua）、排行榜/签到/UV、Redis Stream 队列、缓存设计模式 | 学完 stage3 |
| 5. 类型场景实战 | [stage5-类型场景实战](stage5-类型场景实战/) | 按数据类型逐一展开的 8 个真实业务：验证码防刷、购物车、Feed流、抽奖、热搜榜、签到、UV大屏、网约车 | 学完 stage4 |
| 🎓 毕业项目 | [../community](../community/README.md) | **Redis+MQ+MySQL 全联动**：社区论坛（审核管道/Feed推送/计数削峰/热榜/签到/UV），两边知识总集成 | 学完两套教程 |
| 🚀 新技术专题 | [../stage6-新技术专题](../stage6-新技术专题/README.md) | 虚拟线程 / Spring AI 审核升级 / Redis 向量检索与语义缓存 / OTel 全链路追踪 / GitHub Actions CI / Debezium CDC / Testcontainers | 两套教程+毕业项目后 |

## 与本工作区其它项目的关系

- [mq 教学目录](../mq/README.md) 的 **mq-mall 项目已经在用 Redis**（秒杀 Lua 预扣 + 幂等键），学完 stage3/4 再回看它的 `seckill/` 包会有"原来是这么回事"的感觉。
- Redis 教学环境用 **端口 6381**（`redis-demo/docker-compose.yml`），与 mq-mall 的 6380 独立，可同时运行互不干扰。

## 阶段任务（检验学习效果）

- [ ] **Stage 1**：redis-cli 里用五大类型各存一个"真实业务语义"的值（验证码 String、购物车 Hash、抽奖池 Set、排行榜 ZSet、消息 List）
- [ ] **Stage 1**：跑通 redis-demo 的 `/types` 全部接口，用 redis-cli 观察数据
- [ ] **Stage 2**：`BGSAVE` 一次，找到 dump.rdb；改一条数据，看 AOF 文件内容
- [ ] **Stage 2**：设置一个带 TTL 的 key，用 `TTL` 观察惰性删除和定期删除的效果
- [ ] **Stage 3**：跑通分布式锁接口，两个终端并发抢锁，验证互斥与自动释放
- [ ] **Stage 3**：故意删掉缓存，观察"缓存击穿"演示接口如何用互斥锁挡住并发重建
- [ ] **Stage 4**：Lua 秒杀压测，验证不超卖；ZSet 排行榜实时排名
- [ ] **Stage 4**：用 Redis Stream 的消费者组消费消息，体会它与 RabbitMQ 的异同

## 本教程的约定

- 版本：**Redis 7.x** · Spring Boot 3.2 · Java 17+
- 面试问法：每个重点章节末尾都有，给出答题骨架
- 一贯原则：**先会玩 → 再懂原理 → 会排坑 → 敢上高并发**，与 [mq 教学](../mq/README.md) 同款节奏
