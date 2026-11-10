package com.example.mqmall.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 通用死信消费者（教程 stage3/05）。
 * 所有业务队列重试耗尽的消息统一进入 mall.dlq.queue，落库存档 + 告警，
 * 保证"失败可见可控"，绝不无声丢弃。
 */
@Component
public class DeadLetterConsumer {
    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    private final NamedParameterJdbcTemplate jdbc;

    public DeadLetterConsumer(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @RabbitListener(queues = "mall.dlq.queue")
    public void onDeadLetter(Message message) {
        MessageProperties props = message.getMessageProperties();
        String messageId = props.getMessageId();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        // x-death 头携带死因：rejected(重试耗尽) / expired(TTL) / maxlen(超长)
        String reason = "unknown";
        String sourceQueue = props.getConsumerQueue();
        Object xDeath = props.getHeaders().get("x-death");
        if (xDeath instanceof List<?> deaths && !deaths.isEmpty() && deaths.get(0) instanceof Map<?, ?> death) {
            Object r = death.get("reason");
            Object q = death.get("queue");
            if (r != null) reason = r.toString();
            if (q != null) sourceQueue = q.toString();
        }

        log.error("[死信] messageId={} queue={} reason={} body={}", messageId, sourceQueue, reason, body);
        jdbc.update("""
                INSERT INTO t_mq_dead_letter (message_id, source_queue, reason, payload)
                VALUES (:mid, :q, :r, :p)
                """, new MapSqlParameterSource()
                .addValue("mid", messageId)
                .addValue("q", sourceQueue)
                .addValue("r", reason)
                .addValue("p", body));
        // 生产做法：写入后触发告警（钉钉/企微/邮件），由人工或兜底 job 处理
    }
}
