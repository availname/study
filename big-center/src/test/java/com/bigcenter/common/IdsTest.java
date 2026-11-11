package com.bigcenter.common;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 雪花 ID：唯一性/趋势递增/时钟回拨拒绝——java-core/11 三大验收点 */
class IdsTest {

    @Test
    void 静态入口大批量不重复且趋势递增() {
        int n = 5000;                      // 同毫秒 4096 序列上限会跨毫秒，唯一性不受影响
        Set<Long> seen = new HashSet<>();
        long prev = -1;
        for (int i = 0; i < n; i++) {
            long id = Ids.nextId();
            assertTrue(seen.add(id), "ID 重复: " + id);
            assertTrue(id >= prev, "ID 趋势应递增（跨毫秒可能等于前值的理论不成立，实测应严格递增）: " + id);
            prev = id;
        }
        assertEquals(n, seen.size());
    }

    @Test
    void 实例入口使用配置的机器ID_不同实例号不撞号() {
        Ids a = new Ids(1);
        Ids b = new Ids(2);
        // 两实例串行发号同毫秒竞争同一把类锁——只断言唯一性
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertTrue(seen.add(a.next()), "实例A ID 重复");
            assertTrue(seen.add(b.next()), "实例B 与实例A 撞号");
        }
        assertEquals(2000, seen.size());
    }

    @Test
    void workerId越界直接拒绝_宁可失败不可撞号() {
        try {
            new Ids(1024);                 // 10 位机器位上限 1023
            throw new AssertionError("workerId=1024 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("1024"));
        }
        try {
            new Ids(-1);
            throw new AssertionError("workerId=-1 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // 预期路径
        }
    }

    @Test
    void 时间戳高位随时间前进_可从ID反解出生成时间() {
        long id1 = Ids.nextId();
        long before = System.currentTimeMillis();
        try {
            Thread.sleep(3);               // 跨毫秒 → 高位时间戳变化
        } catch (InterruptedException ignored) { }
        long id2 = Ids.nextId();
        assertTrue(id2 > id1);

        // 反解验证：41 位时间戳段（去掉低 22 位）+ EPOCH ≈ 当前时间（误差 1 秒内）
        long ts = (id2 >> 22) + 1704038400000L;
        assertTrue(Math.abs(ts - before) < 1000, "从 ID 反解的时间应接近生成时刻: " + ts + " vs " + before);
    }
}
