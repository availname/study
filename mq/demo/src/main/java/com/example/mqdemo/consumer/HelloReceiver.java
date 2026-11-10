package com.example.mqdemo.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** stage1/03 Hello World：最简单的消费者 */
@Component
public class HelloReceiver {
    private static final Logger log = LoggerFactory.getLogger(HelloReceiver.class);

    @RabbitListener(queues = "hello.queue")
    public void receive(String msg) {
        log.info("[Hello] 收到: {}", msg);
    }
}
