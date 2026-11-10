# 04 · SOLID 原则与实战组合

五个原则不是教条，是**"代码为什么烂"的五种诊断**。每个原则配一个反例（烂）+ 正例（好），正例全部来自你的项目或你学过的源码。

## S — 单一职责原则（Single Responsibility）

一个类只因为**一个原因**变化。

```
❌ OrderService：下单 + 支付 + 发通知 + 导出报表 + 定时补偿
   → 任何需求都要改它 → 改动互相踩 → 测试范围爆炸 → 上线互相连坐

✓ 拆：OrderService(下单) / PayService(支付) / NotifyListener(MQ通知) / OrderExportJob
   每个类只有一个变化的理由
```

**诊断法**：描述这个类职责时如果用了"和""以及""还有"，就拆。mq-mall 把 `SeckillOrderTxWorker` 从消费者里拆出来，就是单一职责（事务边界独立变化）。

## O — 开闭原则（Open-Closed）

**对扩展开放，对修改关闭**：加新功能靠"加新类"，不靠"改老类"。

```
❌ 每新增一种支付方式就改 PayService 的 switch
✓ 定义 PaymentHandler 接口 → 每种支付一个实现 → Spring 注入 Map 自动收集
   新增微信支付 = 新增一个类（不改任何老代码）
```

你学过的实例：**线程池的 RejectedExecutionHandler**（加拒绝策略不用改线程池）、**Redis 的 maxmemory-policy 八种策略**、**Gateway 的谓词/过滤器体系**。策略模式就是 O 原则的实现手段。

## L — 里氏替换原则（Liskov）

子类必须能**无痕替换**父类——"继承"不是"是 a 种"的关系就别继承。

```
❌ 经典反例：正方形 extends 长方形（正方形把 setWidth/setHeight 绑死）
   → 所有接受长方形的代码遇到正方形全坏 → 语义上"正方形是长方形"≠里氏替换成立

✓ 里氏替换正面例： mq-mall 的 ThreadPoolExecutor 各种 RejectedExecutionHandler
   都满足"调用方无感知替换"——接口契约一致
```

**并发篇的联动**：ThreadLocal 泄漏、虚拟线程 pinning——本质都是"子类/变体违背了父类/宿主的隐含契约"。

## I — 接口隔离原则（Interface Segregation）

接口要小而专，不要"胖接口"逼实现方实现用不到的方法。

```
❌ interface UserService { register(); login(); export(); audit(); }
   → 实现类被迫写出 export(){ throw new UnsupportedOperationException(); }

✓ 拆成 RegisterApi / AuthApi / ExportApi …… 实现类只实现自己关心的
```

呼应 JUC：**AQS 的 tryAcquire/tryRelease 拆分**正是隔离思想的体现（独占与共享分开）。Java 8+ 接口默认方法缓解了胖接口问题，但**接口设计仍要克制**。

## D — 依赖倒置原则（Dependency Inversion）

**高层模块依赖抽象，不依赖具体实现**。"倒置"指依赖方向的倒置：业务 → 接口 ← 实现（实现依赖接口，而非反之）。

```java
// ❌ 高层直接依赖实现：换 Redis 为 Caffeine 要改业务代码
private RedisTemplate redis;                        // 绑死实现

// ✓ 依赖抽象：Spring 的 DI 天然帮你倒置（@Autowired 注入的是接口）
private final StringRedisTemplate redis;            // 还不够抽象（绑 Redis）
private final CacheClient cache;                    // 自定义 CacheClient 抽象 ✓
//   实现1 = RedisCacheClient（分布式） 实现2 = CaffeineCacheClient（本地）
//   —— 多级缓存（stage3/01）切换零成本
```

你项目里 D 原则的完整示范：**community 的 `CacheClient`/`MessageDedupService` 把 Redis 藏在接口后面**——教学版用内存 Map、生产换 Redis，业务代码零改动（README 的"教学简化 vs 生产"表就是这么设计的）。

## 实战组合：秒杀项目的"模式全家桶"复盘

回看 mq-mall 秒杀模块，一个业务里叠了多少模式（全是有用途的，不是炫技）：

| 代码 | 模式 | 为什么 |
|------|------|--------|
| `SeckillService` Lua 预扣 | 策略（分位扣减可换实现） | 库存策略可切换 |
| `SeckillOrderTxWorker` 独立事务 | 单一职责 + 代理失效修复 | 事务边界独立变化 |
| `ReliableMessagePublisher` | 门面 | 藏本地消息表+序列化+Confirm 复杂度 |
| `Confirm/Return` 回调 | 观察者 | 发送结果异步通知 |
| `IdempotentConsumer` 两层防线 | 责任链思想 | Redis→DB 两道闸 |
| `RetryTemplate`/补偿 job | 策略 + 模板 | 退避算法可换 |

## 反面：什么时候**不要**用设计模式

> [!CAUTION]
> 过度设计的三个信号：
> 1. 为"将来可能的扩展"建抽象层，而那个将来 90% 不会来（YAGNI：You Aren't Gonna Need It）
> 2. 一个方法里的 if-else 只有 2~3 个稳定分支就上策略模式——间接层成本超过收益
> 3. 为了用模式而用模式（简历驱动开发）

**判断标准**：模式是为了**管理已经真实存在的变化**，不是预支未来。先写直白代码，变化真的出现了、重复出现了，再重构为模式——**重构的手感比设计的先见更可靠**。

## 面试问法

**Q: 结合你的项目谈谈用过的设计模式。**
A: 按"创建/结构/行为"三组各举实例——创建：Redis 队列声明 QueueBuilder 建造者、Spring 单例注册表；结构：ReliableMessagePublisher 门面（藏本地消息表复杂度）、AOP 代理（事务）、RedisTemplate 装饰；行为：拒绝策略与负载均衡的策略、AQS 模板方法（tryAcquire 钩子）、MQ fanout 观察者。收尾强调：模式服务于"管理变化"，按 YAGNI 原则防止过度设计。

---

**设计模式模块完成** ✅。进入 [07-网络基础](../07-网络基础/README.md)。
