# 06 · AQS 与显式锁

synchronized 是 JVM 内置锁；**AQS（AbstractQueuedSynchronizer）** 是 JUC 大部分同步工具的公共骨架——ReentrantLock、CountDownLatch、Semaphore、CyclicBarrier 底层全是它。学懂 AQS = 一网打尽 JUC。

## AQS 骨架：state + CLH 队列 + 模板方法

```
AQS:
  volatile int state;        ← 同步状态（含义由子类定义！这就是"抽象"的精髓）
     ReentrantLock: state=0 无锁 / 1 持有 / 2 可重入第2次
     CountDownLatch: state=还差几个计数
     Semaphore:       state=剩余许可数
  CLH 等待队列：抢不到 state 的线程，双向链表排队（Node），阻塞在 LockSupport.park()
```

**工作模式（模板方法设计模式——设计模式篇的前菜）**：

- 子类重写 `tryAcquire/tryRelease`（独占）或 `tryAcquireShared/tryReleaseShared`（共享）
- AQS 负责通用的：排队、阻塞（park）、唤醒（unpark）、超时、可中断
- **acquire 流程**：先 tryAcquire（子类逻辑，如 CAS state）→ 失败则加入 CLH 队列 → park 阻塞 → 前驱释放时 unpark → 再 tryAcquire

**公平锁 vs 非公平锁的差别就在 tryAcquire 第一行**：非公平版上来就 CAS 抢（插队，吞吐高）；公平版先 `hasQueuedPredecessors()`（队列里有人就老老实实排队）。

## ReentrantLock：比 synchronized 多四件事

```java
private final ReentrantLock lock = new ReentrantLock();     // 默认非公平

public void doWork() {
    lock.lock();                       // 阻塞获取（可重入：同线程重复 lock 不死锁，state 计数）
    try {
        // 临界区
    } finally {
        lock.unlock();                 // ★ 必须在 finally 里！少解一次锁 = 永久锁死
    }
}
```

| 能力 | synchronized | ReentrantLock |
|------|-------------|---------------|
| 可重入 | ✅ | ✅ |
| 自动释放（异常安全） | ✅（代码块结束自动释放） | ❌ 手动 unlock（必须 finally） |
| **tryLock 超时** | ❌ | ✅ `tryLock(3, SECONDS)`——拿不到就放弃（避免死等） |
| **可中断** | ❌（等锁时 interrupt 无效） | ✅ `lockInterruptibly()`——等锁时能被中断（打破死锁死等） |
| **公平锁** | ❌ | ✅ 构造参数 `new ReentrantLock(true)` |
| Condition 多条件队列 | 单一 wait/notify | ✅ 多个 Condition 精准唤醒 |
| 性能 | JDK6+ 优化后基本持平 | 持平 |

**选型口诀**：默认 synchronized（简单+自动释放）；需要 tryLock/可中断/多条件 → ReentrantLock。**ReentrantLock 也是 stage6/01 虚拟线程场景推荐的锁**（synchronized 内阻塞会 pin 载体线程，JDK21）。

## Condition：精准唤醒（对比 wait/notify）

`synchronized + wait/notify` 只有一个等待队列，`notifyAll` 全部唤醒（有的线程被唤醒后发现条件不满足又睡回去——惊群）。Condition 给每个条件一个独立队列：

```java
ReentrantLock lock = new ReentrantLock();
Condition notFull  = lock.newCondition();   // "队列不满"条件
Condition notEmpty = lock.newCondition();   // "队列不空"条件

// 生产者
lock.lock();
try {
    while (isFull()) notFull.await();       // 不满才放
    queue.add(item);
    notEmpty.signal();                       // 只唤醒等"非空"的消费者
} finally { lock.unlock(); }
```

这就是 `ArrayBlockingQueue` 的内部实现——所以**生产上直接用 BlockingQueue**（06 篇的阻塞队列家族），Condition 是它的零件。

## JUC 协作三件套（都基于 AQS）

### CountDownLatch：等 N 件事全部完成

```java
CountDownLatch latch = new CountDownLatch(3);      // 计数 3
// 3 个线程各自干完活：latch.countDown();           // 计数 -1
latch.await();                                     // 主线程阻塞到计数归零
```

场景：主线程并行启动 3 个初始化任务（拉配置、连 DB、预热缓存），**全部完成才对外服务**。计数归零后不可重用（一次性的）。

### CyclicBarrier：N 个线程互相等到齐再一起走

```java
CyclicBarrier barrier = new CyclicBarrier(4, () -> System.out.println("人到齐，开会"));
// 4 个线程各自到达后 barrier.await(); 全员到齐触发回调，然后大家继续
barrier.reset();    // 可重用（Cyclic = 循环）
```

场景：多玩家副本"全员就位开打"、分阶段计算（每阶段结束等齐再进下一阶段）。**与 CountDownLatch 的区别一句话：一个是"等事件"，一个是"等人齐"；前者一次性，后者可重用。**

### Semaphore：N 张许可证（限流器）

```java
Semaphore permits = new Semaphore(10);      // 10 个许可
permits.acquire();                           // 拿不到就阻塞（或 tryAcquire 超时版）
try { useResource(); }
finally { permits.release(); }
```

场景：**并发数限制**——同时最多 10 个连接打下游、限流闸门。与线程池的关系：线程池限的是"本地线程数"，Semaphore 限的是"对某个资源的并发使用数"（可以跨多个线程池用）。呼应 r-stage2/02：**Redis 单线程本质就是一个 Semaphore(1)**。

## 死锁：四条件与排查

死锁四必要条件：**互斥、持有并等待、不可剥夺、循环等待**。破坏任一即可预防（最常用：按固定顺序加锁，破坏循环等待）。

```java
// 经典死锁：t1 锁A后要B；t2 锁B后要A
// 排查：jstack 看到_detected deadlock！ / Arthas thread -b 直接找凶手
```

## 面试问法

**Q: AQS 是什么？**
A: JUC 同步工具的公共骨架：volatile state 表同步状态 + CLH 双向队列排队 + LockSupport park/unpark 阻塞唤醒。子类（模板方法模式）只需重写 tryAcquire/Release 定义 state 的语义——ReentrantLock 的重入计数、CountDownLatch 的计数、Semaphore 的许可数。公平/非公平差别在 tryAcquire 是否先查队列。

**Q: CountDownLatch 和 CyclicBarrier 区别？**
A: 一个"等事件"一次性（countDown 计数归零放行 await，不可重用）；一个"等人齐"可循环（await 全员到齐触发 barrierAction，reset 重置）。

---

**下一篇**：[07-并发容器与ThreadLocal](07-并发容器与ThreadLocal.md)。
