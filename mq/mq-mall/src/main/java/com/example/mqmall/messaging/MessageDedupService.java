package com.example.mqmall.messaging;

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
 * 消费幂等两层防线（教程 stage3/02 的生产级封装）。
 * 第一道：Redis setnx 快速去重（挡 99%）
 * 第二道：t_mq_consume_record 唯一主键（Redis 挂了/清空后仍兜底）
 *
 * 用法：
 *   if (!dedup.tryConsume("order-paid", messageId)) { return; }  // 重复，跳过
 *   try { doBusiness(); } catch (Exception e) { dedup.fail(messageId); throw e; }
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

    /** true = 第一次见到，继续处理；false = 重复消息，直接跳过 */
    public boolean tryConsume(String consumer, String messageId) {
        Boolean first = redis.opsForValue().setIfAbsent("mq:dedup:" + messageId, "1", TTL);
        if (Boolean.FALSE.equals(first)) {
            return false;
        }
        try {
            jdbc.update("INSERT INTO t_mq_consume_record (message_id, consumer) VALUES (:mid, :c)",
                    new MapSqlParameterSource().addValue("mid", messageId).addValue("c", consumer));
            return true;
        } catch (DuplicateKeyException e) {
            redis.delete("mq:dedup:" + messageId);   // 撤销 Redis 占坑，保持两层一致
            return false;
        }
    }

    /** 业务失败时必须调用，否则重投的消息会被去重键误杀 */
    public void fail(String messageId) {
        redis.delete("mq:dedup:" + messageId);
        jdbc.update("DELETE FROM t_mq_consume_record WHERE message_id = :mid",
                new MapSqlParameterSource("mid", messageId));
    }

    /** 每小时清理 24h 前的去重记录（应 ≥ 消息最大重投周期） */
    @Scheduled(fixedDelay = 3600_000)
    public void cleanup() {
        int n = jdbc.update("DELETE FROM t_mq_consume_record WHERE created_at < NOW(3) - INTERVAL 24 HOUR",
                new MapSqlParameterSource());
        if (n > 0) log.info("[幂等清理] 删除过期去重记录 {} 条", n);
    }
}
