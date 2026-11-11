package com.bigcenter.lab;

/**
 * java-core-lab 实验室入口（big-center-PLAN 第三节 C）：
 * 纯 JDK main 示例集，每个类带教程锚点注释，运行结果用日志打印关键现象。
 * 各示例独立运行（IDE 右键 Run 或 mvn exec），互不依赖。
 *
 * <pre>
 * concurrent/  并发八篇：线程状态/锁升级/volatile+DCL/CAS+ABA/线程池七参数/AQS 自实现锁/CHM+ThreadLocal/虚拟线程
 * basics/      基石五篇：集合(HashMap 扩容)/泛型反射/Stream/异常/String 相等性
 * patterns/    设计模式五篇：单例五种/代理 JDK+CGLib/策略+模板/责任链/状态机
 * </pre>
 */
public final class LabRunner {
    private LabRunner() {}

    public static void main(String[] args) {
        System.out.println("java-core-lab：每个示例类自带 main，请单独运行。目录：");
        System.out.println("  com.bigcenter.lab.concurrent.*   // 教程: java-core/01-并发编程 01~08");
        System.out.println("  com.bigcenter.lab.basics.*       // 教程: java-core/00-Java语言基石 01~05");
        System.out.println("  com.bigcenter.lab.patterns.*     // 教程: java-core/06-设计模式 01~05");
    }
}
