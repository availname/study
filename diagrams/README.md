# diagrams：交互式示意图集

**双击任意 HTML 即可在浏览器打开**（单文件零依赖，无需服务器）。每个示意图都是可交互的动画演示，配合对应教程章节食用。

## 🐣 容易看懂版（生活化比喻，浅色卡通风——没基础也能看懂）

| 文件 | 生活比喻 | 对应教程 |
|------|---------|---------|
| [why-mq.html](why-mq.html) | 📦 **快递柜的诞生**：外卖员打电话催你下楼（同步阻塞）vs 放快递柜发取件码（异步）——MQ 三大好处一页看懂 | [MQ stage1/01 为什么需要 MQ](../mq/stage1-入门使用/01-为什么需要消息队列.md) |
| [redis-fast.html](redis-fast.html) | ⚡ **超级服务员的餐厅**：10 桌 10 个服务员干等（BIO）vs 1 个超级服务员+叫号屏（epoll+内存）——Redis 为什么快 | [Redis stage1/01·stage2/01](../redis/stage1-入门使用/01-为什么需要Redis.md) |
| [virtual-threads.html](virtual-threads.html) | 🦸 **两种餐厅用人方式**：每桌专属服务员（平台线程）vs 灵活跑位（虚拟线程）——含 pinning 比喻 | [stage6/01 虚拟线程](../stage6-新技术专题/01-VirtualThreads虚拟线程.md) |
| [tcp-handshake.html](tcp-handshake.html) | 📞 **接电话与说再见**：三次握手=“喂？听得到吗”三次确认；四次挥手=“我说完了”→“等我还有最后几句”；TIME_WAIT=等 2 分钟防“再见”没传到 | [网络基础 01](../java-core/07-网络基础/01-TCP三次握手四次挥手.md) |
| [cache-3problems.html](cache-3problems.html) | ☕ **奶茶店的三个灾难**：货架=Redis（1秒拿）后厨=MySQL（30秒做）；穿透=问不存在的商品、击穿=爆款卖完几百人同抢、雪崩=整排货架同时过期 | [Redis stage3/01](../redis/stage3-问题解决/01-缓存穿透击穿雪崩.md) |
| [http-journey.html](http-journey.html) | 📮 **寄快递**：输入网址后发生了什么——写快递单→查通讯录(DNS)→接通电话(TCP)→上锁(HTTPS)→寄出→拆信→回信→渲染 | [网络基础 02](../java-core/07-网络基础/02-HTTP协议演进与HTTPS.md) |
| [mysql-transaction.html](mysql-transaction.html) | 🏦 **老掌柜的记账本**：转账断电→撕草稿回滚（原子性）；钢笔小票（redo）防赖账（持久性）；MVCC 快照防脏看（隔离性） | [MySQL 深入 02 事务](../java-core/03-MySQL深入/02-事务与MVCC.md) |
| [threadlocal.html](threadlocal.html) | 🧥 **健身房的更衣柜**：每人一个柜子互不干扰（线程隔离）；⚠ 线程池线程不死→不 remove 就泄漏 | [Java 并发 07 ThreadLocal](../java-core/01-并发编程/07-并发容器与ThreadLocal.md) |
| [distributed-lock.html](distributed-lock.html) | 🔒 **商场公共储物柜**：SET NX EX 抢锁、requestId 校验释放、EX 断电兜底 | [Redis stage3/03 分布式锁](../redis/stage3-问题解决/03-分布式锁.md) · [Java 并发 03](../java-core/01-并发编程/03-volatile与JMM内存模型.md) |

## 🔧 技术版（深色工程风，带参数调节与状态面板）

| 文件 | 演示什么 | 对应教程 | 操作 |
|------|---------|---------|------|
| [thread-lifecycle.html](thread-lifecycle.html) | 线程六状态流转动画 | [Java 并发 01](../java-core/01-并发编程/01-线程基础与生命周期.md) | 点按钮触发事件，看线程球在状态间移动 + 非法流转会报错 |
| [thread-pool.html](thread-pool.html) | **线程池执行流程**（核心→队列→临时→拒绝） | [Java 并发 05](../java-core/01-并发编程/05-线程池ThreadPoolExecutor.md) | 调参数、点洪峰提交 20 任务；试"队列 9999"复现无界队列 OOM 坑 |
| [bptree.html](bptree.html) | **B+ 树索引与回表**（两棵树 + 查询路径高亮） | [MySQL 深入 01](../java-core/03-MySQL深入/01-索引B+树与回表.md) | 对比 SELECT *（回表 2 次树查找）vs 覆盖索引（1 次结束） |
| [seckill-funnel.html](seckill-funnel.html) | 秒杀四层漏斗（削峰可视化） | [MQ 实战 stage4/01](../mq/stage4-高并发实战/01-削峰填谷-秒杀系统.md) | 设用户数/库存，点"开始秒杀"看逐层过滤 + DB 匀速 |
| [reliability.html](reliability.html) | **消息可靠性全链路**（本地消息表→Confirm→补偿→死信） | [MQ stage3/01](../mq/stage3-问题解决/01-消息丢失怎么防.md) · [community](../community/README.md) | 四大故障场景切换，单步/自动播放，看"三道机制咬合成闭环" |
| [spring-bean-lifecycle.html](spring-bean-lifecycle.html) | Spring Bean 十一步生命周期 + **三级缓存循环依赖**流转 | [Spring 核心 01](../java-core/04-Spring核心原理/01-IoC容器与Bean生命周期.md) | 切换两个演示：生命周期逐步走 / 循环依赖三级缓存流转（含构造器注入无解反例） |
| [microservice-arch.html](microservice-arch.html) | **微服务架构全景**（10 组件交互图） | [微服务 05](../java-core/05-微服务全家桶/05-Sentinel熔断限流.md) | 点组件看说明；"请求的旅程"动画演示 网关→Nacos→Feign→DB→MQ 全链路 |
| [mvcc.html](mvcc.html) | **MVCC 版本链与 ReadView 判定动画**（RC/RR 对比） | [MySQL 深入 02](../java-core/03-MySQL深入/02-事务与MVCC.md) | 选隔离级别，走"修改→提交→读取"剧本，看版本链逐版本判定规则与最终读到多少钱 |
| [g1-gc.html](g1-gc.html) | G1 垃圾回收（Region 化堆 + Minor/Mixed GC） | [JVM 03](../java-core/02-JVM虚拟机/03-垃圾回收算法与收集器.md) | 分配对象看 Eden 满→Minor GC→晋升→Mixed GC 全流程，附统计与日志 |
| [payment-state-machine.html](payment-state-machine.html) | **支付状态机 CAS 竞态**（支付 vs 关单） | [系统设计 15](../java-core/15-系统设计·支付系统.md) | 切换 ✅CAS/❌先查再改，跑四个剧本看"一条 UPDATE 完成仲裁" vs 双写资损事故 |
| [kafka-fast.html](kafka-fast.html) | **Kafka 为什么快·五支柱**（每支柱一组 ❌慢 vs ✅快 对照实验） | [Kafka 专篇 05](../mq/stage5-Kafka专篇/05-Kafka高性能读写原理.md) | 切 🐣比喻/🔧技术双视角；顺序写计数、PageCache 命中率、拷贝次数 4→2、往返 12→1、吞吐条 1 盘→6 盘 |
| [cas-aba.html](cas-aba.html) | **CAS/ABA 时序竞态**（值相同 ≠ 没发生过） | [Java 并发 04](../java-core/01-并发编程/04-CAS与原子类.md) | 切 ❌无版本号/✅AtomicStampedReference，跑 ①正常 CAS ②ABA 时序 ③无锁栈回拨 ④自旋空转四个剧本，看版本号怎么拆穿"改走又改回来" |
| [redlock-debate.html](redlock-debate.html) | **RedLock 论战与 Fencing Token**（Martin vs antirez） | [redis stage3/05](../redis/stage3-问题解决/05-RedLock争议与FencingToken.md) | 五剧本四角色：主从丢锁→RedLock 抢锁→GC 停顿→时钟跳变→fencing token 拒收旧凭证，双辩手立场面板 |
| [delay-task.html](delay-task.html) | **延迟任务双保险**（延迟消息 vs 扫表兜底） | [mq stage4/02](../mq/stage4-高并发实战/02-订单超时自动取消.md) · [延迟任务专题](../mq/延迟任务方案横向对比.md) | 四剧本：消息准时关单/消息丢了扫表兜/CAS 仲裁双路竞态/支付先赢两路落空，看三层防线（及时·完整·正确）各司其职 |

## 特性

- 全部单文件、零外部依赖、离线可看
- 深色主题、带操作按钮与日志区
- 数据可调（线程池参数 / 秒杀用户数），**自己实验得出结论**
