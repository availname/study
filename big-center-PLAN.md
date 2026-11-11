# big-center 项目任务书（新会话从这里开始执行）

> **目标**：把教学体系里所有知识点**融合成一个完整的大项目**——`big-center`，放在 `D:\JavaSpace\study\big-center`。这是整个学习体系的"总装厂"：已有 4 个项目各覆盖一域，本项目把散落的知识点全部收拢进一个真实业务。

## 一、背景：学习体系全景（已在 study/ 仓库内）

| 目录 | 内容 | 与本项目关系 |
|------|------|-------------|
| `java-core/` | 21 篇教学（基石/并发/JVM/MySQL/Spring/微服务/设计模式/网络/测试/系统设计 7 题/接口/日志/OS/安全/事故复盘） | **知识来源**：每写一个功能对应一篇，README 里"对号入座" |
| `mq/` + `redis/` | RabbitMQ/Redis 四阶段教学 | 消息与缓存方案的依据 |
| `learn-center/` | 在线学习平台（抢课四层漏斗/CAS 支付/打卡 Bitmap/ZSet 榜单/HLL UV/错题本/通知/AI） | **复用其代码模式**：ApiResponse/UserContext(AuthInterceptor)/Ids(雪花)/lua 原子预扣 |
| `community/` | 社区论坛（审核管道/Feed/热榜衰减/幂等） | 复用审核状态机与热榜 HeatDecayJob 模式 |
| `mq/mq-mall/` | 订单消息系统（本地消息表/Confirm/补偿/死信/对账） | 复用可靠性全家桶（ReliableMessagePublisher 全套可直接抄） |
| `stage6/` `stage7/` | 新技术与 AI 教学 | AI 能力按 learn-center 的 Mock/Real 双实现模式 |

**约束**：Spring Boot 3.2 / Java 17 / MySQL 8 / Redis 7 / RabbitMQ（与体系其他项目一致，端口分配见下）；测试用纯 Mockito 不依赖容器（学习体系惯例：`mvn test` 无 Docker 全绿）。

## 二、项目定位：一个"技术中台 + 增值业务"平台

选一个能自然装下全部未落地知识点的业务壳——**"开发者成长平台"**（与教学体系自洽，简历可讲）：

```
big-center —— 开发者成长平台
├── 门户/短链服务      s.cn/Ab3xK9 推广链接（短链接系统 12 篇）
├── 用户服务          JWT 双 token 登录（19 篇）+ 扫码登录（12 篇）
├── 学习/社区业务      课程+帖子（点赞 14 篇 / 消息推送 14 篇）
├── 活动运营          限时红包雨（抢红包 14 篇）+ 限量课程秒杀（learn-center 模式）
├── 支付中心          模拟支付网关（支付系统 15 篇：状态机 CAS/回调幂等/对账）
├── 基础设施          限流器（12 篇）/ 分布式 ID（11 篇）/ 接口规范（16）/ 日志规范（17）
└── 实验室模块        java-core-lab：并发/基石/设计模式的可运行 main 示例集
```

## 三、功能-知识点对照表（实现的验收清单）

### A. 之前没有代码落点的知识点（本项目核心新增，必须全部有）

| 功能 | 教学出处 | 技术要点 |
|------|---------|---------|
| 短链接服务 | java-core/12 题一 | 发号器(雪花)+Base62、302 跳转统计、布隆防穿透、Redis 缓存 |
| 扫码登录 | java-core/12 题二 | 二维码 uuid 状态机 WAITING→SCANNED→CONFIRMED、Redis TTL 5min、轮询接口 |
| 限流器 | java-core/12 题三 | Redis Lua 滑动窗口（ZSET 时间戳）+ 注解 @RateLimit AOP 应用到接口 |
| 点赞系统 | java-core/14 题一 | Set 防重+INCR(Lua 原子)→MQ 异步刷库→对账修正 |
| 抢红包 | java-core/14 题二 | 二倍均值预拆分入库、LPOP 原子认领、24h 延迟退款（TTL+DLX） |
| 消息推送 | java-core/14 题三 | SSE 在线推+离线落库+拉取兜底；未读数=最新ID−已读位点 |
| JWT 登录 | java-core/19 | 双 token（15min+7d 刷新）、BCrypt 密码、登出黑名单、越权 where user_id 防护 |
| 分布式 ID | java-core/11 | 雪花（时钟回拨处理）+ 号段两种实现，供短链/订单用 |
| 设计模式实战 | java-core/06 五篇 | 责任链（参数校验链）/策略（支付渠道）/状态机（红包生命周期）/门面+模板方法（支付网关） |

### B. 从现有项目"总装"进来的能力（证明融会贯通）

| 能力 | 抄哪里 | 在 big-center 的角色 |
|------|--------|---------------------|
| 可靠消息全家桶 | mq-mall 的 ReliableMessagePublisher/死信/对账 | 点赞刷库、红包退款、推送 fanout 都走它 |
| 秒杀四层漏斗 | learn-center 的 EnrollController+lua | "限量新课抢购" |
| CAS 支付状态机 | learn-center PaymentService + 15 篇 | 支付中心（回调幂等/掉单查单/三单对账） |
| 审核/热榜 | community 的 AuditService/HeatDecayJob | 帖子审核、热帖榜 |
| AI 能力 | learn-center ai/ 包 Mock/Real 双实现 | AI 学习报告 |

### C. java-core-lab 实验室（子模块，纯 JDK main 方法集）

`big-center/lab/`——并发八篇/基石五篇/设计模式五篇每篇一个可跑的包：

```
lab/src/main/java/lab/
├── basics/     集合(HashMap 扩容演示)/泛型反射/Stream/异常/String 相等性
├── concurrent/ 线程状态/synchronized 锁升级日志/volatile+DCL/CAS+ABA(AtomicStampedReference)
│               线程池七参数演示/AQS 自实现锁/CHM+ThreadLocal 泄漏演示/虚拟线程
├── patterns/   单例(五种写法)/代理(JDK+CGlib)/策略+模板(支付渠道演示)/责任链/状态机
└── jvm/        OOM 三种模拟(堆/元空间/栈)/GC 日志解读参数模板/Arthas 练习脚本
```

每个类带 `main` + 教程锚点注释（`// 教程: java-core/01-并发编程/05 线程池`），运行结果用日志打印关键现象（如锁升级标记、ABA 复现步骤）。

## 四、技术规格（新会话直接照此执行）

- **端口**：8085（避免与 8090/8081/8082/8084 冲突）
- **DB**：复用 mq-mall-mysql(3307)，建库 `big_center`；Redis 复用 6380；RabbitMQ 复用 5673
- **包结构**：`com.bigcenter.{shortlink,auth,pay,redpack,like,push,rate,common,lab}`——每包一个 README 小节说明对应教程
- **规范落地**（16/17 篇自己吃自己的狗粮）：ApiResponse 统一基座、错误码分段（1xxx 用户/2xxx 短链/3xxx 红包/4xxx 支付）、MDC traceId、日志六要素
- **测试**：每模块至少 3 个 Mockito 用例（学 learn-center/redis-demo 的写法，重点钉 Lua 语义/状态机 CAS/幂等）
- **CI**：照 learn-center 的 `.github/workflows/ci.yml` 抄
- **README**：架构图（Mermaid）+ API 路线 + 与 21 篇教学的对号入座表（这是本项目的教学价值所在）
- **语言**：代码注释与 README 全中文，与体系一致

## 五、实施顺序建议（新会话按此推进）

1. **骨架**：pom/WebConfig/ApiResponse/Ids 雪花/错误码分段 + 建库 SQL → 编译过 + IdsTest 绿
2. **JWT 认证**（19 篇）：注册登录/双 token/黑名单/拦截器 → 锁定 AuthInterceptor 与 learn-center 差异（JWT 验签）
3. **短链服务**（12 篇）：发号+跳转+统计，配布隆
4. **支付中心**（15 篇）：门面+策略+状态机 CAS+回调幂等+对账 Job（设计模式在这里落地）
5. **红包**（14 篇）：预拆分/认领/延迟退款——用上可靠消息
6. **点赞+推送**（14 篇）：Lua 原子+MQ 刷库+SSE
7. **扫码登录**（12 篇）：状态机+轮询
8. **限流器**（12 篇）：Lua 滑动窗口+@RateLimit AOP 打到前面所有接口上
9. **java-core-lab**：并发/基石/设计模式 main 集（纯增量，随时可做）
10. **收尾**：README 对号入座表全表填完 + 全模块测试 + 推 GitHub

每步完成标准：编译过 + 该模块测试绿 + README 对应小节更新 + git 提交（中文提交语，不带任何尾注，committer=clz）。

## 六、远程与提交约定

- 仓库：并入 `git@github.com:availname/study.git`（学习体系单仓库，study/ 根已是大仓库——**big-center 直接作为子目录提交，不要另开仓库**）
- 提交人：clz <2486681786@qq.com>（本机 git 已配置）；**提交信息不带 Co-Authored-By/Claude 尾注**
- gh CLI：`D:\tools\gh\bin\gh.exe`（不在 PATH）；push 用 SSH（443 配置在 ~/.ssh/config）
- 端口/容器：mq-mall-mysql(3307)/mq-mall-redis(6380)/mq-mall-rabbitmq(5673) 已有 compose，直接复用
