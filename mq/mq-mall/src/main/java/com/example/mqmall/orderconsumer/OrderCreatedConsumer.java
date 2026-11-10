package com.example.mqmall.orderconsumer;

import com.example.mqmall.messaging.MessageDedupService;
import com.example.mqmall.messaging.Messages.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * order.created 消费者（教程 stage3/02 幂等消费演示）。
 * 代表"审计/缓存重建/推荐画像"这类下游：重复处理无意义但无害。
 */
@Component
public class OrderCreatedConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final MessageDedupService dedup;

    public OrderCreatedConsumer(MessageDedupService dedup) {
        this.dedup = dedup;
    }

    @RabbitListener(queues = "mall.order.created.queue")
    public void onCreated(OrderCreatedEvent event) {
        // 测试入口：故意失败的消息 → 本地重试 3 次 → 死信落库（stage3/05）
        if (event.failOnPurpose()) {
            log.warn("[order.created] 故意失败，演示重试与死信链路 orderNo={}", event.orderNo());
            throw new RuntimeException("故意失败: " + event.orderNo());
        }

        String messageId = "order-created-" + event.orderNo();
        if (!dedup.tryConsume("order-created", messageId)) {
            log.info("[order.created] 重复消息跳过 orderNo={}", event.orderNo());
            return;
        }
        // 真实场景：重建订单缓存、刷新推荐画像、写入审计流水……
        log.info("[order.created] 审计/缓存重建完成 orderNo={} userId={}", event.orderNo(), event.userId());
    }
}
