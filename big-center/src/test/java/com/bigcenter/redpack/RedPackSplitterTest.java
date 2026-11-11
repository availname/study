package com.bigcenter.redpack;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 二倍均值法：总和精确/每份≥1分/期望均等——14 题二拆分验收 */
class RedPackSplitterTest {

    @Test
    void 拆分总和严格等于总额_每份至少1分() {
        for (int trial = 0; trial < 200; trial++) {
            List<Long> parts = RedPackSplitter.split(10000, 7);      // 100 元发 7 份
            assertEquals(7, parts.size());
            assertEquals(10000L, parts.stream().mapToLong(Long::longValue).sum(), "总和必须精确");
            assertTrue(parts.stream().allMatch(p -> p >= 1), "每份 ≥1 分");
        }
    }

    @Test
    void 金额等于份数_每人恰好1分() {
        List<Long> parts = RedPackSplitter.split(5, 5);
        assertEquals(List.of(1L, 1L, 1L, 1L, 1L), parts);
    }

    @Test
    void 一人独享_拿全额() {
        assertEquals(List.of(888L), RedPackSplitter.split(888, 1));
    }

    @Test
    void 金额不足拆分_拒绝() {
        assertThrows(IllegalArgumentException.class, () -> RedPackSplitter.split(3, 5));
        assertThrows(IllegalArgumentException.class, () -> RedPackSplitter.split(100, 0));
    }
}
