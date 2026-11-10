package com.example.mqmall.messaging;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 本地消息表 t_mq_send_record 的读写（教程 stage3/01） */
@Repository
public class SendRecordDao {

    private final NamedParameterJdbcTemplate jdbc;

    public SendRecordDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 必须在业务事务内调用：随业务一起提交，保证"业务成功⇔消息必发"原子 */
    public void insertPending(String messageId, String exchange, String routingKey, String payload) {
        jdbc.update("""
                INSERT INTO t_mq_send_record (message_id, exchange, routing_key, payload, status, next_retry_at)
                VALUES (:mid, :ex, :rk, :payload, 0, NOW(3))
                """, new MapSqlParameterSource()
                .addValue("mid", messageId)
                .addValue("ex", exchange)
                .addValue("rk", routingKey)
                .addValue("payload", payload));
    }

    public void markConfirmed(String messageId) {
        jdbc.update("UPDATE t_mq_send_record SET status = 1 WHERE message_id = :mid AND status = 0",
                new MapSqlParameterSource("mid", messageId));
    }

    public void markFailed(String messageId, String error) {
        jdbc.update("""
                UPDATE t_mq_send_record SET status = 2, last_error = :err
                WHERE message_id = :mid AND status = 0
                """, new MapSqlParameterSource()
                .addValue("mid", messageId)
                .addValue("err", trim(error)));
    }

    /** 待补偿的消息：超时未确认 */
    public List<Map<String, Object>> findPendingDue(int limit) {
        return jdbc.queryForList("""
                SELECT id, message_id, exchange, routing_key, payload, retry_count
                FROM t_mq_send_record
                WHERE status = 0 AND next_retry_at <= NOW(3)
                ORDER BY id LIMIT :lim
                """, new MapSqlParameterSource("lim", limit));
    }

    public void bumpRetry(String messageId, int retryCount, Instant nextRetryAt) {
        jdbc.update("""
                UPDATE t_mq_send_record SET retry_count = :rc, next_retry_at = :nr
                WHERE message_id = :mid
                """, new MapSqlParameterSource()
                .addValue("rc", retryCount)
                .addValue("nr", Timestamp.from(nextRetryAt))
                .addValue("mid", messageId));
    }

    public void markDead(String messageId, String error) {
        jdbc.update("UPDATE t_mq_send_record SET status = 3, last_error = :err WHERE message_id = :mid",
                new MapSqlParameterSource()
                        .addValue("mid", messageId)
                        .addValue("err", trim(error)));
    }

    public Map<String, Long> countByStatus() {
        Map<String, Long> result = new java.util.HashMap<>();
        jdbc.queryForList("SELECT status, COUNT(*) c FROM t_mq_send_record GROUP BY status",
                        new MapSqlParameterSource())
                .forEach(row -> result.put("status_" + row.get("status"), ((Number) row.get("c")).longValue()));
        return result;
    }

    private static String trim(String s) {
        return s == null ? null : (s.length() > 250 ? s.substring(0, 250) : s);
    }
}
