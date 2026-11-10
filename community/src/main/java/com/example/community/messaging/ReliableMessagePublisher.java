package com.example.community.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 可靠消息发送门面（与 mq-mall 同款模式，stage3/01 本地消息表）。
 * sendInTransaction：事务内写发送记录（与业务原子）→ 提交后发送 → Confirm 更新 → 补偿 job 兜底。
 */
@Service
public class ReliableMessagePublisher {
    private static final Logger log = LoggerFactory.getLogger(ReliableMessagePublisher.class);
    private static final int MAX_RETRY = 10;
    private static final long BASE_BACKOFF_MS = 30_000;

    private final RabbitTemplate rabbit;
    private final ObjectMapper om;
    private final SendRecordDao dao;

    public ReliableMessagePublisher(RabbitTemplate rabbit, ObjectMapper om, SendRecordDao dao) {
        this.rabbit = rabbit;
        this.om = om;
        this.dao = dao;
    }

    /** 必须在 @Transactional 内调用：先写本地消息表（与业务原子），提交后发送 */
    public void sendInTransaction(String exchange, String routingKey, String messageId, Object payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("sendInTransaction 必须在业务事务内调用");
        }
        String payloadJson = toJson(payload);
        dao.insertPending(messageId, exchange, routingKey, payloadJson);   // 与业务同事务
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { doSend(exchange, routingKey, messageId, payloadJson); }
        });
    }

    /** 事务外快速发送（低价值通知等场景） */
    public void sendFast(String exchange, String routingKey, String messageId, Object payload) {
        doSend(exchange, routingKey, messageId, toJson(payload));
    }

    private void doSend(String exchange, String routingKey, String messageId, String payloadJson) {
        try {
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            props.setMessageId(messageId);
            rabbit.send(exchange, routingKey,
                    new Message(payloadJson.getBytes(StandardCharsets.UTF_8), props),
                    new CorrelationData(messageId));
        } catch (Exception e) {
            log.error("[发送失败-待补偿] messageId={} err={}", messageId, e.getMessage());
        }
    }

    private String toJson(Object payload) {
        try {
            return om.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("payload 序列化失败", e);
        }
    }

    /** 补偿任务（SendRetryJob 调用）：复用原始 payload 与 messageId 重发 */
    public void resend(String exchange, String routingKey, String messageId, String payloadJson) {
        doSend(exchange, routingKey, messageId, payloadJson);
    }

    /** 补偿 job 本体：扫描超时未确认的消息重发，指数退避，超限标死信 */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 10_000)
    public void compensate() {
        List<Map<String, Object>> pending = dao.findPendingDue(100);
        for (Map<String, Object> row : pending) {
            String messageId = (String) row.get("message_id");
            int retryCount = ((Number) row.get("retry_count")).intValue();
            if (retryCount >= MAX_RETRY) {
                dao.markDead(messageId, "补偿重试超限");
                log.error("[补偿] messageId={} 重试超限标记死信", messageId);
                continue;
            }
            resend((String) row.get("exchange"), (String) row.get("routing_key"),
                    messageId, (String) row.get("payload"));
            long backoff = BASE_BACKOFF_MS << Math.min(retryCount, 5);
            dao.bumpRetry(messageId, retryCount + 1, Instant.now().plusMillis(backoff));
            log.warn("[补偿] 重发 messageId={} 第{}次", messageId, retryCount + 1);
        }
    }
}
