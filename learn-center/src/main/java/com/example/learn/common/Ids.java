package com.example.learn.common;

import java.util.concurrent.atomic.AtomicLong;

/** ID 生成（教学简化雪花——全局唯一，避免"内存计数器重启撞号"的幂等误杀坑，详见踩坑实录坑二） */
public final class Ids {
    private static final AtomicLong SEQ = new AtomicLong();

    private Ids() {}

    public static long nextId() {
        return (System.currentTimeMillis() << 12) | (SEQ.incrementAndGet() & 0xFFF);
    }
}
