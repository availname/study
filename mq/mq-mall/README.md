# mq-mall：MQ 实战项目（订单消息系统）

教程（[../README.md](../README.md)）四阶段知识点的**完整落地工程**：一个"下单 → 支付 → 异步发货链路 + 秒杀 + 超时关单"的消息驱动系统。

> 本项目是教程的"毕业设计"：每一段代码都标注了对应的教程章节。建议先跑起来玩一遍，再对照教程读代码。

## 技术栈

Spring Boot 3.2 · RabbitMQ · MySQL 8 · Redis 7 · Docker Compose

## 一键启动

```bash
cd mq/mq-mall
docker compose up -d --wait     # 启动 MySQL(3307) + Redis(6380) + RabbitMQ(5673)
mvn spring-boot:run             # 启动应用(8090)，建表自动执行
```

- 应用：http://localhost:8090
- RabbitMQ 管理界面：http://localhost:15673 （guest/guest）
- 端口特意错开（3307/6380/5673），不与教程 demo 的环境（5672）冲突，两套可同时运行。

## 架构总览

```
                     ┌─────────────────────── 本地事务（原子）───────────────────────┐
POST /orders ──→ 锁库存 t_stock ─→ 建订单 t_order(10) ─→ 流水 ─→ t_mq_send_record   │
                     └─────────────────── 提交后 afterCommit ──────────────────────┘
                                            │
                        ┌───────────────────┼──────────────────────────┐
                        ▼                   ▼                          ▼
               mall.order.created   mall.order.paid(支付后)      order.delay.queue(TTL 2min)
                        │                   │                          │ TTL到期死信
                        ▼                   ▼                          ▼
              审计/缓存重建消费者      ┌─ 积分(唯一索引幂等)      mall.order.timeout.queue
                                    ├─ 发券(业务唯一键幂等)          │ CAS 10→50
                                    └─ 通知(日志模拟)                │ 释放库存
                                                                    ▼
                                                        扫表兜底 OrderSweepJob（双保险）

秒杀：POST /seckill → Redis Lua 原子预扣(判断+扣减+一人一单) → MQ → 4并发匀速落库 → DB WHERE stock>=1 兜底

可靠性基建：Confirm/Return 回调 → 更新本地消息表；SendRetryJob 补偿重发；
           消费重试3次 → dlx.exchange → mall.dlq.queue → 死信落库 t_mq_dead_letter；
           消费幂等两层：Redis setnx + t_mq_consume_record 唯一键
```

## API 清单（按此顺序玩）

| 步骤 | 请求 | 验证什么 |
|------|------|---------|
| 1 | `POST /api/orders?userId=1&skuId=1&count=1` | 下单：库存锁定 + 两条事务消息 + 延迟消息发出 |
| 2 | `GET /api/orders/{orderNo}` | 订单"待支付"；RabbitMQ 管理界面看到 created 消息被消费 |
| 3 | `POST /api/orders/{orderNo}/pay` | 支付：CAS 10→20，积分/发券/通知三个下游异步完成 |
| 3' | 再调一次同样的 pay | 支付幂等：日志显示"忽略重复支付" |
| 4 | 不支付，等 2 分钟 | 延迟消息到点关单 + 库存释放；也可观察扫表兜底（20s 一轮） |
| 5 | `POST /api/seckill/1?users=300` | 秒杀：接入层瞬间完成，300 人抢购（一人一单拦截重复） |
| 6 | 轮询 `GET /api/seckill/1/stats` | 看"排队中"逐步清零 —— 消费端匀速落库 = 削峰 |
| 7 | `GET /api/admin/reconcile` | 对账：Redis 预扣数 == DB 订单数 == 库存扣减数，"一致": true |
| 8 | `POST /api/admin/test-dlq` | 可靠链路演练：故意失败 → 重试 3 次 → 死信 |
| 9 | `GET /api/admin/dead-letters` | 死信落库存档（失败可见可控） |
| 10 | `GET /api/admin/stats` | 全局健康度：订单分布/发送记录状态/死信数 |

curl 示例：

```bash
curl -X POST "http://localhost:8090/api/orders?userId=1&skuId=1&count=1"
curl -X POST "http://localhost:8090/api/orders/O20260101120000xxxx/pay"
curl -X POST "http://localhost:8090/api/seckill/1?users=300"
curl "http://localhost:8090/api/admin/reconcile"
```

## 第二场景域：视频平台内容处理流水线（stage5）

电商之外，本项目第二个场景域演示 MQ 的另一种用法——**任务管道 + 结果广播**（[stage5 教程](../stage5-真实场景-视频平台/01-场景与架构总览.md)）：

```
POST /api/videos（秒回 videoId，本地消息表受理）
  → media.submitted.queue → 转码(1.5s 模拟 FFmpeg)
  → media.transcoded.queue → 审核（BLOCK 拒绝 / HUMAN 人工卡点 / 通过）
  → media.audited.queue → 上架(CDN 预热)
  → media.published.fanout ─┬→ media.recommend.queue（推荐索引）
                             └→ media.notify.queue（粉丝推送）
```

| 接口 | 干什么 | 玩法 |
|------|--------|------|
| `POST /api/videos?title=x` | 提交视频（`--data-urlencode` 传中文！） | 标题含 **FAIL** 转码必失败、**BLOCK** 审核拒绝、**HUMAN** 人工卡点 |
| `GET /api/videos/{id}` | 轮询状态机（"转码中…"的数据源） | UPLOADED→TRANSCODING→AUDITING→PUBLISHED |
| `POST /api/videos/{id}/retry` | 失败任务人工重提（清幂等键+重发） | 观察状态机+幂等两道防线的交互（踩坑实录坑九） |
| `POST /api/videos/{id}/audit?pass=` | 人工审核放行/拒绝 | 卡在 HUMAN_AUDIT 的视频由这里推进 |
| `GET /api/videos/stats` | 各状态分布（管道心电图） | 哪个状态堆积=哪级环节有瓶颈 |

代码：`media/` 包（`MediaPipelineConsumers` 一条管道五个消费者）+ `config/MediaTopologyConfig`。

## 目录结构与教程对照

| 代码 | 干什么 | 教程 |
|------|--------|------|
| `config/RabbitTopologyConfig` | 拓扑声明：Topic 交换机 / 死信 / TTL 延迟链 / 惰性队列（含 Quorum 注释） | stage1/04 · stage2/02 · stage3/05 |
| `config/RabbitInfraConfig` | Jackson 转换器、Confirm/Return 回调接本地消息表、秒杀专用消费容器 | stage2/03 |
| `messaging/ReliableMessagePublisher` | **发送门面**：事务内写消息表 + afterCommit 发送 + 重发 API | stage3/01 |
| `messaging/SendRecordDao` + `compensation/SendRetryJob` | 本地消息表 + 补偿重发（指数退避→死信） | stage3/01 |
| `messaging/MessageDedupService` | 消费幂等两层：Redis setnx + DB 唯一键，失败回滚键 | stage3/02 |
| `messaging/DeadLetterConsumer` | 死信统一落库 + 告警日志 | stage3/05 |
| `order/OrderService` | 下单/支付/关单：锁库存 + CAS 状态机 + 事务消息 | stage4/02 |
| `orderconsumer/*` | created/paid/timeout 三个消费者：三种幂等形态示范 | stage3/02 |
| `seckill/*` | Lua 预扣 + 匀速消费 + DB 兜底 + 失败归还 + 对账 | stage4/01 |
| `compensation/OrderSweepJob` | 超时订单扫表兜底（与延迟消息双保险） | stage4/02 |
| `admin/AdminController` | 死信/消息状态/对账/链路演练端点 | stage3/04 · stage4/03 |

## 项目里能观察到的"教科书瞬间"

1. **本地消息表生效**：下单后立刻 `docker stop mq-mall-rabbitmq` → RabbitMQ 起回来后，SendRetryJob 补偿重发，消息一条不丢（管理界面/日志可见 `[补偿] 重发`）。
2. **延迟关单 + 扫表双保险**：下单后不支付，2 分钟后订单自动变 50、库存涨回来；注释掉延迟队列再试，扫表仍会兜底关单。
3. **支付 vs 关单竞态**：在超时前最后一刻调 pay，要么支付成功（关单 CAS 落空），要么提示"已超时关闭"——绝不出现"付了钱订单被关"。
4. **削峰曲线**：秒杀 stats 里"排队中"从 300 逐渐清零，DB 始终按 ~1300/s 消化（把 `DB_COST_MS` 调大到 50ms 更明显）。
5. **幂等两层**：手动往 `mall.order.paid.queue` 重发同一条消息（管理界面 Duplicate），日志显示跳过；再把 Redis 清空重试，DB 唯一索引仍拦截。

## 踩坑实录（冒烟测试真实踩到的，比教程更教训）

这三个坑都是本项目第一次跑通时真实发生的，代码里已修复并留了注释。**每个都是生产事故的经典形态**：

### 坑一：消息"双重序列化"，消费者反序列化失败
`OrderService` 先 `toJson(event)` 把对象变成字符串再交给 publisher，publisher 内部又序列化一次——消费者收到的是"JSON 字符串包着 JSON"，反序列化炸成 `MismatchedInputException`。
**教训**：消息序列化必须只发生一次、且收敛在一个地方（本项目收敛在 `ReliableMessagePublisher`）。业务代码只传对象。
修复记录：`OrderService` 直接传事件对象。

### 坑二：内存发号器重启归零，新订单被自己的幂等层"误杀"
秒杀订单号用 `AtomicLong(1_000_000)` 内存自增——应用重启后从 1,000,001 重新发号，而上一实例写入 Redis 的幂等键 `mq:dedup:seckill-1000036`（TTL 24h）还在 → `tryConsume` 判定"重复消息"→ **静默跳过**。
现象极其隐蔽：消息被正常消费、无死信、无报错、无日志，但数据库就是没有订单。
**教训**：①幂等键必须基于**全局唯一**的业务 ID（雪花/Leaf），内存计数器重启即撞号；②幂等跳过路径也应打 INFO 日志，静默 return 会让排查变成噩梦。
修复记录：`SeckillService` 改用 `Ids.nextId()`。

### 坑三：扫表兜底把"抢到未付款"的秒杀单当普通超时单关掉
超时扫表 `WHERE status=10` 会命中秒杀单（演示环境秒杀单无支付环节，永远 status=10），关单释放 DB 库存但 Redis 不归还 → Redis/DB 漂移。
**教训**：兜底逻辑的筛选条件必须和业务语义对齐（排除 `seckill_flag IS NULL`）；"关单释放库存"在双库存体系（Redis+DB）里必须两边同步归还。
修复记录：`OrderDao.findTimeoutPending` 排除秒杀单，README"扩展练习"里留了正确的秒杀超时释放方案。

## 单元测试（无 Docker 也能跑）

`mvn test` 即可，纯 Mockito，不碰 MySQL/RabbitMQ/Redis：

| 测试类 | 固定住的行为 | 对应教学 |
|--------|--------|------|
| `OrderServiceTest`（9 用例） | **CAS 状态机资损防线**：支付成功记 DEDUCT 流水 + 发 order.paid；重复支付幂等返回、绝不二次扣库存；已关单支付抛异常、绝不发货；关单 CAS 失败返回 false 且**绝不动库存**（防超卖）；下单两条消息（业务事件 + 延迟关单）缺一不可 | stage4/02 |
| `MessageDedupServiceTest`（4 用例） | 幂等两层：Redis setnx 第一道拦截不碰 DB；DB 唯一键冲突回滚 Redis 标记；`fail()` 清两层允许重投重消费 | stage3/02 |

> 单测锁的是"输家不许碰库存/消息"这类**业务不变量**，而不是 mock 的调用次数——竞态赢家干什么都行，输家多走一步就是事故。

## 教学简化点（生产要换掉的）

| 项目里 | 生产里 |
|--------|--------|
| 经典队列（注释了 Quorum 写法） | 关键队列 Quorum Queue 多副本 |
| TTL+DLX 延迟（固定 2 分钟） | 延迟插件/RocketMQ 任意延迟 |
| 雪花简化版 Ids | Leaf/雪花 + 时钟回拨处理 |
| 秒杀结果存内存 Map | 存 Redis/DB，重启可恢复 |
| 通知 = 日志 | 短信/推送网关 + 失败重试 |
| 单机部署 | 消费者/生产者拆独立服务 + 注册中心 |

## 扩展练习（学完教程后做）

1. 把 `OrderPaidConsumer` 拆成独立 `coupon-service` 工程（跨进程验证解耦）
2. 给秒杀链路加 RocketMQ 事务消息替代 Redis 预扣 + 延迟关单（对照两种方案）
3. 用 JMeter 压 `POST /api/orders`，观察 Hikari 连接数与消费速率的关系
4. 实现"死信自动重放"管理端点：把 t_mq_dead_letter 的消息重新投递
5. 接入 Prometheus + Grafana，给 Ready 堆积/死信数配告警（stage3/04）
