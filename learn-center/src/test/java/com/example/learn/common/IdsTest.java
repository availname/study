package com.example.learn.common;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 雪花简化版 ID：同毫秒内 12 位序列防撞号（幂等防误杀的前提，详见踩坑实录坑二） */
class IdsTest {

    @Test
    void 同一批次内不重复且严格递增() {
        int n = 2000;                      // < 4096（12 位序列上限），同毫秒内不会回绕
        Set<Long> seen = new HashSet<>();
        long prev = -1;
        for (int i = 0; i < n; i++) {
            long id = Ids.nextId();
            assertTrue(seen.add(id), "ID 重复: " + id);
            assertTrue(id > prev, "ID 未递增: " + id + " after " + prev);
            prev = id;
        }
        assertEquals(n, seen.size());
    }

    @Test
    void 时间戳部分随时间前进() {
        long before = Ids.nextId();
        long after = 0;
        try {
            Thread.sleep(3);               // 跨毫秒 → 高位时间戳变化
            after = Ids.nextId();
        } catch (InterruptedException ignored) { }
        assertTrue(after > before);
    }
}
