package com.example.mqdemo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 拓扑声明 + 消息转换器 + 生产端可靠性回调。
 * 对应教程：stage1/04（拓扑）、stage2/03（Confirm/Return）、stage3/05（死信/延迟）。
 */
@Configuration
public class RabbitConfig {

    /** 生产必配：Jackson JSON 序列化（默认 JDK 序列化又慢又不可读） */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** 生产端可靠性：Confirm + Return 回调（日志演示，生产里要记补偿表） */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);

        // Confirm：消息是否到达 Broker
        template.setConfirmCallback((data, ack, cause) -> {
            String id = data != null ? data.getId() : "?";
            if (ack) {
                System.out.println("[Confirm] 已到达 broker, id=" + id);
            } else {
                System.err.println("[Confirm] 未到达 broker! id=" + id + ", cause=" + cause);
                // 生产做法：标记补偿表，定时任务重发
            }
        });
        // Return：到了 Broker 但路由不到任何队列，被退回
        template.setReturnsCallback(returned ->
                System.err.println("[Return] 消息被退回! exchange=" + returned.getExchange()
                        + ", routingKey=" + returned.getRoutingKey()
                        + ", replyText=" + returned.getReplyText()
                        + ", body=" + new String(returned.getMessage().getBody())));
        return template;
    }

    // ==================== Hello World / Topic ====================

    @Bean
    public Queue helloQueue() {
        return QueueBuilder.durable("hello.queue").build();
    }

    /** Topic 业务事件交换机：绑定两种监听方式，配合 /topic 接口验证 */
    @Bean
    public TopicExchange bizTopic() {
        return new TopicExchange("biz.topic");
    }

    @Bean
    public Queue orderStarQueue() { return QueueBuilder.durable("order.star.queue").build(); }

    @Bean
    public Queue orderHashQueue() { return QueueBuilder.durable("order.hash.queue").build(); }

    @Bean
    public Binding orderStarBinding() {
        return BindingBuilder.bind(orderStarQueue()).to(bizTopic()).with("order.*");
    }

    @Bean
    public Binding orderHashBinding() {
        return BindingBuilder.bind(orderHashQueue()).to(bizTopic()).with("order.#");
    }

    // ==================== 死信（stage3/05） ====================
    // 普通业务队列：消费失败(重试耗尽) 的消息死信到这里

    @Bean
    public Queue demoBizQueue() {
        return QueueBuilder.durable("demo.biz.queue")
                .deadLetterExchange("dlx.exchange")
                .deadLetterRoutingKey("demo.dead")
                .build();
    }

    @Bean
    public DirectExchange dlxExchange() { return new DirectExchange("dlx.exchange"); }

    @Bean
    public Queue demoDlq() { return QueueBuilder.durable("demo.dlq").build(); }

    @Bean
    public Binding demoDlqBinding() {
        return BindingBuilder.bind(demoDlq()).to(dlxExchange()).with("demo.dead");
    }

    // ==================== 延迟消息：TTL + DLX（stage3/05） ====================
    // 无消费者的 TTL 队列，5 秒后过期 → 死信到 order.close.queue
    // （教学用固定 TTL 演示机制；任意延迟用 rabbitmq-delayed-message-exchange 插件或 RocketMQ）

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable("order.delay.queue")
                .ttl(5000)                              // x-message-ttl: 5s（教学缩短，真实是 30min）
                .deadLetterExchange("order.close.exchange")
                .deadLetterRoutingKey("order.close")
                .build();
    }

    @Bean
    public DirectExchange orderCloseExchange() { return new DirectExchange("order.close.exchange"); }

    @Bean
    public Queue orderCloseQueue() { return QueueBuilder.durable("order.close.queue").build(); }

    @Bean
    public Binding orderCloseBinding() {
        return BindingBuilder.bind(orderCloseQueue()).to(orderCloseExchange()).with("order.close");
    }

    // ==================== 秒杀（stage4/01） ====================

    @Bean
    public DirectExchange seckillExchange() { return new DirectExchange("seckill.exchange"); }

    @Bean
    public Queue seckillOrderQueue() { return QueueBuilder.durable("seckill.order.queue").build(); }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillOrderQueue()).to(seckillExchange()).with("order.create");
    }
}
