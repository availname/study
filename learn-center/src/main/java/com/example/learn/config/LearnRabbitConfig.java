package com.example.learn.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * learn-center MQ 拓扑：
 * ① lc.enroll 交换机：抢课消息 → 异步建单；下单延迟取消链（TTL→死信→超时队列）
 * ② lc.dlx → lc.dlq：失败兜底
 */
@Configuration
public class LearnRabbitConfig {

    @Bean
    public TopicExchange lcEnrollExchange() { return new TopicExchange("lc.enroll", true, false); }

    @Bean
    public DirectExchange lcOrderTimeoutExchange() { return new DirectExchange("lc.order.timeout.exchange", true, false); }

    @Bean
    public DirectExchange lcDlx() { return new DirectExchange("lc.dlx", true, false); }

    @Bean
    public Queue lcEnrollCreatedQueue() {
        return QueueBuilder.durable("lc.enroll.created.queue")
                .deadLetterExchange("lc.dlx").deadLetterRoutingKey("lc.dead").build();
    }

    /** 延迟取消队列：TTL 到期 → 死信转发到超时队列（TTL+DLX 方案，教程 stage3/05） */
    @Bean
    public Queue lcOrderDelayQueue() {
        return QueueBuilder.durable("lc.order.delay.queue")
                .ttl(120_000)
                .deadLetterExchange("lc.order.timeout.exchange")
                .deadLetterRoutingKey("lc.order.timeout")
                .build();
    }

    @Bean
    public Queue lcOrderTimeoutQueue() {
        return QueueBuilder.durable("lc.order.timeout.queue")
                .deadLetterExchange("lc.dlx").deadLetterRoutingKey("lc.dead").build();
    }

    @Bean
    public Queue lcDlqQueue() { return QueueBuilder.durable("lc.dlq.queue").build(); }

    @Bean
    public Binding enrollCreatedBinding() {
        return BindingBuilder.bind(lcEnrollCreatedQueue()).to(lcEnrollExchange()).with("enroll.created");
    }

    @Bean
    public Binding enrollDelayBinding() {
        return BindingBuilder.bind(lcOrderDelayQueue()).to(lcEnrollExchange()).with("enroll.delay");
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(lcOrderTimeoutQueue()).to(lcOrderTimeoutExchange()).with("lc.order.timeout");
    }

    @Bean
    public Binding lcDlqBinding() {
        return BindingBuilder.bind(lcDlqQueue()).to(lcDlx()).with("lc.dead");
    }
}
