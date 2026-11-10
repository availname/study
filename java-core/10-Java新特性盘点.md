# 10 · Java 新特性盘点（8 → 21）

教程代码里已经用了不少新特性（record/var/文本块/虚拟线程），这里系统盘点——面试"你了解 Java 新特性吗"必考。

| 版本 | 年份 | 重点特性 |
|------|------|---------|
| 8 | 2014 | Lambda、Stream、Optional、接口默认方法、新日期 API |
| 9-11 | 2017-2018 | 模块化、**var**、**HttpClient**、集合工厂方法 `List.of()` |
| 14-16 | 2020-2021 | **switch 表达式**、**文本块 """**、instanceof 模式匹配（预览）|
| **17 LTS** | 2021 | sealed 密封类、Record（16 正式） |
| **21 LTS** | 2023 | **虚拟线程**（stage6/01）、Record 模式匹配、序列化集合 |

## ① Record（record 类——learn-center 的 Course/Question 已用）

```java
public record Course(Long id, String title, String teacher, Integer price) {}
// 自动生成：全参构造 / getXxx() / equals / hashCode / toString
// 不可变（无 setter）→ 天然并发安全（并发篇：能不可变就不共享）
```

- 字段访问是 `course.title()` 不是 `getTitle()`——**注意 MyBatis-Plus/Jackson 需要 getter 时补上**（mq-mall 教程里的 Course 就补了 getter）
- 适用：值对象/DTO/消息体；不适合：有可变状态的实体（JPA/MP 实体保持 class）

## ② sealed 密封类 + 模式匹配（17/21）

```java
public sealed interface Payment permits Alipay, WechatPay {}
public record Alipay(...) implements Payment {}
public record WechatPay(...) implements Payment {}

// 模式匹配 switch：编译器保证穷举（新增子类时编译报错提醒——防漏处理）
String desc = switch (payment) {
    case Alipay a -> "支付宝";
    case WechatPay w -> "微信";
};   // 无 default 也编译通过（sealed 保证穷举）
```

## ③ switch 表达式（14）：箭头语法 + 返回值

```java
// 旧：break 加赋值；新：箭头无穿透 + 可作表达式
String name = switch (status) {
    case 10 -> "待支付";
    case 20 -> "已支付";
    default -> "未知";
};
```

## ④ 文本块（15）：多行字符串

```java
String json = """
        {
          "name": "张三"
        }
        """;    // 缩进智能处理——SQL/JSON 可读性大幅提升
```

## ⑤ var（10）：局部变量类型推断

```java
var list = new ArrayList<User>();     // 编译器推断，仅局部变量可用
```

## ⑥ instanceof 模式匹配（16）

```java
// 旧：if (o instanceof String) { String s = (String) o; ... }
if (o instanceof String s) {          // 判断+转型+声明一步完成
    System.out.println(s.length());
}
```

## 面试问法

**Q: Java 8 之后有哪些重要新特性？**
A: 按"开发效率→并发革命→类型系统"三线——效率：Lambda/Stream/var/文本块/switch 表达式；并发：**虚拟线程 21**（并发革命，stage6/01）；类型系统：**Record 不可变值对象**、sealed+模式匹配（编译器穷举检查）。生产建议：新项目全用 17/21 LTS，Record 用于 DTO/消息体，虚拟线程用于 IO 密集。

---

**下一篇**：[11-分布式系统基础](11-分布式系统基础.md)。
