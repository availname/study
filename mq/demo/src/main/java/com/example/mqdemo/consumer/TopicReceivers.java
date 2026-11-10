package com.example.mqdemo.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** stage1/04 Topic：两个队列两种绑定，观察通配符路由结果 */
@Component
public class TopicReceivers {
    private static final Logger log = LoggerFactory.getLogger(TopicReceivers.class);

    /** 绑定 order.*：只匹配 order.一个词 */
    @RabbitListener(queues = "order.star.queue")
    public void onOrderStar(String msg) {
        log.info("[Topic order.*] 收到: {}", msg);
    }

    /** 绑定 order.#：匹配 order. 开头的所有词 */
    @RabbitListener(queues = "order.hash.queue")
    public void onOrderHash(String msg) {
        log.info("[Topic order.#] 收到: {}", msg);
    }
}
