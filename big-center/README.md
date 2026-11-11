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

> TODO：收尾阶段补 Mermaid 架构图

## 功能 ↔ 教学章节 对号入座

| 功能 | 教学出处 | 状态 |
|------|---------|------|
| 分布式 ID（雪花，时钟回拨处理） | [java-core/11-分布式ID](../java-core/) | ✅ `common/Ids` |
| 接口规范（ApiResponse/错误码分段） | [java-core/16-接口设计](../java-core/) | ✅ `common/` |
| 日志规范（MDC traceId/六要素） | [java-core/17-日志规范](../java-core/) | ✅ `config/TraceIdFilter` |
| JWT 登录（双 token/黑名单） | [java-core/19-登录态与接口安全](../java-core/19-登录态与接口安全.md) | ✅ `auth/`（JwtService 双 token + BCrypt + 黑名单 + refresh 旋转；`JwtAuthInterceptor` 与 learn-center Redis token 拦截器同构，差异只在"验签解析"替代"Redis 查 userId"） |
| 短链接服务（发号/302/布隆） | [java-core/12-系统设计三题](../java-core/12-系统设计三题.md) 题一 | ✅ `shortlink/`（雪花→Base62 发号无碰撞；302 可统计；布隆→Redis→库三层解析；INCR+阈值回写 Write Behind） |
| 支付中心（状态机 CAS/回调幂等/对账） | [java-core/15-系统设计·支付系统](../java-core/15-系统设计·支付系统.md) | ✅ `pay/`（四关卡：CAS 状态机+输方分支 / 流水唯一索引+CAS 双层幂等 / 掉单查单补偿 / 金额分+比对+验签；T+1 三单对账差错池；设计模式：门面+策略 `PaymentFacade`/`PaymentChannel`） |
| 抢红包（二倍均值/TTL+DLX） | [java-core/14-系统设计进阶三题](../java-core/14-系统设计进阶三题.md) 题二 | ✅ `redpack/`（二倍均值预拆+LPUSH 重活放写少侧；SADD 防重+LPOP 原子认领不超发不重复、没抢到退回名单；明细 MQ 削峰落库；24h TTL+CAS 状态机过期退款、CAS 输方幂等） |
| 点赞系统（Lua 原子/MQ 刷库） | [java-core/14-系统设计进阶三题](../java-core/14-系统设计进阶三题.md) 题一 | ✅ `like/`（Set 判重+INCR 计数 Lua 原子化 `lua/like.lua`、`unlike.lua`；MySQL 只异步归档） |
| 消息推送（SSE+离线落库） | java-core/14 题三 | ✅ `push/`（三通道：SSE 在线直推→落库→拉取兜底；Redis 在线路由；未读数=最新 ID−已读位点 O(1)） |
| 扫码登录（uuid 状态机） | [java-core/12-系统设计三题](../java-core/12-系统设计三题.md) 题二 | ✅ `auth/qrcode/`（uuid 状态机 WAITING→SCANNED→CONFIRMED 条件推进（setIfPresent CAS）、TTL 5min 防重放、确认人=扫码人防越权、token 一次性签发即清理、PC 轮询接口） |
| 限流器（Lua 滑动窗口+AOP） | [java-core/12-系统设计三题](../java-core/12-系统设计三题.md) 题三 | ✅ `rate/`（ZSET 时间戳滑动窗口 Lua 原子 `lua/rate_limit.lua`、@RateLimit 注解 AOP 细限到人、超限 5001 快速失败；已打到登录（防撞库 5/min）/抢红包（30/min）/下单（10/min）） |
| 设计模式实战（责任链/策略/状态机/门面） | [java-core/06-设计模式](../java-core/06-设计模式/) 五篇 | ✅ 落在业务里：策略+门面 `pay/PaymentFacade`、状态机 `pay/PayOrderMapper.cas*`/`redpack/casStatus`、责任链 `PaymentService.handleCallback` 验签→金额→幂等链；可运行示例 `lab/patterns/`（单例五种/JDK 代理/策略+模板+责任链/状态机表驱动） |
| java-core-lab 实验室 | [java-core/00](../java-core/00-Java语言基石/)、[01-并发](../java-core/01-并发编程/)、[02-JVM](../java-core/02-JVM虚拟机/)、[06-设计模式](../java-core/06-设计模式/) | ✅ `lab/`（basics：HashMap 扩容反射/泛型擦除+注解扫描/Stream 分组/String 相等性；concurrent：线程六状态/锁升级/CAS+ABA 版本号/volatile+DCL/线程池七参数/AQS 自实现锁/CHM+ThreadLocal 泄漏/虚拟线程衔接；jvm：内存区+Young GC+可恢复 OOM+GC 参数模板；patterns 同上）——每个类带 `// 教程:` 锚点注释，单独运行 main |

## 包结构

```
com.bigcenter
├── common      ApiResponse/ErrorCode/Ids/全局异常（规范地基，16/17 篇吃狗粮）
├── config      WebConfig/TraceIdFilter（第 2 步起接入 JWT 拦截器）
├── auth        JWT 认证：JwtService（access 15min 验签 + refresh 7d Redis 撤销 + 黑名单）/AuthService（BCrypt）/JwtAuthInterceptor（三件套：校验-ThreadLocal-清理）/UserContext
├── shortlink   短链（第 3 步）
├── pay         支付中心（第 4 步）
├── redpack     红包（第 5 步）
├── like/push   点赞+推送（第 6 步）
├── rate        限流器（第 8 步）
└── lab         java-core-lab（第 9 步）
```

## 运行

```bash
# 依赖：mq-mall-mysql(3307)/mq-mall-redis(6380)/mq-mall-rabbitmq(5673) 已有 compose
docker exec mq-mall-mysql mysql -uroot -proot123 -e "CREATE DATABASE IF NOT EXISTS big_center DEFAULT CHARSET utf8mb4"
mvn spring-boot:run
```

## 测试

```bash
mvn test    # 纯 Mockito 单测，无 Docker 全绿（学习体系铁律）
```
