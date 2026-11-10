# 05 · String 与对象相等性：每天都在用、面试天天挖

```java
String a = "abc";
String b = new String("abc");
System.out.println(a == b);          // false —— 但为什么？涉及常量池
System.out.println(a.equals(b));     // true
Integer x = 127, y = 127;
System.out.println(x == y);          // true —— 换成 128 就是 false！
```

这四个输出是**初级面试的必考题**，也是线上代码真实踩过的坑（用 `==` 比较包装类做业务判断，环境一换就偶发 bug）。

## 一、String 为什么设计成不可变

| 不可变的好处 | 说明 | 你已学的连接 |
|-------------|------|-------------|
| **常量池共享** | 字符串大量重复，不可变才能安全共享一个实例 | JVM 篇 01 运行时数据区 |
| **hashCode 可缓存** | String 首次计算后缓存进对象内部——**HashMap key 大多用 String**，重复 get 不重算 | 集合篇 01 |
| **安全** | 方法/网络参数若可变，校验后被改走就完了（路径、SQL、类名加载） | 类加载篇 02（自定义类加载器找名字） |
| **线程安全** | 天然 final，多线程共享无锁 | 并发篇 03 不可变对象策略 |

**底层结构（Java 9+）**：`byte[] value` + `int hash` 缓存字段—— latin1 字符省一半内存（压缩字符串）。

## 二、常量池：== 什么时候坑你

```java
String a = "abc";                       // 字面量 → 编译期进常量池
String b = "abc";
System.out.println(a == b);             // ✅ true：同一个池对象
String c = new String("abc");           // new 一定堆上开新对象（池里已有的"abc"复用）
System.out.println(a == c);             // ❌ false
String d = "ab" + "c";                  // 编译期常量折叠 → 直接变 "abc" 入池
System.out.println(a == d);             // ✅ true
String e = "ab";  String f = e + "c";   // 运行期拼接 → StringBuilder → 新堆对象
System.out.println(a == f);             // ❌ false！
String g = f.intern();                  // intern：堆对象入池/复用池内引用
System.out.println(a == g);             // ✅ true
```

**结论一行**：字符串比较**永远用 equals**，`==` 只在"我明确想比较引用同一性"时用——这不是风格问题，是正确性问题。

**经典追问：`new String("abc")` 创建几个对象？** 最多两个：常量池一个（若无）+ 堆上一个。答题别背"一个/两个"——说出"看池里有没有"才是真懂。

## 三、循环拼接：性能不是玄学

```java
// ❌ 循环内 + 拼接：每次循环 new 一个 StringBuilder + toString 新串，O(n²)
String s = "";
for (String p : parts) s += p;

// ✅ 循环外一个 StringBuilder，O(n)
StringBuilder sb = new StringBuilder();
for (String p : parts) sb.append(p);
```

单行 `"a" + b + "c"` 编译器**自动优化成 StringBuilder**，不用手写——坏的是**循环/递归里的拼接**（每轮新建）。另外 StringBuilder 线程不安全（并发篇 02：无锁的代价），多线程拼接场景用 StringBuffer（synchronized）或各自拼完合并。

## 四、equals 与 hashCode 契约（HashMap 的命门）

```java
// 坑：只重写 equals 不重写 hashCode
class User {
    String id;
    public boolean equals(Object o) { return o instanceof User u && u.id.equals(id); }
    // ❌ 没写 hashCode —— 默认返回对象地址哈希
}
Set<User> set = new HashSet<>();
set.add(new User("u1"));
set.contains(new User("u1"));    // false！—— 写进去的找不回来了
```

**契约规则**（集合篇 01 的原理落位）：
1. `equals` 相等 → `hashCode` **必须**相等（否则 HashMap 定位到不同桶，永远找不到）
2. `hashCode` 相等 → equals 不必相等（哈希冲突正常）
3. 哈希定位是**两段式**：先 hashCode 找桶 → 桶内 equals 精确比对——两者各管一段

**为什么必须同时重写**：record（Java 16+）/Lombok `@EqualsAndHashCode` 帮你自动生成两者——手写 POJO 用 IDE 生成，别漏勾 hashCode。

## 五、包装类缓存：Integer == 的 128 分界线

```java
Integer a = 127, b = 127;
a == b        // ✅ true —— 自动装箱走 IntegerCache，[-128,127] 复用同一个对象
Integer c = 128, d = 128;
c == d        // ❌ false —— 超缓存范围，各自 new
```

- **自动装箱 = `Integer.valueOf()`**（不是 new）——小值命中缓存池，大值真开对象
- 业务比较包装类**一律 equals 或先拆箱**：`a.equals(b)` / `a.intValue() == b.intValue()`
- 性能细节：高频装箱拆箱（循环里 `Long` 累加）是真开销——用原始类型 `long` 累加（Effective Java 经典条目，阿里规约明令禁止循环装箱）

## 面试问法

**Q: String 为什么不可变？**
A: 四个收益：常量池可安全共享（省内存）、hashCode 可缓存（HashMap key 高频查）、安全（校验后内容不会变，类加载/文件路径都靠它）、线程安全。底层 byte[] + 缓存 hash，9+ 压缩字符串。不可变带来的代价是每次"修改"都是新对象——所以循环拼接必须 StringBuilder。

**Q: hashCode 不重写会怎样？**
A: 契约破坏：equals 相等的两个对象 hashCode 不同 → HashSet/HashMap 用 hashCode 定桶，第二个对象被定位到不同桶 → contains/get 找不到已放进去的"相等"对象。哈希定桶+equals 精确比对是两段式，二者必须满足"equals 相等则 hashCode 必等"。

---

回 [00 模块 README](README.md) · 相关：[02-泛型反射与注解](02-泛型反射与注解.md)（equals 的 instanceof 用法）、[01-集合框架](01-集合框架.md)（HashMap 两段式定位）。
