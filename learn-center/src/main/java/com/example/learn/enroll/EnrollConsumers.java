package com.example.learn.enroll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 抢课消费者：异步建单（mq 教学 stage4/01 削峰落地）。
 * 唯一索引(user_id+course_id)兜底幂等；订单建好后发延迟取消消息（超时未支付自动取消）。
 */
@Component
public class EnrollConsumers {
    private static final Logger log = LoggerFactory.getLogger(EnrollConsumers.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbit;
    private final StringRedisTemplate redis;

    public EnrollConsumers(NamedParameterJdbcTemplate jdbc,
                          org.springframework.amqp.rabbit.core.RabbitTemplate rabbit,
                          StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
        this.redis = redis;
    }

    @RabbitListener(queues = "lc.enroll.created.queue")
    public void onEnrollCreated(Map<String, Object> msg) {
        long userId = ((Number) msg.get("userId")).longValue();
        long courseId = ((Number) msg.get("courseId")).longValue();
        String orderNo = "LC" + com.example.learn.common.Ids.nextId();

        try {
            // ① 建订单（唯一索引 user_id+course_id 兜底幂等：重投不会重复建单）
            int inserted = jdbc.update("""
                    INSERT IGNORE INTO t_enroll_order (order_no, user_id, course_id, status, price)
                    SELECT :ono, :uid, :cid, 0, c.price FROM t_course c
                    WHERE c.id = :cid
                    """, new MapSqlParameterSource()
                    .addValue("ono", orderNo).addValue("uid", userId).addValue("cid", courseId));
            if (inserted == 0) {
                log.info("[建单] 已存在（幂等跳过） orderNo={}", orderNo);
                return;
            }
            // ② 扣课程名额（DB 兜底：WHERE enrolled < quota 防超卖）
            int n = jdbc.update("""
                    UPDATE t_course SET enrolled = enrolled + 1
                    WHERE id = :cid AND enrolled < quota
                    """, new MapSqlParameterSource().addValue("cid", courseId));
            if (n == 0) {
                log.warn("[建单] DB 名额不足（Redis/DB 漂移） courseId={}", courseId);
            }
            // ③ 发延迟取消消息：超时未支付自动取消（TTL+DLX 延迟链）
            rabbit.convertAndSend("lc.enroll", "enroll.delay",
                    Map.of("orderNo", orderNo, "userId", userId));
            log.info("[建单] orderNo={} userId={} courseId={} 建单完成，已发超时取消延迟消息", orderNo, userId, courseId);
        } catch (Exception e) {
            log.error("[建单] 失败 orderNo={} err={}", orderNo, e.getMessage(), e);
            throw e;      // 上抛 → 本地重试 3 次 → 死信
        }
    }

    /** 超时取消消费者：TTL 到期消息 → CAS 取消 + Redis 名额回补 */
    @RabbitListener(queues = "lc.order.timeout.queue")
    public void onOrderTimeout(Map<String, Object> msg) {
        String orderNo = String.valueOf(msg.get("orderNo"));
        long userId = ((Number) msg.get("userId")).longValue();
        // CAS 取消：只有待支付才取消（与支付并发互斥）
        int n = jdbc.update("""
                UPDATE t_enroll_order SET status = 2, cancel_at = NOW(3)
                WHERE order_no = :ono AND status = 0
                """, new MapSqlParameterSource().addValue("ono", orderNo));
        if (n == 1) {
            // 释放 DB 名额 + Redis 名额回补（一人一课集合保留——用户还能再抢）
            jdbc.update("""
                    UPDATE t_course c JOIN t_enroll_order o ON o.course_id = c.id
                    SET c.enrolled = GREATEST(c.enrolled - 1, 0)
                    WHERE o.order_no = :ono
                    """, new MapSqlParameterSource().addValue("ono", orderNo));
            Map<String, Object> order = jdbc.queryForMap(
                    "SELECT course_id FROM t_enroll_order WHERE order_no = :ono",
                    new MapSqlParameterSource().addValue("ono", orderNo));
            long courseId = ((Number) order.get("course_id")).longValue();
            redis.opsForValue().increment("lc:enroll:quota:" + courseId);
            redis.opsForSet().remove("lc:enroll:users:" + courseId, String.valueOf(userId));
            log.info("[超时取消] orderNo={} 已取消，Redis/DB 名额均已回补", orderNo);
        }
    }
}
