# 03 · Stream 与 Lambda：声明式集合处理

你项目里已经到处在用：`posts.stream().map(...).toList()`、`removeIf(...)`、`sorted(Comparator...)`。本篇系统化 + 讲清性能真相。

## Lambda 与函数式接口：方法变成了"值"

```java
// 函数式接口 = 只有一个抽象方法的接口（@FunctionalInterface 标记）
Runnable r = () -> System.out.println("跑");            // 无参无返回
Comparator<Integer> c = (a, b) -> a - b;                // 两参一返回
Supplier<String> s = () -> "供给";                       // 无参有返回（生产者）
Function<String, Integer> f = String::length;           // 一进一出（转换）
Predicate<String> p = str -> str.isEmpty();             // 进一出 boolean（判断）
Consumer<String> c2 = System.out::println;              // 进无出（消费）
// 方法引用：System.out::println 等价于 x -> System.out.println(x) —— 更简洁
```

四大内置接口（Supplier 生产 / Function 转换 / Predicate 判断 / Consumer 消费）——**记住这四个角色，Stream 的所有 API 都是它们的组合**。

## Stream：声明式集合处理

```java
// 命令式（for 循环）：说"怎么做"
List<String> names = new ArrayList<>();
for (User u : users) {
    if (u.getAge() >= 18) names.add(u.getName().toUpperCase());
}

// 声明式（Stream）：说"要什么"
List<String> names = users.stream()
        .filter(u -> u.getAge() >= 18)
        .map(u -> u.getName().toUpperCase())
        .toList();
```

常用操作全景：

```java
users.stream()
     .filter(u -> u.isActive())                    // 过滤
     .map(User::getName)                           // 转换
     .distinct()                                   // 去重
     .sorted(Comparator.comparing(User::getScore).reversed())  // 排序
     .limit(10)                                    // 前 10 个
     .collect(Collectors.toList());

// 聚合
long count = stream.count();
Optional<User> max = stream.max(Comparator.comparing(User::getScore));
int total = stream.mapToInt(User::getAge).sum();   // 原始类型流（避免装箱）

// 分组（最强大招：SQL group by 的内存版）
Map<String, List<User>> byCity = users.stream()
        .collect(Collectors.groupingBy(User::getCity));
Map<Boolean, List<User>> parts = users.stream()
        .collect(Collectors.partitioningBy(User::isActive));   // 二分

// 拼接
String joined = names.stream().collect(Collectors.joining(", ", "[", "]"));
```

## 关键认知：惰性求值与一次消费

```java
users.stream()
     .peek(u -> System.out.println("经过: " + u))    // peek 用来观察
     .filter(u -> u.isActive())
     .count();                                       // 终止操作才触发执行
// 没有终止操作 → 中间操作一行都不会执行（惰性）

// 优化：filter 在 map 前（少做无用转换）
users.stream().filter(...).map(...)   // ✓ 先过滤再转换
// Stream 只能消费一次 → 复用会抛 IllegalStateException（重新 stream() 即可）
```

## 性能真相：Stream 不一定比 for 快

```
简单遍历：    for 循环更快（无对象开销）——数量小、逻辑直白就用 for
复杂流水线：  Stream 可读性碾压（filter/map/sorted 一眼懂意图）
大数据量：    stream.parallel() 并行流（ForkJoinPool）——但小数据并行反而慢（任务拆分开销）
             且 parallel 在虚拟线程/容器里要小心（默认用公共池）
结论：可读性优先选 Stream；热点循环（百万级）用 for；并行流先压测
```

## Optional：消灭 NPE 的容器

```java
// ❌ 链式 NPE：user.getAddress().getCity()（address 为 null 就炸）
// ✓ Optional
Optional.ofNullable(user)
        .map(User::getAddress)
        .map(Address::getCity)
        .orElse("默认城市");
Optional.ofNullable(user).orElseThrow(() -> new BizException("用户不存在"));
// 反模式（别把 Optional 当字段/参数用——它是"返回值可能为空"的表达）
```

项目实例：community 的 `postDao.find(id).orElseThrow(() -> new BizException("帖子不存在"))` 就是 Optional 的标准用法（dao 返回 Optional，调用方决定空值语义）。

## 面试问法

**Q: Stream 的中间操作和终止操作？惰性求值？**
A: 中间操作（filter/map/sorted）返回 Stream 支持链式、**惰性**（无终止操作不执行）；终止操作（collect/forEach/count/reduce）触发整条流水线。一次消费，复用需重新 stream()。优化：filter 放 map 前；并行流小数据慎用。

**Q: Lambda 的原理？**
A: 函数式接口（单抽象方法）+ Lambda 表达式 → 编译期生成 invokedynamic 指令，运行时由 LambdaMetafactory 动态生成实现类（非匿名内部类语法糖）——方法引用/捕获变量的行为由此而来。捕获的局部变量需 effectively final（值拷贝语义）。

---

**下一篇**：[04-异常体系与最佳实践](04-异常体系与最佳实践.md)。
