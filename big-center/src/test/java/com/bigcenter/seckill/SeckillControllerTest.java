package com.bigcenter.seckill;

import com.bigcenter.auth.UserContext;
import com.bigcenter.common.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 秒杀 Lua 语义验收（learn-center enroll.lua 语义钉子）：
 * 预扣成功发 MQ / 一人一课幂等 / 售罄快速失败不发 MQ。
 */
class SeckillControllerTest {

    private StringRedisTemplate redis;
    private RabbitTemplate rabbit;
    private SeckillController controller;

    @BeforeEach
    void setUp() throws Exception {
        redis = Mockito.mock(StringRedisTemplate.class);
        rabbit = Mockito.mock(RabbitTemplate.class);
        controller = new SeckillController(redis, rabbit);
        UserContext.set(1001L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void 预扣成功_返回1_发MQ建单消息() {
        when(redis.execute(Mockito.<RedisScript<Long>>any(), Mockito.<List<String>>any(), anyString()))
                .thenReturn(1L);
        ApiResponse<Map<String, Object>> resp = controller.grab(42L);
        assertEquals(0, resp.code());
        assertTrue(String.valueOf(resp.data().get("orderNo")).startsWith("S"));
        // 削峰：预扣成功必须发 MQ（异步建单），不能同步落库
        verify(rabbit).convertAndSend(Mockito.eq("bc.seckill"), Mockito.eq("seckill.created"), any(Object.class));
    }

    @Test
    void 一人一课_Lua返回负2_不发MQ() {
        when(redis.execute(Mockito.<RedisScript<Long>>any(), Mockito.<List<String>>any(), anyString()))
                .thenReturn(-2L);
        ApiResponse<Map<String, Object>> resp = controller.grab(42L);
        assertTrue(resp.code() != 0);
        assertTrue(resp.msg().contains("已抢过"));
        verify(rabbit, Mockito.never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void 售罄_Lua返回0_快速失败不发MQ() {
        when(redis.execute(Mockito.<RedisScript<Long>>any(), Mockito.<List<String>>any(), anyString()))
                .thenReturn(0L);
        ApiResponse<Map<String, Object>> resp = controller.grab(42L);
        assertTrue(resp.msg().contains("名额已满"));
        verify(rabbit, Mockito.never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void 结果轮询_已抢到返回true() {
        SetOperations<String, String> setOps = Mockito.mock(SetOperations.class);
        when(redis.opsForSet()).thenReturn(setOps);
        when(setOps.isMember("bc:seckill:users:42", "1001")).thenReturn(true);
        ApiResponse<Map<String, Object>> resp = controller.result(42L);
        assertEquals(Boolean.TRUE, resp.data().get("已抢到名额"));
    }
}
