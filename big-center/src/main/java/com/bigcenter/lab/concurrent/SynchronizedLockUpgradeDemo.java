package com.bigcenter.lab.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 锁升级演示：无锁 → 偏向锁 → 轻量级锁 → 重量级锁（教程: java-core/01-并发编程/02-synchronized与锁原理）
 * <p>运行：观察单线程重复加锁（偏向）→ 两线程交替（轻量级自旋）→ 竞争加剧（重量级）。
 * JDK15+ 已默认停用偏向锁（-XX:+UseBiasedLocking 移除），输出以"轻量级→重量级"两段为主——
 * 这本身就是教学点：技术会演进，面试讲"演进史+现状"才加分。</p>
 */
public class SynchronizedLockUpgradeDemo {
    private static final Logger log = LoggerFactory.getLogger(SynchronizedLockUpgradeDemo.class);

    static final Object LOCK = new Object();
    static int counter = 0;

    public static void main(String[] args) throws Exception {
        // 阶段一：单线程重复加锁——锁只被一个线程反复获取（历史上是偏向锁场景；现代 JDK 直接轻量级）
        log.info("阶段一：单线程无竞争加锁 100 万次");
        for (int i = 0; i < 1_000_000; i++) {
            synchronized (LOCK) { counter++; }
        }
        log.info("counter = {}（无竞争，无重量级开销）", counter);

        // 阶段二：两线程交替——轻量级锁：CAS 自旋获取
        log.info("阶段二：两线程交替竞争（轻量级锁：CAS 自旋）");
        Thread t1 = new Thread(SynchronizedLockUpgradeDemo::spinSection, "T1");
        Thread t2 = new Thread(SynchronizedLockUpgradeDemo::spinSection, "T2");
        t1.start(); t2.start();
        t1.join(); t2.join();
        log.info("counter = {}（自旋期间未膨胀为重量级）", counter);

        // 阶段三：多线程长临界区——自旋失败次数超阈值 → 膨胀为重量级锁（操作系统 mutex）
        log.info("阶段三：4 线程长临界区竞争（膨胀为重量级锁）");
        Thread[] heavy = new Thread[4];
        for (int i = 0; i < 4; i++) {
            heavy[i] = new Thread(SynchronizedLockUpgradeDemo::longSection, "H" + i);
            heavy[i].start();
        }
        for (Thread t : heavy) t.join();
        log.info("结束：观察点=临界区长度决定膨胀时机；现代 JDK 起点已是轻量级");
    }

    private static void spinSection() {
        for (int i = 0; i < 100_000; i++) {
            synchronized (LOCK) { counter++; }      // 极短临界区：CAS 自旋划算
        }
    }

    private static void longSection() {
        for (int i = 0; i < 5; i++) {
            synchronized (LOCK) {
                try { Thread.sleep(1); } catch (InterruptedException ignored) { }   // 长临界区：自旋不划算
                counter++;
            }
        }
    }
}
