package com.bigcenter.lab.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池七参数演示（教程: java-core/01-并发编程/05 线程池）：
 * corePoolSize / maximumPoolSize / keepAliveTime / unit / workQueue / threadFactory / handler
 * <p>关键现象：先填满 core → 队列 → 再扩到 max → 拒绝策略。顺序错了（先扩 max）是高频面试误区。</p>
 */
public class ThreadPoolSevenParamsDemo {
    private static final Logger log = LoggerFactory.getLogger(ThreadPoolSevenParamsDemo.class);

    public static void main(String[] args) throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2,                                      // corePoolSize：常驻线程
                4,                                      // maximumPoolSize：峰值线程
                10, TimeUnit.MILLISECONDS,              // 非核心线程空闲存活时间
                new ArrayBlockingQueue<>(2),            // workQueue：容量 2
                r -> new Thread(r, "lab-" + POOL_SEQ.incrementAndGet()),   // threadFactory：命名便于排障
                new ThreadPoolExecutor.AbortPolicy()); // handler：拒绝策略（默认 Abort 抛异常）

        // 提交 8 个长任务，观察执行顺序：2 个进 core、2 个进队列、2 个扩 max、第 7/8 个被拒
        for (int i = 1; i <= 8; i++) {
            final int no = i;
            try {
                pool.execute(() -> {
                    log.info("任务 {} 由 {} 执行", no, Thread.currentThread().getName());
                    try { Thread.sleep(200); } catch (InterruptedException ignored) { }
                });
                log.info("任务 {} 已提交：活跃={} 队列={} 完成={}", no,
                        pool.getActiveCount(), pool.getQueue().size(), pool.getCompletedTaskCount());
            } catch (RejectedExecutionException e) {
                log.warn("任务 {} 被拒绝（AbortPolicy）——拒绝策略生效", no);
            }
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        log.info("结束：提交顺序=core→queue→max→reject（不是 core→max→queue）");
    }

    private static final java.util.concurrent.atomic.AtomicInteger POOL_SEQ =
            new java.util.concurrent.atomic.AtomicInteger();
}
