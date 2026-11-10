package com.example.learn.enroll;

import com.example.learn.common.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 抢课分布式闸门单测（r-stage3/03 看门狗锁的控制流固定，不需要真 Redis）：
 * ① tryLock 成功 → 走 Lua 预扣 + 发 MQ，finally 解锁；
 * ② tryLock 失败（3 秒内没抢到）→ 快速失败提示，绝不发 MQ；
 * ③ 防误删：只解自己持有的锁（isHeldByCurrentThread 才 unlock）。
 */
class EnrollLockTest {

    private StringRedisTemplate redis;
    private RabbitTemplate rabbit;
    private RedissonClient redisson;
    private RLock lock;
    private EnrollController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        redis = mock(StringRedisTemplate.class);
        rabbit = mock(RabbitTemplate.class);
        redisson = mock(RedissonClient.class);
        lock = mock(RLock.class);
        when(redisson.getLock(anyString())).thenReturn(lock);
        controller = new EnrollController(redis, rabbit, redisson);
        com.example.learn.user.UserContext.set(1001L);
    }

    @Test
    void 抢到锁且Lua预扣成功_发MQ建单并解锁() throws Exception {
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), anyString())).thenReturn(1L);

        ApiResponse<Map<String, Object>> resp = controller.enroll("t", 101L);

        assertEquals(0, resp.code());
        assertTrue(String.valueOf(resp.data().get("orderNo")).startsWith("E"));
        // 锁名 = 同课闸门：lc:lock:enroll:{courseId}
        verify(redisson).getLock("lc:lock:enroll:101");
        ArgumentCaptor<Object> msg = ArgumentCaptor.forClass(Object.class);
        verify(rabbit).convertAndSend(eq("lc.enroll"), eq("enroll.created"), msg.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) msg.getValue();
        assertEquals(1001L, body.get("userId"));
        verify(lock).unlock();
    }

    @Test
    void 三秒没抢到锁_快速失败且不发MQ() throws Exception {
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(false);

        ApiResponse<Map<String, Object>> resp = controller.enroll("t", 101L);

        assertEquals(400, resp.code());
        assertEquals("抢课人数过多，请稍后再试", resp.msg());
        verify(rabbit, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        // 没拿到锁绝不能 unlock（解了别人的锁 = SETNX 演进里的误删事故）
        verify(lock, never()).unlock();
    }

    @Test
    void Lua返回已抢过_直接提示不发MQ() throws Exception {
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true); // finally 里解锁的前置条件
        when(redis.execute(any(DefaultRedisScript.class), anyList(), anyString())).thenReturn(-2L);

        ApiResponse<Map<String, Object>> resp = controller.enroll("t", 101L);

        assertEquals(400, resp.code());
        assertEquals("你已抢过这门课（一人一课）", resp.msg());
        verify(rabbit, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(lock).unlock();
    }

    @Test
    void 中断响应_恢复中断位并快速失败() throws Exception {
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

        ApiResponse<Map<String, Object>> resp = controller.enroll("t", 101L);

        assertEquals(400, resp.code());
        assertTrue(Thread.interrupted()); // 校验已恢复中断位，顺便清掉
        verify(rabbit, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(lock, never()).unlock();
    }
}
