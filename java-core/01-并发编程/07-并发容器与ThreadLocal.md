# 07 · 并发容器与 ThreadLocal

> 🎬 **配套交互演示（容易看懂版）**：[diagrams/threadlocal.html](../../diagrams/threadlocal.html)——健身房更衣柜：切线程身份存取，亲眼看"你的柜子别人开不了"和"不 remove 就泄漏"。

普通容器（HashMap/ArrayList）在多线程下要么报错（ConcurrentModificationException）要么丢数据。JUC 提供了一批并发容器——重点讲 **ConcurrentHashMap**（面试最高频）和 **ThreadLocal**（坑最多），BlockingQueue 家族一笔带过（06 篇已见 Condition 的用法）。

## 一、ConcurrentHashMap：为什么 HashMap 不能并发用

HashMap 并发三宗罪：
1. **数据覆盖**：两线程同时 put，后者覆盖前者
2. **扩容死循环**（JDK7 头插法扩容形成环形链表 → CPU 100%）——JDK8 改尾插解决
3. **size 不准 / ConcurrentModificationException**

### JDK8 的 ConcurrentHashMap 设计（背三层）

```
外层：Node[] table 数组
     ├─ 桶为空 → CAS 写入新节点（无锁！）
     ├─ 桶非空 → synchronized 锁住【桶的头节点】（锁粒度=单个桶，其它桶不受影响）
     └─ 桶链太长(≥8) → 转红黑树（O(N)→O(logN)，防哈希碰撞攻击）
扩容：多线程协同迁移（helpTransfer），每个线程领一段桶区间
size：BaseCounter + CounterCell[] 分段计数（LongAdder 同款思想！stage6 计数篇呼应）
```

**锁粒度的演进**：JDK7 分段锁（Segment extends ReentrantLock，锁一段）→ JDK8 **CAS + synchronized 锁单桶头节点**。粒度越细并发越高。

**为什么用 synchronized 不用 ReentrantLock**：JDK8 优化后 synchronized 有偏向/轻量级锁，桶级竞争下差距可忽略，且省内存（每个 Node 上挂锁对象太贵）。——"内置锁够用了"的时代判断，与你学的锁升级呼应。

**key/value 都不允许 null**：并发下 `get(key)==null` 无法区分"不存在"还是"值就是 null"（有歧义），map 接口语义要求 containsKey 二次确认——并发下二次确认间状态又变了。HashMap 单线程可以 null。

### 使用纪律

```java
// ❌ 复合操作仍然不原子！
if (!map.containsKey(k)) map.put(k, v);      // check-then-act 竞态

// ✓ 用原子复合方法
map.putIfAbsent(k, v);
map.computeIfAbsent(k, key -> expensiveLoad(key));    // 缓存加载的标准写法
```

## 二、CopyOnWriteArrayList：读多写极少列表

原理：**写时复制整个数组**（加锁复制→改→替换引用），读完全无锁（读旧快照）：

```java
List<String> listeners = new CopyOnWriteArrayList<>();   // 事件监听器列表——典型场景
// 写：每次 add 复制整个数组（O(N)，写少无所谓）
// 读：不加锁，永远一致（迭代器是快照，不会 CME）
```

适用：**监听器/配置/白名单**这类"读极多、写极少、列表不大"的场景。写多 = 复制风暴 + 内存翻倍，别用。

## 三、BlockingQueue 家族：生产者消费者的标准件

| 实现 | 特点 | 场景 |
|------|------|------|
| `ArrayBlockingQueue` | 数组、**有界**、一把锁 | 限流队列首选（有界=可控） |
| `LinkedBlockingQueue` | 链表、默认**无界**（慎用！线程池 5 篇的坑） | 可指定容量后可用 |
| `SynchronousQueue` | 零容量，手递手交接 | CachedThreadPool 内部 |
| `PriorityBlockingQueue` | 优先级堆 | 优先级任务 |
| `DelayQueue` | 到期才能取 | 延迟任务（r-stage4/03 ZSet 方案的 Java 版） |

四个方法（put/take 阻塞、offer/poll 超时、add/remove 异常、peek/element 不动）——06 篇 Condition 的实战成品。线程池的工作队列就是它们。

## 四、ThreadLocal：每个线程自己的"口袋"

**原理**：每个 Thread 对象里有一个 `ThreadLocalMap`（key=ThreadLocal 实例的弱引用，value=值）。`threadLocal.get()` = 拿**当前线程**的 map 按 key 查——所以各线程存取互不干扰。

```java
private static final ThreadLocal<User> CONTEXT = new ThreadLocal<>();

CONTEXT.set(currentUser);       // 入口处（拦截器）绑定
...任何层都能 CONTEXT.get()...   // 中间任意层直接取（不用层层传参）
CONTEXT.remove();               // ★ 用完必须移除
```

**用途**：①上下文传递（登录用户/traceId，替代参数层层传递）②非线程安全对象的线程隔离（SimpleDateFormat）③数据库连接/事务绑定（Spring `@Transactional` 的连接就是 ThreadLocal 绑定的！）。

### 内存泄漏原理（必考）

```
Thread 对象（长生命周期，线程池里永不销毁）
  └─ ThreadLocalMap
       └─ Entry: [key=ThreadLocal 弱引用] → [value=你的对象 强引用]
```

ThreadLocal 实例本身可以被 GC（key 是弱引用，会变 null），但 **value 是强引用**，线程不死 value 就一直被 Entry 链持有——线程池里线程永不死 → value 泄漏。

**防泄漏规矩**：

```java
try {
    CONTEXT.set(user);
    doBusiness();
} finally {
    CONTEXT.remove();     // ★ finally 里 remove——和不还锁一个级别的错误
}
```

### ThreadLocal 与虚拟线程的坑（stage6/01 交叉）

虚拟线程**千万级**时，每个虚拟线程一份 ThreadLocalMap → 内存放大；且 ThreadLocal 的 set/get 在虚拟线程上工作正常但**传递跨线程时会丢**（异步切换线程）——跨线程上下文传递用 `ScopedValue`（JDK 21 预览）或 Micrometer 的 context 传播。**记住：ThreadLocal 只在同线程内有效，换线程即失效**。

## 面试问法

**Q: ConcurrentHashMap JDK8 的实现？**
A: CAS+volatile：空桶 CAS 写入；非空桶 synchronized 锁桶头节点（粒度=单桶）；链表长度≥8 转红黑树；扩容多线程协同迁移；计数用 CounterCell 分段（LongAdder 思想）。不允许 null。对比 JDK7 的 Segment 分段锁：粒度从段细化到桶。

**Q: ThreadLocal 内存泄漏？**
A: Thread 里的 ThreadLocalMap，key 是 ThreadLocal 的弱引用（可被 GC 变 null），value 是强引用——线程池线程长存导致 value 无法回收。规约：finally 里 remove()。另：ThreadLocal 不能跨线程传递，虚拟线程场景注意内存放大，跨线程用 ScopedValue/context 传播。

---

**下一篇**：[08-并发实战与虚拟线程衔接](08-并发实战与虚拟线程衔接.md)——收束整个并发模块。
