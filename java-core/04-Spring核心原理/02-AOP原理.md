# 02 · AOP 原理：动态代理与织入

AOP（面向切面编程）= 把"所有方法都要做的事"（日志、事务、权限）从业务代码里抽出来，由容器**在运行时动态织入**。实现手段只有一个词：**动态代理**。

## 代理的两个实现（面试必考对比）

### JDK 动态代理：基于接口

```java
// 运行时生成一个"实现了目标接口"的代理类
Object proxy = Proxy.newProxyInstance(
        loader, new Class[]{UserService.class},      // 必须有接口！
        (p, method, args) -> {
            before();                                 // 织入的增强逻辑
            Object result = method.invoke(target, args);
            after(result);
            return result;
        });
```

### CGLib：基于子类继承

```java
// 运行时生成"目标类的子类"，重写方法织入逻辑（无接口也行）
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(UserService.class);           // 子类 → final 类不行！
enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
    before();
    Object result = proxy.invokeSuper(obj, args);
    after(result);
    return result;
});
```

| | JDK 动态代理 | CGLib |
|--|------------|-------|
| 基础 | 反射 + 接口 | 字节码生成子类 |
| 要求 | 目标必须实现接口 | 类和方法**不能是 final** |
| 性能 | JDK8+ 两者接近 | 生成稍慢、执行稍快 |

**Spring Boot 2.x 起默认全部用 CGLib**（`proxyTargetClass=true`）——不再纠结"有没有接口"，行为统一。

## 五种通知与执行顺序

```java
@Aspect
@Component
public class LogAspect {
    @Pointcut("execution(* com.example.community..*Controller.*(..))")
    public void controller() {}

    @Around("controller()")                       // 包裹全程，最强大（可改参数/吞异常/多次执行）
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long t0 = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();        // 放行到目标方法
            log.info("耗时 {}ms", ...);
            return result;
        } catch (Throwable e) {
            log.error("异常", e); throw e;
        }
    }

    @Before("controller()")  public void before() { }     // 方法前
    @After("controller()")   public void after()  { }     // finally 语义（成功失败都走）
    @AfterReturning(...)     public void ret()    { }     // 正常返回后
    @AfterThrowing(...)      public void thr()    { }     // 抛异常后
}
```

正常流：`Around 前 → @Before → 目标方法 → @AfterReturning → @After → Around 后`
异常流：`Around 前 → @Before → 抛异常 → @AfterThrowing → @After → 异常继续抛`

## 代理失效：所有坑的共同根因（★最重要一节）

**增强逻辑织入在代理对象里，目标方法内部用 `this` 调用自己的其它方法时，走的是原始对象、绕过了代理**——切面/事务全部失效。

```java
@Service
public class OrderService {
    public void create() {
        this.save();          // ❌ this.save 走原始对象 → @Transactional(save) 失效！
    }

    @Transactional
    public void save() { ... }
}
```

**失效场景全家福（同一根因 + 变体）**：

| 场景 | 失效原因 |
|------|---------|
| ① 自调用 `this.method()` | 没走代理（上面例子）|
| ② 方法非 public（CGLib 也不增强 protected/包级） | 代理子类无法覆盖非 public |
| ③ `final` 方法 | CGLib 子类无法重写 final |
| ④ `static` 方法 | 属于类不属于实例，无 this 可代理 |
| ⑤ 对象是 `new` 出来的 | 不在容器里，根本没有代理 |

**修复方案**：

```java
// 修复1：注入自己（拿到的是代理）
@Autowired @Lazy private OrderService self;
public void create() { self.save(); }        // 走代理 ✓

// 修复2：从容器拿代理
applicationContext.getBean(OrderService.class).save();

// 修复3（结构性最优）：把被代理方法挪到另一个 Bean（mq-mall 的 SeckillOrderTxWorker 就是这么拆的）
```

mq-mall 拆出 `SeckillOrderTxWorker`、community 用 `@Lazy` 注入——**你在两个项目里已经用过修复 1 和 3 的两种姿势**。

## 切点表达式速查

```java
execution(* com.example.community.service.*.*(..))
//          返回值  包            类  方法  任意参数
// 任意返回值 = * ；任意参数 = (..)；类下所有 = *.*

@within(org.springframework.transaction.annotation.Transactional)   // 标注了注解的类
@annotation(Transactional)                                           // 标注了注解的方法
```

## 面试问法

**Q: AOP 的实现原理？JDK 和 CGLib 的区别？**
A: 运行时动态代理织入。JDK 动态代理基于接口（Proxy+InvocationHandler 反射调用）；CGLib 生成目标子类重写方法（final 类/方法不行）。Boot 2.x 起默认 CGLib 统一行为。通知顺序：Around 包裹 Before/AfterReturning/AfterThrowing/After。

**Q: @Transactional/AOP 什么情况下失效？**
A: 万变不离其宗——**调用没走代理**：同类自调用 this、非 public、final/static 方法、new 出来的对象。修复：注入自己 @Lazy、getBean、或方法挪到别的 Bean（本质都是"让调用走代理对象"）。另注意 CGLib 场景 @Transactional 标在接口上不生效。

---

**下一篇**：[03-事务原理与失效八连](03-事务原理与失效八连.md)——把 @Transactional 的机制与八个坑一次讲全。
