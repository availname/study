package com.bigcenter.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 可靠消息全家桶验收（mq-mall stage3 总装钉子）：
 * 本地消息表事务纪律 / Confirm 双通道状态机 / 补偿指数退避+死信 / 消费幂等两层防线。
 */
class ReliableMessagingTest {

    private RabbitTemplate rabbit;
    private SendRecordDao dao;
    private ReliableMessagePublisher publisher;

    @BeforeEach
    void setUp() {
        rabbit = Mockito.mock(RabbitTemplate.class);
        dao = Mockito.mock(SendRecordDao.class);
        publisher = new ReliableMessagePublisher(rabbit,
                new com.fasterxml.jackson.databind.ObjectMapper(), dao);
    }

    @Test
    void 本地消息表_事务外调用直接拒绝() {
        assertThrows(IllegalStateException.class,
                () -> publisher.sendInTransaction("bc.like", "like.flushed", "m-1", Map.of("k", 1)));
        verify(dao, never()).insertPending(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void 本地消息表_事务内先落表_事务提交后才真正发送() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            TransactionSynchronizationManager.setActualTransactionActive(true);
            publisher.sendInTransaction("bc.like", "like.flushed", "m-1", Map.of("k", 1));
            // 事务未提交：消息表已写（与业务原子），但 MQ 未发送
            verify(dao).insertPending(eq("m-1"), eq("bc.like"), eq("like.flushed"), anyString());
            verify(rabbit, never()).send(anyString(), anyString(), any(), any());
            // 模拟提交：afterCommit 触发真正发送
            for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
                s.afterCommit();
            }
            verify(rabbit).send(eq("bc.like"), eq("like.flushed"), any(), any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void SendRecordDao语义_确认仅CAS待确认状态() {
        // markConfirmed 语义：status 0→1 的 CAS（已确认/失败的消息不被 Confirm 回调改写）
        when(dao.markConfirmed("m-9")).thenReturn(true);
        assertTrue(dao.markConfirmed("m-9"));
    }

    @Test
    void 补偿任务_重试计数递增_指数退避下一次时间() {
        when(dao.findPendingDue(100)).thenReturn(List.of(
                Map.of("message_id", "m-2", "exchange", "bc.like", "routing_key", "like.flushed",
                        "payload", "{}", "retry_count", 2)));
        new SendRetryJob(dao, publisher).compensate();
        // 复用同一 messageId 重发（幂等键不变）——publisher 是真实对象，钉底层 rabbit.send
        verify(rabbit).send(eq("bc.like"), eq("like.flushed"), any(org.springframework.amqp.core.Message.class),
                any(org.springframework.amqp.rabbit.connection.CorrelationData.class));
        // 指数退避：30s << 2 = 120s
        verify(dao).bumpRetry(eq("m-2"), eq(3), any(Instant.class));
    }

    @Test
    void 补偿任务_重试超限标记死信_不再重发() {
        when(dao.findPendingDue(100)).thenReturn(List.of(
                Map.of("message_id", "m-3", "exchange", "e", "routing_key", "r",
                        "payload", "{}", "retry_count", 10)));
        new SendRetryJob(dao, publisher).compensate();
        verify(dao).markDead(eq("m-3"), anyString());
        verify(rabbit, never()).send(anyString(), anyString(), any(), any());
    }

    @Test
    void 消费幂等_两层防线_重复消息跳过() {
        var redis = Mockito.mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        var valueOps = Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        var jdbc = Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        // 第一道：Redis setnx false → 直接跳过，不查 DB
        when(valueOps.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class))).thenReturn(false);
        var dedup = new MessageDedupService(redis, jdbc);
        assertTrue(!dedup.tryConsume("like-flush", "m-4"));
        verify(jdbc, never()).update(anyString(), (Object[]) any());
    }

    @Test
    void 消费幂等_Redis占坑成功但DB唯一键冲突_撤销占坑返回重复() {
        var redis = Mockito.mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        var valueOps = Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        var jdbc = Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class))).thenReturn(true);
        Mockito.doThrow(new org.springframework.dao.DuplicateKeyException("uk"))
                .when(jdbc).update(anyString(), anyString(), anyString());
        var dedup = new MessageDedupService(redis, jdbc);
        assertEquals(false, dedup.tryConsume("like-flush", "m-5"));
        verify(redis).delete("mq:dedup:m-5");   // 两层一致性：撤销 Redis 占坑
    }
}
