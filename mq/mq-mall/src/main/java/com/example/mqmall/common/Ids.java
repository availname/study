package com.example.mqmall.common;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/** ID 生成。教学版简化雪花算法；生产请用 Leaf/雪花时钟回拨处理版 */
public final class Ids {
    private static final AtomicLong SEQ = new AtomicLong();
    private static final SecureRandom RND = new SecureRandom();

    private Ids() {}

    /** 时间戳毫秒 << 12 | 12位序列：单机够用 */
    public static long nextId() {
        return (System.currentTimeMillis() << 12) | (SEQ.incrementAndGet() & 0xFFF);
    }

    public static String orderNo() {
        String ts = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "O" + ts + String.format("%04d", RND.nextInt(10000));
    }
}
