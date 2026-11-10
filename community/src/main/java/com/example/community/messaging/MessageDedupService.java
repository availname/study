package com.example.community.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 消费幂等两层防线（stage3/02，模式复用自 mq-mall）：
 * Redis setnx 第一道 + t_mq_consume_record 唯一键第二道（本库）。
 */
@Service
public class MessageDedupService {
    private static final Logger log = LoggerFactory.getLogger(MessageDedupService.class);
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final NamedParameterJdbcTemplate jdbc;

    public MessageDedupService(StringRedisTemplate redis, NamedParameterJdbcTemplate jdbc) {
        this.redis = redis;
        this.jdbc = jdbc;
    }

    public boolean tryConsume(String consumer, String messageId) {
        Boolean first = redis.opsForValue().setIfAbsent("mq:dedup:" + messageId, "1", TTL);
        if (Boolean.FALSE.equals(first)) {
            log.info("[幂等] 重复消息跳过: {} {}", consumer, messageId);
            return false;
        }
        try {
            jdbc.update("INSERT INTO t_mq_consume_record (message_id, consumer) VALUES (:mid, :c)",
                    new MapSqlParameterSource().addValue("mid", messageId).addValue("c", consumer));
            return true;
        } catch (DuplicateKeyException e) {
            redis.delete("mq:dedup:" + messageId);
            return false;
        }
    }

    public void fail(String messageId) {
        redis.delete("mq:dedup:" + messageId);
        jdbc.update("DELETE FROM t_mq_consume_record WHERE message_id = :mid",
                new MapSqlParameterSource("mid", messageId));
    }

    @Scheduled(fixedDelay = 3600_000)
    public void cleanup() {
        jdbc.update("DELETE FROM t_mq_consume_record WHERE created_at < NOW(3) - INTERVAL 24 HOUR",
                new MapSqlParameterSource());
    }
}
