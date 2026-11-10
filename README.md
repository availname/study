# 🗺️ Java 后端学习工作区 · 总导航

> 🚀 **配套学习应用**：双击打开 **[study-app.html](diagrams/study-app.html)**——把整个教学体系装进一个零依赖网页：课程导航与完成度追踪（148 篇）、23 个示意图画廊、58 张面试翻转卡、12 坑速览、任务清单（进度存浏览器本地）。

这是一个完整的 **Java 后端开发学习体系**：约 160 篇教学文档 + 4 个可运行项目 + 23 个交互示意图 + 12 坑实录。本页是唯一入口，按下面的路线走即可。

## 学习路线（按此顺序，不要跳）

```
第 0 步：环境准备（一天）
  Docker 装好 → mq/mq-mall 和 redis/redis-demo 的容器起起来 → 确认 mvn 编译通过

第 1 步：入门（两周）——先"会用"
  mq/stage1-入门使用        （RabbitMQ：装/五种模式/HelloWorld）
  redis/stage1-入门使用     （Redis：五大类型/Key 管理/SpringBoot 整合）
  └─ 边学边玩两个 demo 工程的接口

第 2 步：原理（三周）——再"懂原理"
  redis/stage2-底层原理     （单线程/epoll/持久化/主从集群）
  mq/stage2-底层原理        （AMQP/持久化/Confirm/三大MQ对比）
  java-core/01-并发编程     （JUC 八篇——虚拟线程的前传）
  java-core/02-JVM虚拟机    （内存/GC/调优/排查）
  └─ 并发和 JVM 是面试王炸，慢点学、多动手

第 3 步：排坑（两周）——然后"会排坑"
  redis/stage3-问题解决     （穿透击穿雪崩/一致性/分布式锁/大Key）
  mq/stage3-问题解决        （丢失/重复/乱序/堆积/死信）
  java-core/03-MySQL深入    （索引/MVCC/锁/日志/优化）
  └─ 每篇末尾的"面试问法"背骨架；踩坑实录配合看

第 4 步：实战（三周）——最后"敢上高并发"
  mq/stage4-高并发实战      （秒杀/订单超时/大促链路）
  redis/stage5-类型场景实战 （8 个类型场景）
  java-core/04-Spring核心   （IoC/AOP/事务失效八连）
  java-core/05-微服务全家桶 （Nacos/Feign/Gateway/Sentinel/Seata）
  └─ 跑通 mq-mall 和 community 两个项目（README 有 API 玩法路线）

第 5 步：扩展与就业（持续）
  stage6-新技术专题         （虚拟线程/Spring AI/向量检索/OTel/CI/CDC）
  stage7-AI应用开发         （Spring AI/RAG进阶/AI工作流与Agent/MCP/AI安全/微调选型/多模态）
  redis/stage5-类型场景实战 / java-core/06-设计模式 / 07-网络基础
  mq-mall + community 推 GitHub（配 GitHub Actions CI）
  面试前：java-core/面试速查手册 + 简历项目话术 + 面试反问与Offer沟通
  安全补强：java-core/19-登录态与接口安全（两个项目登录态的生产升级地图）
```

## 目录索引

| 目录 | 内容 | 入口 |
|------|------|------|
| `mq/` | 消息队列 16 篇 + Kafka 专篇 5 + RocketMQ 专篇 2 + 视频平台场景 4 + **延迟任务方案横向对比专题** + **mq-mall 项目**（订单/秒杀/可靠性） | [mq/README](mq/README.md) |
| `redis/` | Redis 19 篇 + 8 场景 + **redis-demo** | [redis/README](redis/README.md) |
| `stage6-新技术专题/` | 虚拟线程/Spring AI/向量检索/OTel/CICD/CDC/Testcontainers/响应式 | [README](stage6-新技术专题/README.md) |
| `stage7-AI应用开发/` | LLM全景/Spring AI流式/RAG/RAG进阶/AI工程化/AI工作流与Agent/MCP/AI安全评测/**微调选型(LoRA)/多模态** | [README](stage7-AI应用开发/README.md) |
| `community/` | **总毕业项目**（四件套联动的社区论坛） | [README](community/README.md) |
| `learn-center/` | **终极整合项目**：在线学习平台（MyBatis-Plus + 限量抢课秒杀 + 超时取消延迟链 + 签到/榜单/UV + 刷题错题本） | [README](learn-center/README.md) |
| `java-core/` | Java 后端核心 50+ 篇（并发/JVM/MySQL/Spring/微服务/设计模式/网络/Git/**系统设计 7 题/接口设计/日志规范/OS 基础/登录态与接口安全/K8s 入门**）+ **面试速查手册** + **模拟面试追问链** + **简历项目话术** + **面试反问与Offer沟通** | [README](java-core/README.md) |
| `diagrams/` | 23 个交互示意图（🐣 生活比喻版 + 🔧 技术版） | [README](diagrams/README.md) |
| `mq/踩坑实录-完整复盘.md` | 12 个真实坑（现象→排查→根因→修复→教训） | [直接看](mq/踩坑实录-完整复盘.md) |
| `learn-center/README.md` | learn-center 运行/API/知识融合映射 | [直接看](learn-center/README.md) |
| `java-core/面试速查手册.md` | 全部模块高频面试题 + 答题骨架 | [直接看](java-core/面试速查手册.md) |
| `java-core/简历项目话术.md` | 两个项目怎么写进简历 + 面试追问应对 | [直接看](java-core/简历项目话术.md) |

## 交互示意图索引（🐣 生活比喻优先）

| 学什么 | 打开 | 比喻 |
|--------|------|------|
| 为什么用 MQ | [why-mq.html](diagrams/why-mq.html) | 快递柜：放柜就走 vs 打电话催你下楼 |
| Redis 为什么快 | [redis-fast.html](diagrams/redis-fast.html) | 超级服务员+叫号屏：1 人干过 10 人 |
| 虚拟线程 | [virtual-threads.html](diagrams/virtual-threads.html) | 服务员灵活跑位 vs 每桌专属 |
| TCP 握手挥手 | [tcp-handshake.html](diagrams/tcp-handshake.html) | 接电话：“喂？听得到吗”×3 |
| 缓存三大问题 | [cache-3problems.html](diagrams/cache-3problems.html) | 奶茶店：穿透/击穿/雪崩三灾难 |
| HTTP 请求旅程 | [http-journey.html](diagrams/http-journey.html) | 寄快递：写单→查地址→接电话→上锁→寄出 |
| 事务 ACID | [mysql-transaction.html](diagrams/mysql-transaction.html) | 记账本：草稿+钢笔小票防赖账 |
| ThreadLocal | [threadlocal.html](diagrams/threadlocal.html) | 健身房更衣柜：一人一柜 |
| 分布式锁 | [distributed-lock.html](diagrams/distributed-lock.html) | 商场储物柜：抢锁/钥匙/自动开 |
| 线程池流程 | [thread-pool.html](diagrams/thread-pool.html) | 核心满排队、队满扩容、满了拒绝 |
| 线程生命周期 | [thread-lifecycle.html](diagrams/thread-lifecycle.html) | 六状态流转（非法流转会报错） |
| B+ 树与回表 | [bptree.html](diagrams/bptree.html) | 两棵树对照：回表 vs 覆盖索引 |
| 消息可靠性全链路 | [reliability.html](diagrams/reliability.html) | 本地消息表→Confirm→补偿→死信 四场景 |
| 秒杀四层漏斗 | [seckill-funnel.html](diagrams/seckill-funnel.html) | 10w 用户→DB 只收 1000 单 |
| Spring Bean 生命周期 | [spring-bean-lifecycle.html](diagrams/spring-bean-lifecycle.html) | 十一步 + 三级缓存循环依赖 |
| 微服务架构全景 | [microservice-arch.html](diagrams/microservice-arch.html) | 10 组件点读 + 请求旅程动画 |
| G1 垃圾回收 | [g1-gc.html](diagrams/g1-gc.html) | Region 化堆：Minor/Mixed GC |
| MVCC 版本链 | [mvcc.html](diagrams/mvcc.html) | RC/RR 判定动画（读到 1000 还是 500） |
| 支付状态机 CAS | [payment-state-machine.html](diagrams/payment-state-machine.html) | 支付 vs 关单竞态：CAS 仲裁 vs 双写资损 |
| Kafka 为什么快 | [kafka-fast.html](diagrams/kafka-fast.html) | 分拣中心：还书车只进不出（顺序写）× 五支柱对照 |
| CAS/ABA 时序 | [cas-aba.html](diagrams/cas-aba.html) | 值相同≠没发生过：版本号拆穿改走又改回 |
| RedLock 论战 | [redlock-debate.html](diagrams/redlock-debate.html) | Martin vs antirez：GC 停顿/时钟跳变 → fencing token 拒收旧凭证 |
| 延迟任务双保险 | [delay-task.html](diagrams/delay-task.html) | 延迟消息(主) vs 扫表(兜) vs CAS(裁) 四结局 |

## 全部动手任务（总检查清单）

**环境类**
- [ ] Docker 起 mq-mall 三件套 + redis-learn，两个 demo 工程跑通
- [ ] mq-mall / community 推上 GitHub，CI 绿勾

**mq 教程**
- [ ] 五种工作模式 demo 全跑通；Confirm/Return/死信链路演练
- [ ] mq-mall：下单→支付→超时关单→秒杀压测→reconcile 对账一致

**redis 教程**
- [ ] 五大类型业务化练习；分布式锁并发抢锁演示
- [ ] 布隆过滤器/语义缓存概念能讲清（stage6/03）

**java-core**
- [ ] 并发八篇的 main 方法示例全部跑过
- [ ] JVM 五篇 + Arthas 本机实操一遍
- [ ] 面试速查手册过三遍，每题能口述 60 秒

**项目**
- [ ] community 全 API 路线走通（README 十一步）
- [ ] 两个项目各写一段简历描述（见简历话术）并能答出追问

## 环境速查

```bash
# 基础设施（mq-mall 三件套 + redis 教学）
docker start mq-mall-mysql mq-mall-rabbitmq mq-mall-redis redis-learn

# 三个应用
cd mq/mq-mall && mvn spring-boot:run        # 8090
cd redis/redis-demo && mvn spring-boot:run  # 8081
cd community && mvn spring-boot:run         # 8082
```

| 端口 | 服务 |
|------|------|
| 8090 / 8081 / 8082 / 8084 | mq-mall / redis-demo / community / **learn-center** 应用 |
| 3307 / 6380 / 6381 / 5673 | mq-mall MySQL / mq-mall Redis / redis-learn / RabbitMQ |
| 15673 | RabbitMQ 管理界面（guest/guest） |
