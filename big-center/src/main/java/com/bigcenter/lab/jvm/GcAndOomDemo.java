package com.bigcenter.lab.jvm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * JVM 内存区域与 GC 观察演示（教程: java-core/02-JVM虚拟机）：
 * 用 ManagementFactory 读运行时数据（生产用 Arthas/JConsole）；OOM 只演示可安全恢复的堆溢出。
 * <p>GC 日志参数模板（面试背骨架）：
 * -Xms1g -Xmx1g -Xmn512m -XX:+UseG1GC -Xlog:gc*（JDK9+ 统一日志）
 * -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/dump</p>
 */
public class GcAndOomDemo {
    private static final Logger log = LoggerFactory.getLogger(GcAndOomDemo.class);

    public static void main(String[] args) {
        // ① 运行时内存区：堆 = Eden + 2*Survivor + Old
        var mem = ManagementFactory.getMemoryMXBean();
        log.info("① 堆初始 {} / 最大 {}；非堆 {}",
                mem.getHeapMemoryUsage().getInit() / 1024 / 1024 + "MB",
                mem.getHeapMemoryUsage().getMax() / 1024 / 1024 + "MB",
                mem.getNonHeapMemoryUsage().getUsed() / 1024 / 1024 + "MB");

        // ② 一次"制造 Young GC"的过程：短命对象填满 Eden
        long gcBefore = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> gc.getCollectionCount()).sum();
        for (int i = 0; i < 1_000_000; i++) {
            byte[] shortLived = new byte[64];       // 短命对象：Eden 分配，朝生夕死
            if (shortLived.length < 0) log.info("never");   // 防 JIT 消除整个分配（教学观察）
        }
        long gcAfter = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> gc.getCollectionCount()).sum();
        log.info("② 分配 100 万小对象：GC 总次数 {} → {}（Young GC 分代假说的'朝生夕死'）", gcBefore, gcAfter);

        // ③ 元空间意识：反射/动态代理生成类会占 Metaspace（CGLib 大量代理类泄漏场景）
        log.info("③ 已加载类 {} 个（动态代理/CGLib 泄漏盯这里）",
                ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());

        // ④ 可恢复的堆 OOM 演示（-Xmx 下限内安全进行）
        try {
            List<byte[]> hold = new ArrayList<>();
            long total = 0;
            try {
                while (true) {
                    hold.add(new byte[10 * 1024 * 1024]);   // 每轮 10MB
                    total += 10;
                }
            } catch (OutOfMemoryError e) {
                log.info("④ OOM 复现：累计申请 {}MB 后 {}（强引用集合撑爆堆——典型内存泄漏形态）",
                        total, e.getClass().getSimpleName());
            }
            hold.clear();       // 引用可释放——与"缓存无界增长"事故同形态（20 篇事故复盘）
            log.info("④ 清理引用后可继续运行——OOM 事故排查第一步：谁持有引用不放");
        } catch (Exception e) {
            log.warn("演示异常", e);
        }
        log.info("GC 参数模板见类注释；线上排查三件套：jstat 看频率 → dump 看对象 → Arthas 看热点");
    }
}
