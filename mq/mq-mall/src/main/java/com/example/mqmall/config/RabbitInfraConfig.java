package com.example.mqmall.config;

import com.example.mqmall.messaging.SendRecordDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * RabbitMQ 基础设施：消息转换器、带 Confirm/Return 的 RabbitTemplate、秒杀专用消费容器。
 * 对应教程 stage2/03（Confirm/Return 回调落到本地消息表状态）。
 */
@Configuration
public class RabbitInfraConfig {

    /** 生产必配：Jackson JSON 序列化 */
    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper om) {
        return new Jackson2JsonMessageConverter(om);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf,
                                         Jackson2JsonMessageConverter converter,
                                         SendRecordDao sendRecordDao) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        template.setMandatory(true);   // 路由不到队列必须退回（配合 publisher-returns）

        // Confirm：ack=false 说明消息没到 Broker —— 标记本地消息表为失败，等补偿任务重发
        template.setConfirmCallback((data, ack, cause) -> {
            if (data == null) return;
            if (ack) {
                sendRecordDao.markConfirmed(data.getId());
            } else {
                sendRecordDao.markFailed(data.getId(), "BROKER_NACK: " + cause);
            }
        });

        // Return：消息到了 Broker 但路由不到任何队列 —— 标记失败，补偿任务会重发
        template.setReturnsCallback(returned -> {
            String mid = returned.getMessage().getMessageProperties().getMessageId();
            sendRecordDao.markFailed(mid, "UNROUTABLE: " + returned.getExchange()
                    + "/" + returned.getRoutingKey() + " " + returned.getReplyText());
        });
        return template;
    }

    /** 秒杀消费容器：4 并发 + 大 prefetch（削峰放水口），自带独立重试模板（stage3/04 治本） */
    @Bean
    public SimpleRabbitListenerContainerFactory seckillFactory(ConnectionFactory cf,
                                                               Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(4);
        factory.setPrefetchCount(20);

        RetryTemplate retry = new RetryTemplate();
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(3);
        ExponentialBackOffPolicy backoff = new ExponentialBackOffPolicy();
        backoff.setInitialInterval(500);
        backoff.setMultiplier(2);
        backoff.setMaxInterval(5000);
        retry.setRetryPolicy(policy);
        retry.setBackOffPolicy(backoff);
        factory.setRetryTemplate(retry);
        factory.setDefaultRequeueRejected(false);   // 重试耗尽 → 死信，不重回队列
        return factory;
    }
}
