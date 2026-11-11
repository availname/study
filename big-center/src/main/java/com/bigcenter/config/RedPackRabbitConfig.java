package com.bigcenter.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 红包 MQ 拓扑（TTL+DLX 延迟退款链，mq 教学 stage3/05 方案）：
 * 发红包 → 发 24h TTL 消息进 bc.redpack.delay.queue（无消费者）→ 到期死信转发
 * → bc.redpack.refund.queue → RedPackRefundConsumer 执行退款。
 */
@Configuration
public class RedPackRabbitConfig {

    @Bean
    public DirectExchange bcRedpackRefundExchange() {
        return new DirectExchange("bc.redpack.refund.exchange", true, false);
    }

    /** 延迟队列：不设消费者，TTL 到期死信转发到退款队列 */
    @Bean
    public Queue bcRedpackDelayQueue() {
        return QueueBuilder.durable("bc.redpack.delay.queue")
                .ttl((int) java.time.Duration.ofHours(24).toMillis())
                .deadLetterExchange("bc.redpack.refund.exchange")
                .deadLetterRoutingKey("redpack.refund")
                .build();
    }

    @Bean
    public Queue bcRedpackRefundQueue() {
        return QueueBuilder.durable("bc.redpack.refund.queue")
                .deadLetterExchange("bc.dlx").deadLetterRoutingKey("bc.dead").build();
    }

    @Bean
    public Binding redpackDelayBinding() {
        // 发消息直接进延迟队列（默认交换机路由 key=队列名），此处声明仅供死信转发的交换机绑定关系
        return new Binding("bc.redpack.delay.queue", Binding.DestinationType.QUEUE,
                "bc.redpack.refund.exchange", "redpack.delay", null);
    }

    @Bean
    public Binding redpackRefundBinding() {
        return BindingBuilder.bind(bcRedpackRefundQueue())
                .to(bcRedpackRefundExchange()).with("redpack.refund");
    }
}
