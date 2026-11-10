package com.example.community.config;

import com.example.community.messaging.SendRecordDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 拓扑 + 基础设施（Confirm/Return 接本地消息表）。
 * 模式与 mq-mall 完全同构——可靠性基建是可复制的"模式"，不是一次性代码。
 */
@Configuration
public class RabbitConfig {

    @Value("${community.post-daily-limit:5}")
    private int postDailyLimit;

    // ==================== 拓扑 ====================

    /** 帖子事件交换机（Topic）：审核管道的串联 */
    @Bean
    public TopicExchange postExchange() { return new TopicExchange("community.post", true, false); }

    /** 发布完成广播（Fanout）：Feed 推送 / 通知 */
    @Bean
    public FanoutExchange publishedFanout() { return new FanoutExchange("community.published.fanout", true, false); }

    /** 通用死信交换机 */
    @Bean
    public DirectExchange dlxExchange() { return new DirectExchange("community.dlx", true, false); }

    private Queue bizQueue(String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange("community.dlx")
                .deadLetterRoutingKey("community.dead")
                .build();
    }

    @Bean public Queue postSubmittedQueue()  { return bizQueue("community.post.submitted.queue"); }
    @Bean public Queue postFeedQueue()       { return bizQueue("community.post.feed.queue"); }
    @Bean public Queue postNotifyQueue()     { return bizQueue("community.post.notify.queue"); }
    @Bean public Queue deadLetterQueue()     { return QueueBuilder.durable("community.dlq.queue").build(); }

    @Bean public Binding submittedBinding() { return BindingBuilder.bind(postSubmittedQueue()).to(postExchange()).with("post.submitted"); }
    @Bean public Binding feedBinding()      { return BindingBuilder.bind(postFeedQueue()).to(publishedFanout()); }
    @Bean public Binding notifyBinding()    { return BindingBuilder.bind(postNotifyQueue()).to(publishedFanout()); }
    @Bean public Binding dlqBinding()       { return BindingBuilder.bind(deadLetterQueue()).to(dlxExchange()).with("community.dead"); }

    // ==================== 基础设施 ====================

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper om) {
        return new Jackson2JsonMessageConverter(om);
    }

    @Bean
    public Map<String, Object> configFlags() { return new HashMap<>(); }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter converter,
                                         SendRecordDao sendRecordDao) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        template.setConfirmCallback((data, ack, cause) -> {
            if (data == null) return;
            if (ack) sendRecordDao.markConfirmed(data.getId());
            else sendRecordDao.markFailed(data.getId(), "BROKER_NACK: " + cause);
        });
        template.setReturnsCallback(returned -> {
            String mid = returned.getMessage().getMessageProperties().getMessageId();
            sendRecordDao.markFailed(mid, "UNROUTABLE: " + returned.getRoutingKey() + " " + returned.getReplyText());
        });
        return template;
    }

    /** 审核管道慢消费者（sleep 模拟 AI），独立容器：prefetch=1 公平处理 */
    @Bean
    public SimpleRabbitListenerContainerFactory auditFactory(ConnectionFactory cf,
                                                             Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(converter);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
