package com.bigcenter.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 帖子审核管道拓扑：bc.post 交换机 → 审核队列（AI 审核→通过/拒绝/人工卡点） */
@Configuration
public class PostRabbitConfig {

    @Bean
    public TopicExchange bcPostExchange() { return new TopicExchange("bc.post", true, false); }

    @Bean
    public Queue bcPostSubmittedQueue() {
        return QueueBuilder.durable("bc.post.submitted.queue")
                .deadLetterExchange("bc.dlx").deadLetterRoutingKey("bc.dead").build();
    }

    @Bean
    public Binding postSubmittedBinding() {
        return BindingBuilder.bind(bcPostSubmittedQueue()).to(bcPostExchange()).with("post.submitted");
    }
}
