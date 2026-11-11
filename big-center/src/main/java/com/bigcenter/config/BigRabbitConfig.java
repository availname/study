package com.bigcenter.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * big-center MQ 拓扑（learn-center LearnRabbitConfig 总装版）：
 * ① bc.seckill 交换机：秒杀预扣成功 → 异步建单（削峰）
 * ② bc.dlx → bc.dlq：消费失败兜底（本地重试 3 次耗尽后落死信）
 */
@Configuration
public class BigRabbitConfig {

    @Bean
    public TopicExchange bcSeckillExchange() { return new TopicExchange("bc.seckill", true, false); }

    @Bean
    public DirectExchange bcDlx() { return new DirectExchange("bc.dlx", true, false); }

    @Bean
    public Queue bcSeckillCreatedQueue() {
        return QueueBuilder.durable("bc.seckill.created.queue")
                .deadLetterExchange("bc.dlx").deadLetterRoutingKey("bc.dead").build();
    }

    @Bean
    public Queue bcDlqQueue() { return QueueBuilder.durable("bc.dlq.queue").build(); }

    @Bean
    public Binding seckillCreatedBinding() {
        return BindingBuilder.bind(bcSeckillCreatedQueue()).to(bcSeckillExchange()).with("seckill.created");
    }

    @Bean
    public Binding bcDlqBinding() {
        return BindingBuilder.bind(bcDlqQueue()).to(bcDlx()).with("bc.dead");
    }
}
