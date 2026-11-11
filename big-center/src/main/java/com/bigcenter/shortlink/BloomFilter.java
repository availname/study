package com.bigcenter.shortlink;

/**
 * 布隆过滤器（java-core/12 题一 + r-stage3/01 缓存穿透防护）。
 * 简化实现（BitSet + 双哈希）：教学演示"可能存在/一定不存在"语义；
 * 生产用 RedisBloom/Redisson RBloomBit。新发号即加入，恶意短码在查库前就被 404 拦截。
 */
public class BloomFilter {

    private final java.util.BitSet bits = new java.util.BitSet();
    private final int bitSize;

    public BloomFilter(int bitSize) {
        this.bitSize = bitSize;
    }

    public void add(String value) {
        bits.set(hash(value, 17));
        bits.set(hash(value, 31));
    }

    /** false=一定不存在（穿透防护拦下）；true=可能存在（继续查缓存/库） */
    public boolean mightContain(String value) {
        return bits.get(hash(value, 17)) && bits.get(hash(value, 31));
    }

    private int hash(String value, int seed) {
        int h = seed;
        for (byte b : value.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            h = h * 31 + b;
        }
        h = h ^ (h >>> 16);
        return Math.abs(h % bitSize);
    }
}
