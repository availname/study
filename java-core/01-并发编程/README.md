# 01 · 并发编程（JUC）——Java 后端面试王炸

为什么后端面试必考并发：**服务器就是并发程序**——你的每一个 HTTP 请求、每一条 MQ 消息、每一次 Redis 读写都发生在多线程环境里。学完本模块你就能回答：

- 线程池参数怎么设？队列满了怎么办？
- volatile 和 synchronized 的区别？CAS 是什么？ABA 问题？
- ConcurrentHashMap 为什么快？1.7 和 1.8 有什么不同？
- ThreadLocal 为什么会内存泄漏？

以及——**理解了这些，你才能看懂虚拟线程（stage6/01）、Redis 单线程（r-stage2/01）、线程池参数调优（community 的消费者配置）到底在干什么**。

## 篇目（按学习顺序）

| 篇 | 主题 | 核心考点 |
|----|------|---------|
| [01](01-线程基础与生命周期.md) | 线程基础 | 创建方式 / 6 状态 / sleep-wait-join 区别 / 中断机制 |
| [02](02-synchronized与锁原理.md) | synchronized | 三种用法 / 字节码 / **锁升级** / 可重入 |
| [03](03-volatile与JMM内存模型.md) | volatile + JMM | 可见性/原子性/有序性 / **happens-before** / DCL 单例 |
| [04](04-CAS与原子类.md) | CAS 与原子类 | CAS 原理 / **ABA 问题** / LongAdder |
| [05](05-线程池ThreadPoolExecutor.md) | 线程池 ★★★ | 七参数 / 执行流程 / 拒绝策略 / **参数怎么设** |
| [06](06-AQS与显式锁.md) | AQS 与显式锁 | ReentrantLock / Condition / CountDownLatch / Semaphore |
| [07](07-并发容器与ThreadLocal.md) | 并发容器 | ConcurrentHashMap / CopyOnWrite / BlockingQueue / **ThreadLocal 泄漏** |
| [08](08-并发实战与虚拟线程衔接.md) | 实战总结 | 并发工具选型决策表 + 衔接虚拟线程（stage6/01） |

## 学法约定

1. 每篇的"核心命令段"都是可跑的 `main` 方法，**跑一遍再读讲解**；
2. 并发代码必须在多线程下观察——每篇给了观察方法（打印/计数器/工具）；
3. 每篇末尾面试问法背骨架，细节讲得出来；
4. **配套回归**：学完 03（volatile/DCL）回头看 community 的双重检查场景；学完 05（线程池）回头看 RabbitMQ 消费者的 concurrency 本质。

开始：[01-线程基础与生命周期](01-线程基础与生命周期.md)。
