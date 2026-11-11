package com.bigcenter.lab.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * CHM 与 ThreadLocal 泄漏演示（教程: java-core/01-并发编程/07-并发容器与ThreadLocal）：
 * ① CHM 原子复合操作（computeIfAbsent——put+get 分开就丢原子性）；
 * ② ThreadLocal 泄漏三要素：线程池线程不死 + value 强引用 + 不调 remove。
 */
public class ChmAndThreadLocalDemo {
    private static final Logger log = LoggerFactory.getLogger(ChmAndThreadLocalDemo.class);

    static final ThreadLocal<byte[]> LEAK = new ThreadLocal<>();
    static final ConcurrentHashMap<String, Integer> MAP = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        // ① CHM 复合原子：统计出现次数——错误写法 map.put(k, map.get(k)+1) 在并发下丢更新
        ExecutorService pool = Executors.newFixedThreadPool(4);
        for (int t = 0; t < 4; t++) {
            pool.execute(() -> {
                for (int i = 0; i < 25_000; i++) {
                    MAP.computeIfAbsent("k", k -> 0);
                    MAP.computeIfPresent("k", (k, v) -> v + 1);     // 一条原子链
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        log.info("① CHM computeIfPresent 原子累加：{}（期望 100000；用 get+put 会丢更新）", MAP.get("k"));

        // ② ThreadLocal 泄漏演示：线程池线程存活，value 不 remove 就一直挂着
        ExecutorService leakPool = Executors.newFixedThreadPool(1);     // 线程不死
        for (int i = 0; i < 3; i++) {
            final int round = i;
            leakPool.execute(() -> {
                LEAK.set(new byte[1024 * 1024]);       // 1MB/轮——线程复用则上一轮 value 还挂着
                log.info("② 轮次 {} 设置 1MB（若上一轮未 remove，泄漏累积）", round);
                LEAK.remove();                          // 纪律：finally 里 remove（与 UserContext.clear 同款）
            });
        }
        leakPool.shutdown();
        leakPool.awaitTermination(3, TimeUnit.SECONDS);
        log.info("② 结论：线程池 + ThreadLocal 必须 set/remove 成对——big-center 拦截器 afterCompletion 就是这条纪律");
    }
}
