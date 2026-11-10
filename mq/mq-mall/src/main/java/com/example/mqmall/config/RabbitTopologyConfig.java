package com.example.mqmall.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑总声明（幂等：重复启动安全）。
 * 对应教程 stage1/04（拓扑）、stage3/05（死信+延迟）。
 */
@Configuration
public class RabbitTopologyConfig {

    /** 订单超时时间（延迟队列 TTL 与扫表兜底共用，见 application.yml） */
    @Value("${mq.mall.order-timeout-ms:120000}")
    private long orderTimeoutMs;

    // ==================== 交换机 ====================

    /** 订单业务事件交换机（Topic，业务事件总线模式） */
    @Bean
    public TopicExchange mallOrderExchange() { return new TopicExchange("mall.order", true, false); }

    /** 秒杀建单交换机 */
    @Bean
    public DirectExchange seckillExchange() { return new DirectExchange("mall.seckill", true, false); }

    /** 通用死信交换机：所有业务队列的重试耗尽消息都到这里（stage3/05） */
    @Bean
    public DirectExchange dlxExchange() { return new DirectExchange("dlx.exchange", true, false); }

    /** 订单超时交换机：延迟队列 TTL 到期死信转发到这里（stage3/05 方案一） */
    @Bean
    public DirectExchange orderTimeoutExchange() { return new DirectExchange("order.timeout.exchange", true, false); }

    // ==================== 队列 ====================

    private Queue bizQueue(String name) {
        // 生产可换 Quorum Queue：QueueBuilder.durable(name).quorum()（教程 stage2/02）
        return QueueBuilder.durable(name)
                .deadLetterExchange("dlx.exchange")
                .deadLetterRoutingKey("mall.dead")
                .build();
    }

    @Bean
    public Queue orderCreatedQueue() { return bizQueue("mall.order.created.queue"); }

    @Bean
    public Queue orderPaidQueue() { return bizQueue("mall.order.paid.queue"); }

    /**
     * 延迟队列：没有消费者，消息靠 x-message-ttl "沉睡"，
     * 到期死信转发到 order.timeout.exchange → order.timeout.queue。
     * 生产任意延迟建议换 rabbitmq-delayed-message-exchange 插件或 RocketMQ 延迟消息（stage3/05）。
     */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable("order.delay.queue")
                .ttl((int) orderTimeoutMs)
                .deadLetterExchange("order.timeout.exchange")
                .deadLetterRoutingKey("order.timeout")
                .build();
    }

    @Bean
    public Queue orderTimeoutQueue() { return QueueBuilder.durable("mall.order.timeout.queue").build(); }

    /** 秒杀建单队列：可预见大堆积 → 惰性队列（stage2/02）；高可用可叠加 .quorum() */
    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable("mall.seckill.order.queue")
                .lazy()
                .deadLetterExchange("dlx.exchange")
                .deadLetterRoutingKey("mall.dead")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() { return QueueBuilder.durable("mall.dlq.queue").build(); }

    // ==================== 绑定 ====================

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(mallOrderExchange()).with("order.created");
    }

    @Bean
    public Binding orderPaidBinding() {
        return BindingBuilder.bind(orderPaidQueue()).to(mallOrderExchange()).with("order.paid");
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillOrderQueue()).to(seckillExchange()).with("seckill.order");
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(orderTimeoutExchange()).with("order.timeout");
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(dlxExchange()).with("mall.dead");
    }
}
