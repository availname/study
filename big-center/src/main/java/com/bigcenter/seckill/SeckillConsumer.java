package com.bigcenter.seckill;

import com.bigcenter.common.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 秒杀建单消费者（漏斗第三层→第四层落地，learn-center EnrollConsumers 总装版）：
 * MQ 削峰匀速落库；唯一索引(user_id+course_id)兜底幂等（重投不重复建单）；
 * DB 扣名额带 WHERE enrolled < quota 防超卖（Redis/DB 漂移时的最终防线）。
 */
@Component
public class SeckillConsumer {
    private static final Logger log = LoggerFactory.getLogger(SeckillConsumer.class);

    private final JdbcTemplate jdbc;

    public SeckillConsumer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @RabbitListener(queues = "bc.seckill.created.queue")
    public void onCreated(Map<String, Object> msg) {
        long userId = ((Number) msg.get("userId")).longValue();
        long courseId = ((Number) msg.get("courseId")).longValue();
        String orderNo = String.valueOf(msg.get("orderNo"));

        try {
            // ① 建订单（唯一索引 user_id+course_id 兜底幂等）
            int inserted = jdbc.update("""
                    INSERT IGNORE INTO t_seckill_order (order_no, user_id, course_id, status)
                    VALUES (?, ?, ?, 0)
                    """, orderNo, userId, courseId);
            if (inserted == 0) {
                log.info("[秒杀建单] 已存在（幂等跳过）orderNo={}", orderNo);
                return;
            }
            // ② DB 扣名额（WHERE enrolled < quota 防超卖——Redis 预扣之外的第二道防线）
            int n = jdbc.update("UPDATE t_course SET enrolled = enrolled + 1 WHERE id = ? AND enrolled < quota", courseId);
            if (n == 0) {
                log.warn("[秒杀建单] DB 名额不足（Redis/DB 漂移）courseId={}", courseId);
            }
            log.info("[秒杀建单] orderNo={} userId={} courseId={} 完成", orderNo, userId, courseId);
        } catch (Exception e) {
            log.error("[秒杀建单] 失败 orderNo={} err={}", orderNo, e.getMessage(), e);
            throw e;    // 上抛 → 本地重试 3 次 → 死信兜底
        }
    }
}
