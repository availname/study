package com.bigcenter.config;

import com.bigcenter.messaging.SendRecordDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 基础设施（mq-mall RabbitInfraConfig 总装版）：
 * Jackson 消息转换 + Confirm/Return 回调联动本地消息表（stage3/01 闭环的"确认"半边）。
 */
@Configuration
public class RabbitInfraConfig {

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

        // Confirm：ack=false 说明消息没到 Broker——标失败，等补偿任务重发
        template.setConfirmCallback((data, ack, cause) -> {
            if (data == null) return;
            if (ack) {
                sendRecordDao.markConfirmed(data.getId());
            } else {
                sendRecordDao.markFailed(data.getId(), "BROKER_NACK: " + cause);
            }
        });

        // Return：消息到了 Broker 但路由不到任何队列——标失败，补偿任务会重发
        template.setReturnsCallback(returned -> {
            String mid = returned.getMessage().getMessageProperties().getMessageId();
            sendRecordDao.markFailed(mid, "UNROUTABLE: " + returned.getExchange()
                    + "/" + returned.getRoutingKey() + " " + returned.getReplyText());
        });
        return template;
    }
}
