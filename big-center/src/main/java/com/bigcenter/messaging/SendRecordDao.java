package com.bigcenter.messaging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 本地消息表 t_mq_send_record 读写（mq-mall SendRecordDao 总装版，stage3/01） */
@Repository
public class SendRecordDao {

    private final JdbcTemplate jdbc;

    public SendRecordDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 必须在业务事务内调用：随业务一起提交，保证"业务成功⇔消息必发"原子 */
    public void insertPending(String messageId, String exchange, String routingKey, String payload) {
        jdbc.update("""
                INSERT INTO t_mq_send_record (message_id, exchange, routing_key, payload, status, next_retry_at)
                VALUES (?, ?, ?, ?, 0, NOW(3))
                """, messageId, exchange, routingKey, payload);
    }

    public boolean markConfirmed(String messageId) {
        return jdbc.update("UPDATE t_mq_send_record SET status = 1 WHERE message_id = ? AND status = 0",
                messageId) > 0;
    }

    public void markFailed(String messageId, String error) {
        jdbc.update("UPDATE t_mq_send_record SET status = 2, last_error = ? WHERE message_id = ? AND status = 0",
                trim(error), messageId);
    }

    /** 待补偿的消息：超时未确认（SendRetryJob 扫描重发） */
    public List<Map<String, Object>> findPendingDue(int limit) {
        return jdbc.queryForList("""
                SELECT message_id, exchange, routing_key, payload, retry_count
                FROM t_mq_send_record
                WHERE status = 0 AND next_retry_at <= NOW(3)
                ORDER BY id LIMIT ?
                """, limit);
    }

    public void bumpRetry(String messageId, int retryCount, Instant nextRetryAt) {
        jdbc.update("UPDATE t_mq_send_record SET retry_count = ?, next_retry_at = ? WHERE message_id = ?",
                retryCount, Timestamp.from(nextRetryAt), messageId);
    }

    /** 重试超限 → 死信状态（3），不再补偿，人工兜底 */
    public void markDead(String messageId, String error) {
        jdbc.update("UPDATE t_mq_send_record SET status = 3, last_error = ? WHERE message_id = ?", trim(error), messageId);
    }

    private String trim(String s) {
        return s == null ? null : s.substring(0, Math.min(s.length(), 250));
    }
}
