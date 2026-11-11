package com.bigcenter.lab.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

/**
 * 线程状态与虚拟线程衔接（教程: java-core/01-并发编程/01-线程基础与生命周期、08-并发实战与虚拟线程衔接）：
 * 六状态 NEW→RUNNABLE→(BLOCKED/WAITING/TIMED_WAITING)→TERMINATED；
 * 虚拟线程（JDK 21）：IO 任务百万并发——载体线程阻塞时卸载（unmount），不占 OS 线程；本项目 J17 用 cached 池对照演示。
 */
public class ThreadLifecycleAndVirtualThreadDemo {
    private static final Logger log = LoggerFactory.getLogger(ThreadLifecycleAndVirtualThreadDemo.class);
    private static final Object MONITOR = new Object();

    public static void main(String[] args) throws Exception {
        // ① 六状态观察
        Thread t = new Thread(() -> {
            synchronized (MONITOR) {
                try { MONITOR.wait(50); } catch (InterruptedException ignored) { }   // TIMED_WAITING
            }
        }, "state-observer");
        log.info("① 刚创建：{}", t.getState());        // NEW
        t.start();
        log.info("已启动：{}", t.getState());           // RUNNABLE
        Thread.sleep(10);
        log.info("wait 中：{}", t.getState());          // TIMED_WAITING
        t.join();
        log.info("结束后：{}", t.getState());           // TERMINATED

        // BLOCKED 观察：主线程先拿锁，另一线程抢锁进入 BLOCKED
        synchronized (MONITOR) {
            Thread contender = new Thread(() -> { synchronized (MONITOR) { } }, "contender");
            contender.start();
            Thread.sleep(50);
            log.info("② 抢不到锁：{}", contender.getState());   // BLOCKED
            contender.join();
        }

        // ③ 虚拟线程衔接（JDK 21 才有 Executors.newVirtualThreadPerTaskExecutor；本项目 J17，
        //    用 cached 线程池演示同等"任务多线程少"的 IO 并发形态——升级 21 后一行换成虚拟线程执行器）
        long start = System.currentTimeMillis();
        var executor = Executors.newCachedThreadPool();
        for (int i = 0; i < 2_000; i++) {
            executor.submit(() -> {
                Thread.sleep(10);                       // 模拟 IO：线程池阻塞等待（虚拟线程在此卸载不占 OS 线程）
                return null;
            });
        }
        executor.shutdown();
        executor.awaitTermination(1, java.util.concurrent.TimeUnit.MINUTES);
        log.info("③ 2000 个 IO 任务各睡 10ms：总耗时 {}ms（cached 池动态扩线程；JDK21 虚拟线程可扩到百万级）",
                System.currentTimeMillis() - start);
        log.info("要点：虚拟线程适合 IO 密集（阻塞即卸载）；CPU 密集仍用平台线程池——big-center 短链跳转这类 IO 场景是典型受益者");
    }
}
