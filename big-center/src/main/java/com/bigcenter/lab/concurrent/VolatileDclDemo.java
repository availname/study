package com.bigcenter.lab.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * volatile 与 DCL 单例演示（教程: java-core/01-并发编程/03-volatile与JMM内存模型）：
 * volatile 两大语义：可见性（写立即刷主存）+ 禁止指令重排（DCL 必需——没有它可能拿到"半初始化对象"）。
 */
public class VolatileDclDemo {
    private static final Logger log = LoggerFactory.getLogger(VolatileDclDemo.class);

    /** 必须 volatile：new 分三步（分配内存→初始化→引用赋值），③ 与 ② 可能重排 */
    private static volatile VolatileDclDemo instance;
    private final int state;

    private VolatileDclDemo() {
        state = 42;
    }

    /** 双重检查锁（DCL）：两次判空 + synchronized 只锁创建路径 */
    public static VolatileDclDemo getInstance() {
        if (instance == null) {                       // 第一次判空：避免每次都进锁
            synchronized (VolatileDclDemo.class) {
                if (instance == null) {               // 第二次判空：可能被别的线程抢先
                    instance = new VolatileDclDemo();
                }
            }
        }
        return instance;
    }

    public static void main(String[] args) throws Exception {
        // 可见性：普通字段跨线程可能永远读不到更新；volatile 保证读端看到最新写
        long deadline = System.currentTimeMillis() + 500;
        Thread writer = new Thread(() -> {
            try { Thread.sleep(50); } catch (InterruptedException ignored) { }
            instance = new VolatileDclDemo();          // volatile 写：立即对读线程可见
        });
        writer.start();

        VolatileDclDemo seen = null;
        while (System.currentTimeMillis() < deadline) {
            seen = VolatileDclDemo.getInstance();
            if (seen.state == 42) break;               // volatile 读：看到完整初始化（state=42）
        }
        log.info("DCL 获取成功：instance={} state={}（半初始化对象 state=0 的风险被 volatile 禁重排挡住）",
                seen != null, seen == null ? -1 : seen.state);
        log.info("两次 getInstance 同一实例：{}", VolatileDclDemo.getInstance() == seen);
        writer.join();
    }
}
