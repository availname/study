package com.bigcenter.lab.basics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * String 与对象相等性演示（教程: java-core/00-Java语言基石/05-String与对象相等性）：
 * == 比引用、equals 比内容、intern 池、不可变与哈希缓存；
 * equals/hashCode 契约（HashMap key 的前提）。
 */
public class StringEqualityDemo {
    private static final Logger log = LoggerFactory.getLogger(StringEqualityDemo.class);

    record Money(long fen) {
        // record 自动生成 equals/hashCode（值语义）——手写类必须成对重写
    }

    static class Broken {
        long fen;
        Broken(long fen) { this.fen = fen; }
        // 只重写 equals 不重写 hashCode → HashMap 里找不到自己（契约破坏经典）
        @Override public boolean equals(Object o) { return o instanceof Broken b && b.fen == fen; }
    }

    public static void main(String[] args) {
        String a = "hello";
        String b = "hel" + "lo";            // 编译期常量折叠 → 同一个字面量
        String c = new String("hello");     // 显式新对象
        String d = ("hel" + args0()).intern();  // 运行时拼接 → 池化后归一

        log.info("① a==b：{}（常量折叠同一池条目）；a==c：{}（new 必新对象）；a==d：{}（intern 归一）",
                a == b, a == c, a == d);
        log.info("② a.equals(c)：{}（内容相等——比较一律 equals，== 只查引用）", a.equals(c));

        // ③ equals/hashCode 契约：只重 equals 的类在 HashMap 里"失踪"
        Broken x = new Broken(100);
        java.util.Map<Broken, String> bad = new java.util.HashMap<>();
        bad.put(x, "值");
        log.info("③ Broken 只重 equals：bad.containsKey(new Broken(100)) = {}（hashCode 未重写→哈希桶不同）",
                bad.containsKey(new Broken(100)));

        Money m1 = new Money(100);
        java.util.Map<Money, String> good = new java.util.HashMap<>();
        good.put(m1, "值");
        log.info("④ record 值语义：good.containsKey(new Money(100)) = {}（equals+hashCode 自动成对）",
                good.containsKey(new Money(100)));
        log.info("⑤ 不可变好处：String 哈希缓存（hashCode 只算一次）+ 天然线程安全——这是它能当 HashMap key 的原因");
    }

    static String args0() { return "lo"; }
}
