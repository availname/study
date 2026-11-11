package com.bigcenter.like;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** 点赞 Lua 语义：判重与计数原子——java-core/14 题一验收（RedisScript 匹配纪律） */
class LikeServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private org.springframework.data.redis.core.SetOperations<String, String> setOps;
    private LikeService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        setOps = Mockito.mock(org.springframework.data.redis.core.SetOperations.class);
        // 链式 mock：先固定各 ops 返回同一实例
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.opsForSet()).thenReturn(setOps);
        service = new LikeService(redis);
    }

    @Test
    void 点赞成功_Lua返回1() {
        // RedisScript 匹配纪律：Mockito.<RedisScript<Long>>any() 消歧，不用 any(DefaultRedisScript.class)
        when(redis.execute(Mockito.<RedisScript<Long>>any(), Mockito.<List<String>>any(), anyString()))
                .thenReturn(1L);
        assertEquals(1L, service.like(42L, 1001L));
    }

    @Test
    void 重复点赞_Lua返回负1_幂等拒绝() {
        // Set SADD 返回 0 → Lua 脚本短路返回 -1（判重与计数同脚本原子，不会重复 INCR）
        when(redis.execute(Mockito.<RedisScript<Long>>any(), Mockito.<List<String>>any(), anyString()))
                .thenReturn(-1L);
        assertEquals(-1L, service.like(42L, 1001L));
    }

    @Test
    void 取消点赞_没点过返回负1_点过返回1() {
        when(redis.execute(Mockito.<RedisScript<Long>>any(), Mockito.<List<String>>any(), anyString()))
                .thenReturn(-1L, 1L);       // 连续返回：先没点过，后正常取消
        assertEquals(-1L, service.unlike(42L, 1001L));
        assertEquals(1L, service.unlike(42L, 1001L));
    }

    @Test
    void 计数与是否点过_读加速层() {
        when(valueOps.get(LikeService.CNT_PREFIX + "42")).thenReturn("123");
        assertEquals(123L, service.likeCount(42L));
        when(valueOps.get(LikeService.CNT_PREFIX + "43")).thenReturn(null);
        assertEquals(0L, service.likeCount(43L));       // 无 key 视为 0，不抛 NPE

        when(redis.opsForSet().isMember(LikeService.SET_PREFIX + "42", "1001")).thenReturn(true);
        assertTrue(service.likedByMe(42L, 1001L));
        assertFalse(service.likedByMe(42L, 1002L));
    }
}
