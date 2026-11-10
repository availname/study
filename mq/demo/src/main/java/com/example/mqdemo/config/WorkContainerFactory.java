package com.example.mqdemo.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * stage1/04：公平分发的容器工厂。
 * prefetch=1 → 消费者处理完（ack）才拿下一条，"能者多劳"。
 * 默认容器工厂走 application.yml 的全局配置（prefetch=10 + 本地重试）。
 */
@Configuration
public class WorkContainerFactory {

    @Bean
    public SimpleRabbitListenerContainerFactory workFactory(ConnectionFactory cf,
                                                            Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(converter);
        factory.setPrefetchCount(1);     // 公平分发的关键
        return factory;
    }
}
