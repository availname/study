package com.example.mqmall.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 消费幂等两层防线单测（教程 stage3/02 行为锁定）：
 * ① Redis setnx 第一道拦住重复；② DB 唯一键第二道兜底，失败必须回滚 Redis 标记；
 * ③ fail() 清两层，让重投消息能重新消费。
 */
class MessageDedupServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private NamedParameterJdbcTemplate jdbc;
    private MessageDedupService dedup;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        jdbc = mock(NamedParameterJdbcTemplate.class);
        dedup = new MessageDedupService(redis, jdbc);
    }

    @Test
    void 首次消费_setnx与插入都成功_返回true() {
        when(valueOps.setIfAbsent(eq("mq:dedup:m1"), eq("1"), any(Duration.class))).thenReturn(true);
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        assertTrue(dedup.tryConsume("order.paid", "m1"));
    }

    @Test
    void Redis键已存在_第一道拦截_不再碰DB() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertFalse(dedup.tryConsume("order.paid", "m1"));
        verifyNoInteractions(jdbc);
    }

    @Test
    void DB唯一键冲突_第二道兜底_并回滚Redis标记() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        assertFalse(dedup.tryConsume("order.paid", "m1"));
        verify(redis).delete("mq:dedup:m1");   // 不清掉重投永远被 Redis 拦死
    }

    @Test
    void fail_清理两层标记_允许重投重新消费() {
        dedup.fail("m1");

        verify(redis).delete("mq:dedup:m1");
        verify(jdbc).update(anyString(), any(MapSqlParameterSource.class));
    }
}
