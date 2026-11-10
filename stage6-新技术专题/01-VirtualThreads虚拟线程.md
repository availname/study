# 01 · Virtual Threads 虚拟线程（Java 21 / Loom）

> 🎬 **配套交互演示（容易看懂版）**：[diagrams/virtual-threads.html](../diagrams/virtual-threads.html)——两种餐厅用人方式：专属服务员 vs 灵活跑位，看同样 50 桌客人两种模式的人力差距。

Java 并发编程 20 年来的最大变革。一句话：**线程变得和对象一样便宜**——百万级线程不再是梦想，IO 密集型应用的并发代码大幅简化。

## 旧世界的痛：平台线程太贵

你写的每一行 `@RabbitListener`、每一次 `HttpClient` 调用，底层都是**平台线程（OS 线程）**在干活：

```
平台线程 = 1:1 映射 OS 线程
  内存：每个 ~1MB 栈空间
  数量：几百个就把系统压弯（上下文切换、调度）
```

于是一切 IO 密集场景都要"省着用线程"：

- **线程池**（池化复用，但池子大小=并发上限）
- **异步回调**（CompletableFuture 层层嵌套，代码变回调地狱）
- **响应式框架**（Reactor/WebFlux，性能好但学习曲线陡、堆栈难读——见 [08 篇路线之争](08-响应式与虚拟线程路线之争.md)）

## 虚拟线程：JVM 层的"用户态线程"

```
虚拟线程（百万级，~KB 级栈，JVM 管理）
   │ mount / unmount（挂载/卸载）
载体线程（Carrier Thread，= CPU 核数个平台线程）
   │
   OS
```

**核心机制**：虚拟线程在执行**阻塞操作**（网络/sleep/锁）时，JVM 把它从载体线程上**卸载（unmount）**，载体线程立刻去跑别的虚拟线程；阻塞结束后再挂载回来继续执行。

- **阻塞不再昂贵**——你写同步代码，JVM 帮你异步
- 数量：`Executors.newVirtualThreadPerTaskExecutor()` 开百万个都没压力

## 使用方式（Spring Boot 3.2 一行开启）

```yaml
spring:
  threads:
    virtual:
      enabled: true    # Tomcat 请求处理、@Scheduled、@Async 全部切到虚拟线程
```

就这一行。community 项目加上它之后：**每个 HTTP 请求、每个定时任务都跑在虚拟线程上**，Tomcat 不再受默认 200 线程池限制，慢接口不再挤占快接口。

代码层面按需创建：

```java
// 万级并发调用外部服务（旧写法：线程池只有 200 并发；新写法：3 万并发轻松）
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<String>> futures = new ArrayList<>();
    for (int i = 0; i < 30_000; i++) {
        futures.add(executor.submit(() -> callRemoteApi(i)));   // 3 万个虚拟线程
    }
    // 等待全部完成……
}
```

## 落地：community 与 MQ 消费者怎么受益

### 1. community 开启虚拟线程（一行配置）

```yaml
# community/application.yml 追加
spring:
  threads:
    virtual:
      enabled: true
```

效果：审核消费者里 `Thread.sleep(800)` 模拟的"AI 审核耗时"若换成**真实 LLM 调用**（几十秒级），虚拟线程下 8 个消费者并发处理 8 个帖子互不拖累；平台线程模式下你得手动调大并发池还得小心 DB 连接池耗尽。

### 2. MQ 消费者是虚拟线程的完美舞台

MQ 消费的本质 = **IO 密集**（等 broker、等 DB、等下游）。虚拟线程下：

```java
// 批量拉取消息后，每条消息一个虚拟线程并行处理（以前要用线程池+计数器管生命周期）
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Callable<Void>> tasks = messages.stream()
            .map(m -> (Callable<Void>) () -> { handle(m); return null; })
            .toList();
    executor.invokeAll(tasks);   // 全部完成才返回——批量消费的并行版
}
```

**注意纪律：虚拟线程不要池化**。它的成本已经是"对象级"，池化反而引入排队瓶颈——**一个任务一个虚拟线程，用完即弃**（官方设计意图）。

### 3. 高并发 HTTP 客户端

批量调用 N 个下游 API（聚合查询、群发通知）：以前 Semaphore+线程池调参，现在直接开 N 个虚拟线程。

## ⚠ 核心坑：Pinning（钉死）

虚拟线程在**`synchronized` 块内阻塞**时，**无法卸载**——它会钉死（pin）载体线程，阻塞多久就浪费多久。这是 JDK 21 的著名限制（JDK 24 的 JEP 491 已彻底解决，但你现在用的 JDK 21 有）。

排查与规避：

```bash
# JVM 参数：pinning 发生时打日志（加上它跑压测，看有没有 warning）
-Djdk.tracePinnedThreads=full
```

```java
// 坏：synchronized 块里做 IO
synchronized (lock) {
    httpClient.send(...);        // 阻塞 → pin 住载体线程！
}

// 好：改用 ReentrantLock（JUC 锁在阻塞时可卸载）
private final ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    httpClient.send(...);        // 阻塞时正常 unmount
} finally { lock.unlock(); }
```

第二类坑：**连接池大小成为新瓶颈**。虚拟线程百万并发，但 MySQL 连接池只有 10——瓶颈转移，池子反而成了保护机制（限流）。**虚拟线程 ≠ 无限吞吐，瓶颈只会转移不会消失**（IO→DB连接→下游容量）。

## 什么时候用/不用

| 用 ✅ | 不用/谨慎 ❌ |
|-------|-------------|
| IO 密集：MQ 消费、HTTP 调用、DB 访问 | CPU 密集：加密、图像处理（虚拟线程无益，核就那么多） |
| 每连接一个线程的老代码直接受益 | 已是成熟响应式系统（迁移收益小于成本） |
| 高并发扇出聚合 | 极小并发场景（差异无感） |

## 面试问法

**Q: 虚拟线程是什么？和平台线程的区别？**
A: JVM 管理的用户态线程：阻塞时 unmount 让出载体线程（数量=核数的平台线程），恢复时 mount 回来。内存 KB 级、可百万并发；平台线程 1:1 映射 OS、MB 级栈、数量受限。写同步代码获得异步性能。JDK 21 正式（JEP 444），Spring Boot 3.2 一行配置启用。注意 JDK 21 的 synchronized pinning 问题和"瓶颈转移"（DB 连接池成为新上限）。

**Q: 虚拟线程和响应式（WebFlux）怎么选？**
A: 见 [08 篇](08-响应式与虚拟线程路线之争.md)——简版：新项目/IO 密集优先虚拟线程（简单直读），已运行的高性能响应式系统不必迁移。

---

**回望**：MQ 教学里 stage2/01 讲"单线程+epoll"是 Node/Redis/Netty 的路线；Loom 是 Java 的第三条路——**线程继续用，成本交给 JVM**。
**下一篇**：[02-SpringAI-LLM审核管道](02-SpringAI-LLM审核管道.md)。
