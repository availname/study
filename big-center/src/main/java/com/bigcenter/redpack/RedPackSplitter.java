package com.bigcenter.redpack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 二倍均值法拆分（java-core/14 题二）：
 * 每份 = random[1, 剩余金额/剩余人数×2) 分，最后一人拿剩余——
 * 保证总和精确、每份 ≥1 分、金额期望均等。预拆好入库/入 List，抢时 LPOP 原子认领。
 */
public final class RedPackSplitter {

    private RedPackSplitter() {}

    /** 拆分：totalFen 总金额（分）、count 份数；每份 ≥1 分，总和严格等于 totalFen */
    public static List<Long> split(long totalFen, int count) {
        if (count <= 0 || totalFen < count) {
            throw new IllegalArgumentException("金额不足拆分（每份至少 1 分）: total=" + totalFen + " count=" + count);
        }
        List<Long> parts = new ArrayList<>(count);
        long remain = totalFen;
        int remainCount = count;
        for (int i = 0; i < count - 1; i++) {
            // 二倍均值：上限 = 剩余/剩余人数×2（减去保底：后面每人还要留 1 分）
            long avg2 = (remain - (remainCount - 1)) / remainCount * 2;
            long grab = remainCount == 1 ? remain
                    : ThreadLocalRandom.current().nextLong(1, Math.max(2, avg2 + 1));
            parts.add(grab);
            remain -= grab;
            remainCount--;
        }
        parts.add(remain);           // 最后一人拿剩余（≥1 分由上限约束保证）
        return parts;
    }
}
