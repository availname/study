package com.example.mqdemo.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * stage3/05 + stage4/02 延迟消息消费：订单超时检查。
 * 消息先在 order.delay.queue(TTL 5s) 里"沉睡"，过期死信转发到 order.close.queue 才被消费。
 */
@Component
public class OrderCloseConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderCloseConsumer.class);

    @RabbitListener(queues = "order.close.queue")
    public void onOrderTimeout(String msg) {
        long receiveAt = System.currentTimeMillis();
        log.info("[延迟消费] 收到超时订单消息: {}（此刻距发送已 5 秒）", msg);
        // 真实逻辑：查订单 → 仍是待支付则 CAS 关单（UPDATE ... WHERE status=10）→ 释放库存
        // 详见教程 stage4/02：关单与支付回调的并发竞态靠状态机 CAS 解决
    }
}
