# Java 后端核心（java-core）

Java 软件开发工程师的**语言与框架核心知识**总目录。与兄弟目录的分工：

```
D:\JavaSpace\study\
├── java-core/   ← 你在这里：Java 语言核心（并发/JVM）+ 框架原理 + 中间件深入
├── mq/          消息队列实战教学（RabbitMQ）
├── redis/       Redis 实战教学
├── stage6-新技术专题/  虚拟线程/Spring AI/向量检索/OTel/CI/CDC
└── community    毕业项目（SpringBoot+MySQL+Redis+MQ）
```

## 全套学习地图（按优先级）

| 优先级 | 模块 | 状态 | 面试权重 | 与已有内容的连接 |
|--------|------|------|---------|----------------|
| ★★★ | [00-Java语言基石](00-Java语言基石/README.md) | ✅ 完整 5 篇 | 集合/泛型/Stream/String 相等性常问 | ConcurrentHashMap（并发07）、TypeReference（序列化） |
| ★★★ | [01-并发编程](01-并发编程/README.md) | ✅ 完整 8 篇 | 后端面试王炸 | 虚拟线程是它的"续集"（[stage6/01](../stage6-新技术专题/01-VirtualThreads虚拟线程.md)）；Redis 单线程模型对照 |
| ★★★ | [02-JVM虚拟机](02-JVM虚拟机/README.md) | ✅ 完整 5 篇 | GC/内存/调优必考 | binlog（你在 MQ 学过）与 GC 日志同源思想 |
| ★★★ | [03-MySQL 深入](03-MySQL深入/README.md) | ✅ 完整 7 篇 | 索引/事务/MVCC 必考 | binlog→CDC（stage6/06）；慢查询→Trace 归因 |
| ★★ | [04-Spring 核心原理](04-Spring核心原理/README.md) | ✅ 完整 5 篇 | IoC/AOP/事务 | community 踩的 @Transactional 自调用坑（并发篇代理失效） |
| ★★ | [05-微服务全家桶](05-微服务全家桶/README.md) | ✅ 完整 7 篇 | SpringCloud Alibaba | 对接你工作区的 pmhub 项目 |
| ★ | [06-设计模式](06-设计模式/README.md) | ✅ 完整 5 篇 | 穿插在源码里讲 | mq-mall 的"可靠消息门面"=门面+模板方法；责任链/状态机篇连支付设计 |
| ★ | [07-网络基础](07-网络基础/README.md) | ✅ 完整 5 篇 | TCP/HTTP | epoll（Redis 篇）与三次握手联合记忆 |
| ★ | [08-Git与协作](08-Git与协作.md) | ✅ | 日常流/分支/事故自救/CI 联动 | 本工作区推 GitHub 的完整实战（SSH 443/gh/踩坑） |
| ★★ | [13-运维部署实战](13-运维部署实战/README.md) | ✅ 完整 5 篇 | Docker/Linux/上线发布/**监控告警/K8s 入门** | 本工作区 compose 实战 + 踩坑实录坑六/七/十一/十二；OTel 篇的 Metrics 姊妹篇；K8s 篇对接微服务 07 多实例问题 |
| 🎯 | [模拟面试追问链](模拟面试追问链.md) | ✅ 13 条链 | 递进式深挖演练 | 沿简历词追问到底（MQ可靠性/幂等/秒杀/锁/JVM/事务/虚拟线程/支付/接口/**安全**） |
| 🎯 | [面试速查手册](面试速查手册.md) | ✅ 40+ 题 | 全模块高频题骨架 | 面试前三遍 |
| 🎯 | [简历项目话术](简历项目话术.md) | ✅ | mq-mall/community 怎么写 + 追问应对 | 含王牌故事讲法 |
| 🎯 | [面试反问与Offer沟通](面试反问与Offer沟通.md) | ✅ | 反问评分题/薪资三句话/多 offer 打分表 | 技术环节之后到拿 offer 之间的事 |
| 🎯 | [10-Java 新特性盘点](10-Java新特性盘点.md) | ✅ | 8→21 特性全景（Record/sealed/模式匹配） |
| 🎯 | [11-分布式系统基础](11-分布式系统基础.md) | ✅ | CAP/BASE/一致性哈希/分布式 ID/事务全景 |
| 🎯 | [12-系统设计三题](12-系统设计三题.md) | ✅ | 短链接/扫码登录/限流器 |
| 🎯 | [14-系统设计进阶三题](14-系统设计进阶三题.md) | ✅ | 点赞/抢红包/消息推送（推拉结合） |
| 🎯 | [15-系统设计·支付系统](15-系统设计·支付系统.md) | ✅ | 状态机 CAS/回调幂等/掉单补偿/三单对账（mq-mall 支付链路期末考） |
| 🎯 | [16-接口设计规范](16-接口设计规范.md) | ✅ | URI/动词幂等表/统一响应体/错误码分段/分页（community 真实锚点） |
| 🎯 | [17-日志规范](17-日志规范.md) | ✅ | 六要素/级别即责任/占位符与异常堆栈/MDC traceId/滚动与脱敏（写给凌晨 3 点的你） |
| 🎯 | [18-操作系统基础](18-操作系统基础.md) | ✅ | 进程线程/用户态内核态/虚拟内存——收拢线程池、锁升级、PageCache、零拷贝、epoll 的 OS 本体 |
| 🎯 | [19-登录态与接口安全](19-登录态与接口安全.md) | ✅ | 认证 vs 授权/三种登录态方案/JWT 登出难题/BCrypt/越权与重放（learn-center 拦截器鉴权的生产升级地图） |
| ★★ | [09-测试与MyBatis](09-测试与MyBatis/README.md) | ✅ 完整 3 篇 | JUnit5+Mockito / MyBatis-Plus / 实战复盘 | community 的 Testcontainers 回归测试；learn-center+community 28 用例落地 |

## 使用方法

1. **顺序**：01 并发 → 02 JVM（这两个是语言地基，必须先学）→ 之后按需
2. 每篇结构统一：**原理图解 → 代码 → 生产坑 → 面试问法**
3. 动手优先级：并发模块每篇都能写 `main` 方法实测；JVM 模块配合 `jps/jmap/jstack` 和 Arthas 实操
4. 与项目联动：学完一个知识点，回头看你 mq-mall/community 里对应的代码（比如学完线程池参数，看 community 的 4 并发消费者本质是什么）

## 阅读体验说明

- 文中的流程图/状态图是 **Mermaid 图**：在 GitHub 网页、IntelliJ IDEA（Markdown 预览装 Mermaid 插件）、Typora 中会渲染成彩色图形；纯文本编辑器里看到的是可读的文字版流程
- 彩色提示框（`> [!TIP]` / `[!WARNING]` / `[!CAUTION]`）是 GitHub 扩展语法：**TIP=技巧**、**IMPORTANT=关键认知**、**WARNING=易错点**、**CAUTION=生产事故高发区**
