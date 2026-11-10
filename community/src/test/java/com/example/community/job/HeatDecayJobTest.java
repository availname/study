package com.example.community.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 热榜时间衰减单测（stage5/05 热搜的行为固定）：
 * 分数 ×0.98 更新；衰减后 <0.5 出榜（remove 而非 add）；空榜不动作。
 */
class HeatDecayJobTest {

    private StringRedisTemplate redis;
    private ZSetOperations<String, String> zset;
    private HeatDecayJob job;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        zset = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zset);
        job = new HeatDecayJob(redis);
    }

    private static TypedTuple<String> tuple(String value, Double score) {
        return new DefaultTypedTuple<>(value, score);
    }

    @Test
    void 正常衰减_分数乘以098() {
        Set<TypedTuple<String>> all = new LinkedHashSet<>();
        all.add(tuple("101", 100.0));
        all.add(tuple("102", 7.0));
        when(zset.rangeWithScores("community:post:hot", 0, -1)).thenReturn(all);

        job.decay();

        verify(zset).add("community:post:hot", "101", 98.0);
        verify(zset).add("community:post:hot", "102", 7.0 * 0.98);
        verify(zset, never()).remove(anyString(), anyString());
    }

    @Test
    void 衰减后低于阈值_出榜而不是更新() {
        Set<TypedTuple<String>> all = new LinkedHashSet<>();
        all.add(tuple("103", 0.5));                    // 0.5×0.98=0.49 < 0.5 → 出榜
        all.add(tuple("104", 0.51));                   // 0.51×0.98≈0.4998 < 0.5 → 出榜
        when(zset.rangeWithScores("community:post:hot", 0, -1)).thenReturn(all);

        job.decay();

        verify(zset).remove("community:post:hot", "103");
        verify(zset).remove("community:post:hot", "104");
        verify(zset, never()).add(anyString(), anyString(), org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    void score为null_跳过不动作() {
        Set<TypedTuple<String>> all = new LinkedHashSet<>();
        all.add(tuple("105", null));
        when(zset.rangeWithScores("community:post:hot", 0, -1)).thenReturn(all);

        job.decay();

        verify(zset, never()).add(anyString(), anyString(), org.mockito.ArgumentMatchers.anyDouble());
        verify(zset, never()).remove(anyString(), anyString());
    }

    @Test
    void 空榜_直接返回不动作() {
        when(zset.rangeWithScores("community:post:hot", 0, -1)).thenReturn(null);

        job.decay();

        verify(zset, never()).add(anyString(), anyString(), org.mockito.ArgumentMatchers.anyDouble());
        verify(zset, never()).remove(anyString(), anyString());   // 空榜：无任何写动作
    }
}
