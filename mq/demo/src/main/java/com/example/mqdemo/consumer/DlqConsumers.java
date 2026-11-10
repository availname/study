package com.example.mqdemo.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * stage3/05 死信链路演示：
 * demo.biz.queue 的消费者对 "boom-" 开头的消息必然抛异常
 * → 本地重试 3 次（yml 配置）→ 重试耗尽且不重回队列 → 死信进 demo.dlq → 死信消费者兜底。
 */
@Component
public class DlqConsumers {
    private static final Logger log = LoggerFactory.getLogger(DlqConsumers.class);

    /** 业务队列消费者：故意失败 */
    @RabbitListener(queues = "demo.biz.queue")
    public void bizConsumer(String msg) {
        if (msg.startsWith("boom-")) {
            log.warn("[业务] 处理失败，准备本地重试... msg={}", msg);
            throw new RuntimeException("模拟业务异常: " + msg);
        }
        log.info("[业务] 处理成功: {}", msg);
    }

    /** 死信队列消费者：重试耗尽的消息最终归宿 */
    @RabbitListener(queues = "demo.dlq")
    public void deadLetterConsumer(String msg) {
        log.error("[死信] 收到重试耗尽的消息，落库人工兜底: {}", msg);
        // 生产做法：INSERT 人工处理表 + 告警通知
    }
}
