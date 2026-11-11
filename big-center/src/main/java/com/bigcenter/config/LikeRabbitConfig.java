package com.bigcenter.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 点赞 MQ 拓扑：like.flushed → 刷库队列（点赞事件异步归档 t_like_archive，削峰） */
@Configuration
public class LikeRabbitConfig {

    @Bean
    public TopicExchange bcLikeExchange() { return new TopicExchange("bc.like", true, false); }

    @Bean
    public Queue bcLikeFlushQueue() {
        return QueueBuilder.durable("bc.like.flush.queue")
                .deadLetterExchange("bc.dlx").deadLetterRoutingKey("bc.dead").build();
    }

    @Bean
    public Binding likeFlushBinding() {
        return BindingBuilder.bind(bcLikeFlushQueue()).to(bcLikeExchange()).with("like.flushed");
    }
}
