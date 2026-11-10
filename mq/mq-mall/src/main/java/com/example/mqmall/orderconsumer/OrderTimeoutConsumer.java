package com.example.mqmall.orderconsumer;

import com.example.mqmall.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 超时关单消费者（教程 stage4/02）。
 * 消息在 order.delay.queue 里 TTL 沉睡（演示 2 分钟）→ 死信转发到本队列。
 * 与扫表兜底共用 OrderService.closeIfTimeout（CAS 幂等，与支付回调并发互斥）。
 */
@Component
public class OrderTimeoutConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutConsumer.class);

    private final OrderService orderService;

    public OrderTimeoutConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = "mall.order.timeout.queue")
    public void onTimeout(String orderNo) {
        boolean closed = orderService.closeIfTimeout(orderNo);
        if (closed) {
            log.info("[延迟关单] {} 超时未支付已自动关闭", orderNo);
        } else {
            log.info("[延迟关单] {} 无需关闭（已支付或已关闭），幂等跳过", orderNo);
        }
    }
}
