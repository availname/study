package com.bigcenter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 死信兜底消费者（mq-mall stage3/05 总装版）：
 * 各队列重试耗尽的消息统一进 bc.dlq → 落库 t_mq_dead_letter 人工兜底（不丢消息）。
 */
@Component
public class DeadLetterConsumer {
    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper om;

    public DeadLetterConsumer(JdbcTemplate jdbc, ObjectMapper om) {
        this.jdbc = jdbc;
        this.om = om;
    }

    @RabbitListener(queues = "bc.dlq.queue")
    public void onDead(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        String messageId = message.getMessageProperties().getMessageId();
        // x-death 头里带来源队列与原因（repeatedly-rejected=重试耗尽 / expired=TTL 到期）
        java.util.List<Map<String, ?>> xDeath =
                message.getMessageProperties().getXDeathHeader();
        String sourceQueue = null;
        String reason = null;
        if (xDeath != null && !xDeath.isEmpty()) {
            sourceQueue = String.valueOf(xDeath.get(0).get("queue"));
            reason = String.valueOf(xDeath.get(0).get("reason"));
        }
        try {
            jdbc.update("INSERT INTO t_mq_dead_letter (message_id, source_queue, reason, payload) VALUES (?, ?, ?, ?)",
                    messageId, sourceQueue, reason, payload);
        } catch (Exception e) {
            // 死信落库本身失败：只打日志——死信队列消息不会重投，避免循环
            log.error("[死信] 落库失败 messageId={} err={}", messageId, e.getMessage());
            return;
        }
        log.warn("[死信] 落库 messageId={} source={} reason={}（人工兜底）", messageId, sourceQueue, reason);
    }
}
