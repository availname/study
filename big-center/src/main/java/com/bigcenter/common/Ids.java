package com.bigcenter.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花 ID（java-core/11-分布式ID 教学落地）：64 位 = 1 符号位 + 41 时间戳 + 10 机器ID + 12 序列
 * <p>与 learn-center 的简化版（时间戳<<12）不同，这里带机器位与时钟回拨处理——供短链发号/订单/红包使用。
 * 按组件注入 workerId（多实例部署时由配置区分），纯工具场景用静态 {@link #nextId()}。</p>
 */
@Component
public class Ids {

    /** 机器 ID 位数（0-1023）与序列位数（同毫秒 0-4095） */
    private static final int WORKER_BITS = 10;
    private static final int SEQ_BITS = 12;
    private static final long SEQ_MASK = (1L << SEQ_BITS) - 1;
    /** 起始纪元 2024-01-01（41 位时间戳可用约 69 年） */
    private static final long EPOCH = 1704038400000L;

    private static long workerIdBits = 0L;
    private static long sequence = 0L;
    private static long lastTimestamp = -1L;

    private final long workerId;

    public Ids(@Value("${big-center.worker-id:1}") long workerId) {
        if (workerId < 0 || workerId >= (1L << WORKER_BITS)) {
            throw new IllegalArgumentException("workerId 必须在 [0,1023] 内: " + workerId);
        }
        this.workerId = workerId;
        workerIdBits = workerId << SEQ_BITS;
    }

    /** 静态便捷入口：默认 workerId=1（单实例/单测场景） */
    public static long nextId() {
        return generate(1L);
    }

    /** 实例入口：使用配置的机器 ID（多实例部署） */
    public synchronized long next() {
        return generate(workerId);
    }

    private static synchronized long generate(long wid) {
        long now = System.currentTimeMillis();
        if (now < lastTimestamp) {
            // 时钟回拨处理（java-core/11 要点）：小幅回拨（≤5ms）自旋等待追平，大幅回拨直接拒绝——宁可失败不可撞号
            long drift = lastTimestamp - now;
            if (drift <= 5) {
                try {
                    Thread.sleep(drift << 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("时钟回拨等待被中断", e);
                }
                now = System.currentTimeMillis();
            }
            if (now < lastTimestamp) {
                throw new IllegalStateException("时钟回拨过大，拒绝发号: 回拨 " + (lastTimestamp - now) + "ms");
            }
        }
        if (now == lastTimestamp) {
            sequence = (sequence + 1) & SEQ_MASK;
            if (sequence == 0) {                    // 同毫秒序列耗尽 → 自旋到下一毫秒
                while ((now = System.currentTimeMillis()) <= lastTimestamp) { }
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = now;
        return ((now - EPOCH) << (WORKER_BITS + SEQ_BITS)) | workerIdBits | sequence;
    }
}
