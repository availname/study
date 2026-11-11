# big-center —— 开发者成长平台

学习体系的**总装厂**：把 java-core 21 篇 + mq/redis 四阶段 + 4 个旧项目的知识点，总装进一个真实业务（任务书见 [`../big-center-PLAN.md`](../big-center-PLAN.md)）。

```
big-center (8085)
├── 门户/短链服务      短链接系统（12 篇）
├── 用户服务          JWT 双 token 登录（19 篇）+ 扫码登录（12 篇）
├── 学习/社区业务      点赞（14-1）/ 消息推送（14-3）
├── 活动运营          限时红包雨（14-2）
├── 支付中心          模拟支付网关（15 篇）
├── 基础设施          限流器（12-3）/ 分布式 ID（11）/ 接口规范（16）/ 日志规范（17）
└── java-core-lab     并发/基石/设计模式可运行 main 示例集
```

## 架构

```mermaid
flowchart LR
    Client[客户端 PC/App] -->|HTTP /api/**| AuthN[JwtAuthInterceptor<br/>19 篇: 验签+黑名单]
    Client -->|/s/{code}| SL[shortlink 短链<br/>12 题一: 布隆→Redis→库, 302+统计]
    Client -->|轮询| QR[auth/qrcode 扫码登录<br/>12 题二: uuid 状态机]

    AuthN --> C[Controllers]
    C --> Rate[rate @RateLimit AOP<br/>12 题三: Lua 滑动窗口]
    Rate --> Svc[Services]

    Svc --> PAY[pay 支付中心<br/>15 篇: CAS 状态机/回调幂等/对账<br/>策略+门面: PaymentFacade]
    Svc --> RP[redpack 红包<br/>14-2: 二倍均值预拆/LPOP 认领]
    Svc --> LK[like 点赞<br/>14-1: Lua 判重+计数原子]
    Svc --> PS[push 推送<br/>14-3: SSE/落库/拉取三通道]
    Svc --> SK[seckill 秒杀<br/>四层漏斗: Lua 预扣/MQ 建单/DB 兜底]
    Svc --> PT[post 帖子<br/>审核管道 AI 三分支/热榜衰减]
    Svc --> AI[ai 学习报告<br/>Mock/Real 双实现]

    PAY & RP & LK & PS & SK & PT --> Redis[(Redis 6380<br/>bc: 前缀)]
    PAY & RP & LK & PS & SL --> MySQL[(MySQL 3307<br/>big_center 库)]
    LK & RP & SK & PT -.削峰.-> MQ[(RabbitMQ 5673)]

    subgraph 基础设施
        Ids[common/Ids 雪花<br/>11 篇: 时钟回拨拒绝]
        Api[common/ApiResponse+ErrorCode<br/>16 篇: 错误码分段]
        Trace[TraceIdFilter<br/>17 篇: MDC traceId]
    end
    Svc --> Ids
    C & Svc --> Api
    Svc --> Trace
```

## 功能 ↔ 教学章节 对号入座

| 功能 | 教学出处 | 状态与落地 |
|------|---------|------|
| 分布式 ID（雪花，时钟回拨处理） | [java-core/11-分布式系统基础](../java-core/11-分布式系统基础.md) | ✅ `common/Ids`（41 时间戳+10 机器+12 序列；回拨≤5ms 等待、更大拒绝——宁可失败不可撞号） |
| 接口规范（ApiResponse/错误码分段） | [java-core/16-接口设计规范](../java-core/16-接口设计规范.md) | ✅ `common/ApiResponse`+`ErrorCode`（0 成功；1xxx 用户/2xxx 短链/3xxx 红包/4xxx 支付/5xxx 系统） |
| 日志规范（MDC traceId/六要素） | [java-core/17-日志规范](../java-core/17-日志规范.md) | ✅ `config/TraceIdFilter`（上游 X-Trace-Id 透传、finally 清 MDC、pattern 内嵌 traceId） |
| JWT 登录（双 token/黑名单） | [java-core/19-登录态与接口安全](../java-core/19-登录态与接口安全.md) | ✅ `auth/`（JwtService 双 token + BCrypt + 黑名单 + refresh 旋转；`JwtAuthInterceptor` 与 learn-center Redis token 拦截器同构，差异只在"验签解析"替代"Redis 查 userId"） |
| 短链接服务（发号/302/布隆） | [java-core/12-系统设计三题](../java-core/12-系统设计三题.md) 题一 | ✅ `shortlink/`（雪花→Base62 发号无碰撞；302 可统计；布隆→Redis→库三层解析；INCR+阈值回写 Write Behind） |
| 支付中心（状态机 CAS/回调幂等/对账） | [java-core/15-系统设计·支付系统](../java-core/15-系统设计·支付系统.md) | ✅ `pay/`（四关卡：CAS 状态机+输方分支 / 流水唯一索引+CAS 双层幂等 / 掉单查单补偿 / 金额分+比对+验签；T+1 三单对账差错池；设计模式：门面+策略 `PaymentFacade`/`PaymentChannel`） |
| 抢红包（二倍均值/TTL+DLX） | [java-core/14-系统设计进阶三题](../java-core/14-系统设计进阶三题.md) 题二 | ✅ `redpack/`（二倍均值预拆+LPUSH 重活放写少侧；SADD 防重+LPOP 原子认领不超发不重复、没抢到退回名单；明细 MQ 削峰落库；24h TTL+CAS 状态机过期退款、CAS 输方幂等） |
| 点赞系统（Lua 原子/MQ 刷库） | java-core/14 题一 | ✅ `like/`（Set 判重+INCR 计数 Lua 原子化 `lua/like.lua`、`unlike.lua`；MySQL 只异步归档） |
| 消息推送（SSE+离线落库） | java-core/14 题三 | ✅ `push/`（三通道：SSE 在线直推→落库→拉取兜底；Redis 在线路由；未读数=最新 ID−已读位点 O(1)） |
| 扫码登录（uuid 状态机） | java-core/12 题二 | ✅ `auth/qrcode/`（uuid 状态机 WAITING→SCANNED→CONFIRMED 条件推进（setIfPresent CAS）、TTL 5min 防重放、确认人=扫码人防越权、token 一次性签发即清理、PC 轮询接口） |
| 限流器（Lua 滑动窗口+AOP） | java-core/12 题三 | ✅ `rate/`（ZSET 时间戳滑动窗口 Lua 原子 `lua/rate_limit.lua`、@RateLimit 注解 AOP 细限到人、超限 5001 快速失败；已打到登录（防撞库 5/min）/抢红包（30/min）/下单（10/min）） |
| 秒杀四层漏斗（限量课抢购） | [learn-center/](../learn-center/) EnrollController+lua（mq-mall 秒杀篇依据） | ✅ `seckill/`（Lua 原子预扣 `lua/seckill.lua`（判重+扣名额+登记三步原子）→ MQ 异步建单削峰（`SeckillConsumer`）→ DB 唯一索引幂等 + `WHERE enrolled<quota` 防超卖兜底；Redis/DB 两道防线 fencing token 思想） |
| 帖子审核管道（AI 三分支） | [community/](../community/) AuditService（MQ 管道模式） | ✅ `post/`（发帖落库待审 + MQ 进管道；AI 审核：BLOCK 拒绝/HUMAN 人工卡点/其余过审；CAS 状态机推进（期望状态必须匹配来源）、积分唯一索引幂等 `t_points_log.message_id`） |
| 热榜时间衰减 | [community/](../community/) HeatDecayJob（stage5/05 热搜同款） | ✅ `post/HeatDecayJob`（ZSet 热度榜每小时 ×0.98，低于 0.5 出榜——旧帖自然沉底不删除） |
| AI 学习报告（Mock/Real 双实现） | [learn-center/](../learn-center/) ai/ 包（stage7/04 防幻觉分工） | ✅ `ai/`（ChatPort 策略接口 + `@ConditionalOnProperty` 切换：数字由系统聚合（秒杀/帖子/积分/点赞真实 SQL），措辞由模型生成——事实系统给，表述 AI 编） |
| 越权防护（where user_id） | java-core/19 第五节 | ✅ `pay/PaymentService.findByNo`（数据权限进查询条件，天然不越权）；推送 send 的 from 取登录态 |
| 设计模式实战（责任链/策略/状态机/门面） | [java-core/06-设计模式](../java-core/06-设计模式/) 五篇 | ✅ 落在业务里：策略+门面 `pay/PaymentFacade`、状态机 `pay/casPaid` 与 `redpack/casStatus`、责任链 `PaymentService.handleCallback` 验签→金额→幂等链；可运行示例 `lab/patterns/` |
| java-core-lab 实验室 | [00 基石](../java-core/00-Java语言基石/) / [01 并发](../java-core/01-并发编程/) / [02 JVM](../java-core/02-JVM虚拟机/) / [06 设计模式](../java-core/06-设计模式/) | ✅ `lab/`（basics：HashMap 扩容反射/泛型擦除+注解扫描/Stream 分组/String 相等性；concurrent：线程六状态/锁升级/CAS+ABA 版本号/volatile+DCL/线程池七参数/AQS 自实现锁/CHM+ThreadLocal 泄漏/虚拟线程衔接；jvm：内存区/Young GC/可恢复 OOM/GC 参数模板） |

## 包结构

```
com.bigcenter
├── common      ApiResponse/ErrorCode(分段)/BizException/Ids(雪花)/GlobalExceptionHandler
├── config      WebConfig(JWT 拦截注册)/TraceIdFilter(MDC)
├── auth        JwtService/AuthService/JwtAuthInterceptor/UserContext + qrcode/(扫码登录)
├── shortlink   Base62/BloomFilter/ShortLinkService+Controller（三层解析+Write Behind）
├── pay         PaymentChannel(策略)/PaymentFacade(门面)/PaymentService(四关卡)/ReconcileService(对账)
├── redpack     RedPackSplitter(二倍均值)/RedPackService(SADD+LPOP)
├── like        LikeService(lua/like.lua、unlike.lua)
├── push        PushService(SSE 三通道+位点)
├── rate        RateLimit(注解)/RateLimiterService(lua/rate_limit.lua)/RateLimitAspect(AOP)
├── seckill     SeckillController(lua/seckill.lua 四层漏斗)/SeckillConsumer(MQ 建单削峰)
├── post        PostService(审核管道)/AuditConsumer(AI 三分支+CAS)/HeatDecayJob(热榜衰减)
├── ai          ChatPort(策略)/MockChatPort/RealChatPort/AiReportService(数据聚合+AI 表述)
└── lab         java-core-lab（basics/concurrent/jvm/patterns，教程锚点注释）
```

## API 路线

| 模块 | 接口 | 说明 |
|------|------|------|
| 认证 | `POST /api/auth/register` `/login` `/refresh` `/logout`，`GET /api/auth/me` | register/login/refresh 免登录 |
| 短链 | `POST /api/shortlink`，`GET /api/shortlink/{code}/stats`，`GET /s/{code}` | /s 跳转免登录 302 |
| 扫码 | `POST /api/qrcode/create`（免登录），`/{uuid}/scan` `/{uuid}/confirm`（App 登录态），`GET /{uuid}/status`（免登录轮询） | 状态机四接口 |
| 支付 | `POST /api/pay/order` `/callback`（渠道回调免登录），`POST /{no}/compensate`，`GET /{no}` | 金额单位分 |
| 红包 | `POST /api/redpack`，`POST /{packId}/grab`，`GET /{packId}/details` | grab 带 @RateLimit |
| 推送 | `GET /api/push/connect`(SSE)，`POST /send` `/read`，`GET /pull` `/unread` | 位点差值算未读 |
| 秒杀 | `POST /api/seckill/{courseId}`，`GET /api/seckill/result/{courseId}` | Lua 预扣+MQ 建单+轮询 |
| 帖子 | `POST /api/post`，`GET /api/post/{id}`，`GET /api/post/hot?n=10` | 发帖进审核管道；热榜 ZSet |
| AI | `GET /api/ai/report` | 四项数据聚合 → AI 表述 |

## 运行

```bash
# 依赖：mq-mall-mysql(3307)/mq-mall-redis(6380)/mq-mall-rabbitmq(5673) 已有 compose
docker exec mq-mall-mysql mysql -uroot -proot123 -e "CREATE DATABASE IF NOT EXISTS big_center DEFAULT CHARSET utf8mb4"
mvn spring-boot:run
```

## 测试

```bash
mvn test    # 79 例纯 Mockito 单测，无 Docker 全绿（学习体系铁律）
```

重点钉住的教学语义：Lua 判重计数原子（like）、CAS 输方分支（pay 竞态三分支）、双层幂等（回调唯一索引）、LPOP 不超发+名单退回（redpack）、状态机条件推进（qrcode setIfPresent）、Write Behind 阈值回写（shortlink）、秒杀预扣成功才发 MQ（seckill）、审核三分支+CAS 输方跳过+积分幂等（post/AuditConsumer）、热榜衰减出榜阈值（HeatDecayJob）、AI 报告数据进 prompt（ai/AiReportService）。
