# community：Redis + MQ 联动毕业项目

> 🎬 这个项目的可靠性架构（本地消息表→Confirm→补偿→死信）有**配套交互演示**：[diagrams/reliability.html](../diagrams/reliability.html)——四大故障场景单步动画。

**Spring Boot 3.2 + MySQL + Redis + RabbitMQ** 四件套全联动的社区论坛。它是 [mq 教学](../mq/README.md) 与 [redis 教学](../redis/README.md) 两个体系的**总毕业设计**——一个业务闭环里同时用到两边几乎全部知识点。

## 一图看懂三方分工

```
                    ┌──────────────── Redis（高频热数据，全部可丢可重建）────────────────┐
                    │  Token登录态(String+TTL)  防刷计数(INCR+Lua)  内容去重(SET NX)      │
用户请求 → [应用] → │  阅读计数(INCR削峰)  点赞去重(Set)  热帖榜(ZSet)  积分榜(ZSet)      │
                    │  Feed收件箱(List+LTRIM)  通知(List)  签到(Bitmap)  UV(HLL)         │
                    └───────────────────────────┬───────────────────────────────────┘
                                                │ 异步管道与广播（RabbitMQ）
                    ┌───────────────────────────▼───────────────────────────────────┐
                    │ community.post 交换机: post.submitted → AI审核消费者(BLOCK拒/HUMAN卡点) │
                    │ community.published.fanout: ─┬→ Feed推送消费者(写粉丝收件箱)        │
                    │                              └→ 通知消费者(写作者收件箱)            │
                    │ 本地消息表(事务消息) + Confirm补偿Job + 死信落库 —— 模式复用自 mq-mall │
                    └───────────────────────────┬───────────────────────────────────┘
                                                │ 异步刷底账（Write Behind）
                    ┌───────────────────────────▼───────────────────────────────────┐
                    │ MySQL（真源）：用户/帖子正文与状态/积分流水/发送记录/死信存档        │
                    └───────────────────────────────────────────────────────────────┘
```

**分工哲学一句话**：Redis 扛"高频写+随时可重建"，MQ 扛"异步管道+广播"，MySQL 只做"真源与底账"。

## 业务闭环（一条帖子的一生）

```
发帖 → ①每日限5篇(String INCR Lua) ②内容MD5去重(SET NX) → 落库(待审) + 本地消息表
  → MQ审核管道：BLOCK→拒绝+通知 ∥ HUMAN→人工卡点(管理端放行) ∥ 通过
      → 发帖奖励10积分（DB流水幂等 + ZSet积分榜）
      → fanout 广播 → Feed推送(写粉丝收件箱) + 作者通知
  → 浏览：阅读计数 INCR + 热榜 ZINCRBY + UV PFADD（三连写全在 Redis）
  → 点赞：Set 去重 + 热度+3
  → CounterFlushJob 每10秒把 Redis 计数刷进 DB 底账（DECR 增量法）
  → HeatDecayJob 每小时热榜 ×0.98 衰减
```

## 运行（复用已有一套基础设施，本项目零新容器）

```bash
# 前置：mq-mall 的 mysql(3307)/rabbitmq(5673) 与 redis-learn(6381) 在跑
docker exec mq-mall-mysql mysql -uroot -proot123 -e "CREATE DATABASE IF NOT EXISTS community DEFAULT CHARSET utf8mb4"
cd community && mvn spring-boot:run    # 应用 @ localhost:8082，建表自动执行
```

## API 玩法路线（按此顺序 curl）

| 步骤 | 请求 | 联动点 |
|------|------|--------|
| 1 | `POST /api/users/register&login` | 登录态=Redis String+TTL 7天 |
| 2 | `POST /api/social/follow/{id}?token=` | 关注关系=Set 双写 |
| 3 | `POST /api/posts?token=&title=&content=` | 限流(INCR Lua)+去重(SET NX)+事务消息进审核管道 |
| 3' | title 含 `BLOCK`/`HUMAN` 再发两帖 | MQ审核：自动拒绝 / 人工卡点 |
| 4 | 等 2 秒 → `GET /api/social/feed?token=` | fanout→Feed消费者 写收件箱，O(1) 直读 |
| 5 | `GET /api/posts/{id}` | Redis三连写：INCR阅读+ZINCRBY热度+PFADD UV |
| 6 | `POST /api/posts/{id}/like?token=` ×2 | Set 去重：第二次被拦 |
| 7 | `GET /api/posts/hot` | ZSet 热榜（阅读1/点赞3/发帖5 加权） |
| 8 | `POST /api/sign?token=` ×2 | Bitmap 幂等签到+积分（DB 唯一键防重发） |
| 9 | `POST /api/admin/audit/{id}?pass=true` | 人工卡点放行（HUMAN_AUDIT→APPROVED） |
| 10 | 等 10 秒 → `GET /api/admin/stats` | 三方大盘：状态分布/本地消息表/热榜/UV |
| 11 | `GET /api/social/rank/points` · `GET /api/social/notifications` | 积分榜(ZSet) / 通知收件箱(List) |

curl 中文参数一律 `--data-urlencode`（否则 Tomcat 400）。

## 知识点映射（学过的每章都在这找到归宿）

| 本项目功能 | Redis 知识点 | MQ 知识点 | MySQL 知识点 |
|-----------|-------------|-----------|-------------|
| 登录态 | String+TTL（r-stage5/01） | — | — |
| 发帖防刷 | INCR+首次TTL 的 Lua（r-stage5/01） | — | — |
| 发帖防重 | SET NX EX 指纹（r-stage1/03） | — | — |
| 发帖→审核 | — | 管道模式+状态机CAS+人工卡点（m-stage5/02） | t_post 状态列 |
| 发帖可靠投递 | — | 本地消息表+Confirm+补偿（m-stage3/01） | t_mq_send_record |
| 审核通过奖励 | ZSet 积分榜 | 消费幂等（m-stage3/02） | t_points_log 唯一键 |
| Feed 推送 | List LPUSH+LTRIM 防大Key（r-stage5/03） | fanout 广播（m-stage1/04） | — |
| 阅读计数 | INCR 削峰 + Write Behind DECR 增量法（r-stage4/04） | — | view_count 底账 |
| 点赞 | Set SADD 去重 + SCARD（r-stage1/03） | — | like_count 底账 |
| 热帖榜 | ZSet 热度+衰减 job（r-stage5/05） | — | — |
| 签到 | Bitmap SETBIT 幂等 + 连签位运算（r-stage5/06） | — | 积分流水幂等 |
| UV | HLL PFADD/PFCOUNT（r-stage5/07） | — | — |
| 死信兜底 | — | DLX + 死信落库（m-stage3/05） | t_mq_dead_letter |

（r- = redis 教学章节，m- = mq 教学章节）

## 本项目开发中真实踩的坑（补录）

1. **SETBIT 返回值语义反了**：它返回"设置前的旧值"（0=首次），判断写反导致首次签到显示"已签过"——SET 系列命令的返回值必须查文档确认，不能想当然。
2. **状态机 CAS 的来源状态写死**：approve 只认"待审(0)"，人工卡点的帖子是状态 3，放行时 CAS 不中静默卡死——**同一目标状态可以有多个来源状态**，CAS 的期望值要与调用方匹配（参数化 fromStatus）。
3. **ValueOperations 没有 bitCount/bitField**（不同版本 API 差异）——改用 GET 原始位串 + 应用层解位，零版本依赖。

## 测试（单元测试 + Testcontainers 集成测试）

`mvn test` 即可：

**① 纯 Mockito 单元测试（11 个，任何环境都跑）**

| 测试类 | 固定住的行为 | 对应教学 |
|--------|------------|---------|
| `MessageDedupServiceTest` | 两层幂等：Redis 拦截不碰 DB / DB 冲突回滚 Redis 标记（坑九）/ fail() 清两层 | mq stage3/02 |
| `HeatDecayJobTest` | 热度 ×0.98；<0.5 出榜（remove 非 add）；空榜/空 score 不动作 | redis stage5/05 |
| `CounterFlushJobTest` | Write Behind：DECR 已落库增量（**不删 key**）/ 帖子不存在不刷库 / 缺值跳过 | redis stage4/04 |

**② Testcontainers 集成测试（3 个，需 Docker）**
- **本机有 Docker** → 自动起临时 MySQL/Redis 容器跑 3 个回归用例（SETBIT 旧值语义/本地消息表生命周期/幂等两层防线——锁住踩坑实录的三个坑）
- **本机没 Docker** → 用例自动 Skipped（`disabledWithoutDocker = true`），不会变红
- CI（GitHub Actions 的 ubuntu Runner 自带 Docker）→ 每次 push 真跑

## 教学简化点（生产要做）

| 简化 | 生产 |
|------|------|
| token 直接当参数 | Authorization 头 + 拦截器/网关鉴权（**升级地图：[java-core/19-登录态与接口安全](../java-core/19-登录态与接口安全.md)**——对比 learn-center 的拦截器实现） |
| 关注关系只存 Redis | DB 主存 + Redis 加速 + 对账 |
| 阅读不去重 | 按"用户+帖子"Bitmap/Set 去重 |
| Feed 全推模式 | 推拉结合（大V拉，stage5/03） |
| 密码 MD5 | BCrypt（19 篇·第四节） |
| 单实例 | 多实例时登录态/锁/计数天然跨实例（Redis 的价值所在） |
