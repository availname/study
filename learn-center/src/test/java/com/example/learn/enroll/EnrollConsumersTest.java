package com.example.learn.enroll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 抢课消费者单测（stage3 幂等 / stage4 延迟链的行为固定）：
 * ① 首次建单 → 扣名额 + 发延迟取消消息；② 重投（INSERT IGNORE 返回 0）→ 幂等跳过；
 * ③ 超时取消 CAS=0（已支付）→ 不回补名额；CAS=1 → DB+Redis 双回补。
 */
class EnrollConsumersTest {

    private NamedParameterJdbcTemplate jdbc;
    private RabbitTemplate rabbit;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private SetOperations<String, String> setOps;
    private EnrollConsumers consumers;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        jdbc = mock(NamedParameterJdbcTemplate.class);
        rabbit = mock(RabbitTemplate.class);
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        setOps = mock(SetOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.opsForSet()).thenReturn(setOps);
        consumers = new EnrollConsumers(jdbc, rabbit, redis);
    }

    @Test
    void 首次建单_扣名额并发延迟取消消息() {
        // 两次 update：①INSERT IGNORE 建单成功 ②扣名额成功
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1, 1);

        consumers.onEnrollCreated(Map.of("userId", 9L, "courseId", 5L));

        verify(jdbc, times(2)).update(anyString(), any(MapSqlParameterSource.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> msgCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rabbit).convertAndSend(eq("lc.enroll"), eq("enroll.delay"), msgCaptor.capture());
        assertEquals(Set.of("orderNo", "userId"), msgCaptor.getValue().keySet());
        assertTrue(((String) msgCaptor.getValue().get("orderNo")).startsWith("LC"));
    }

    @Test
    void 重复消费_幂等跳过_不发延迟消息() {
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(0);

        consumers.onEnrollCreated(Map.of("userId", 9L, "courseId", 5L));

        verify(jdbc, times(1)).update(anyString(), any(MapSqlParameterSource.class));
        verifyNoInteractions(rabbit);
    }

    @Test
    void DB名额不足_仍发延迟消息_订单可走取消链() {
        // 建单成功但名额扣减失败（Redis/DB 漂移）——当前行为：warn 后继续，超时链兜底回收
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1, 0);

        consumers.onEnrollCreated(Map.of("userId", 9L, "courseId", 5L));

        verify(rabbit).convertAndSend(eq("lc.enroll"), eq("enroll.delay"), any(Map.class));
    }

    @Test
    void 超时取消_待支付订单_双回补名额() {
        // 两次 update：①CAS 取消成功 ②DB 名额释放；再查 course_id
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1, 1);
        when(jdbc.queryForMap(anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(Map.of("course_id", 5L));

        consumers.onOrderTimeout(Map.of("orderNo", "LC123", "userId", 9L));

        verify(valueOps).increment("lc:enroll:quota:5");
        verify(setOps).remove("lc:enroll:users:5", "9");
    }

    @Test
    void 超时取消_订单已支付_CAS失败不回补() {
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(0);

        consumers.onOrderTimeout(Map.of("orderNo", "LC123", "userId", 9L));

        verify(jdbc, times(1)).update(anyString(), any(MapSqlParameterSource.class));
        verifyNoInteractions(valueOps, setOps);
    }
}
