package com.example.mqmall.orderconsumer;

import com.example.mqmall.messaging.MessageDedupService;
import com.example.mqmall.messaging.Messages.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * order.paid 消费者：支付成功的三个下游（教程 stage4/03）。
 * 幂等三层示范：
 *   ① 消息级去重（Redis+DB 两层，MessageDedupService）
 *   ② 积分：t_points_log.message_id 唯一索引兜底
 *   ③ 优惠券：user+type+activity 业务唯一键兜底（防"Redis 失效重复发券"事故）
 */
@Component
public class OrderPaidConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderPaidConsumer.class);
    private static final int PAY_POINTS = 100;

    private final MessageDedupService dedup;
    private final NamedParameterJdbcTemplate jdbc;

    public OrderPaidConsumer(MessageDedupService dedup, NamedParameterJdbcTemplate jdbc) {
        this.dedup = dedup;
        this.jdbc = jdbc;
    }

    @RabbitListener(queues = "mall.order.paid.queue")
    public void onPaid(OrderPaidEvent event) {
        String messageId = "order-paid-" + event.orderNo();
        if (!dedup.tryConsume("order-paid", messageId)) {
            log.info("[order.paid] 重复消息跳过 orderNo={}", event.orderNo());
            return;
        }
        try {
            grantPoints(event, messageId);
            grantCoupon(event);
            notify(event);
        } catch (Exception e) {
            dedup.fail(messageId);   // 失败回滚去重键，让重投的消息能重新处理
            throw e;
        }
    }

    private void grantPoints(OrderPaidEvent event, String messageId) {
        try {
            jdbc.update("""
                    INSERT INTO t_points_log (user_id, points, reason, message_id)
                    VALUES (:uid, :pts, '支付奖励', :mid)
                    """, new MapSqlParameterSource()
                    .addValue("uid", event.userId())
                    .addValue("pts", PAY_POINTS)
                    .addValue("mid", messageId));
        } catch (DuplicateKeyException e) {
            log.info("[积分] 唯一索引拦截重复发放 orderNo={}", event.orderNo());
            return;
        }
        // 账户累加（ON DUPLICATE KEY UPDATE：天然幂等的 UPSERT 写法）
        jdbc.update("""
                INSERT INTO t_points_account (user_id, points) VALUES (:uid, :pts)
                ON DUPLICATE KEY UPDATE points = points + :pts
                """, new MapSqlParameterSource()
                .addValue("uid", event.userId())
                .addValue("pts", PAY_POINTS));
        log.info("[积分] user={} +{}分", event.userId(), PAY_POINTS);
    }

    private void grantCoupon(OrderPaidEvent event) {
        try {
            jdbc.update("""
                    INSERT INTO t_coupon (user_id, coupon_type, activity) VALUES (:uid, 'NEW_USER', 'PAY_PROMO')
                    """, new MapSqlParameterSource("uid", event.userId()));
            log.info("[发券] user={} 新人券x1", event.userId());
        } catch (DuplicateKeyException e) {
            log.info("[发券] 业务唯一键拦截重复发放 user={}", event.userId());
        }
    }

    private void notify(OrderPaidEvent event) {
        // 真实场景：调短信/推送网关；这里日志模拟
        log.info("[通知] 短信+推送已发送 orderNo={} 金额={}分", event.orderNo(), event.amount());
    }
}
