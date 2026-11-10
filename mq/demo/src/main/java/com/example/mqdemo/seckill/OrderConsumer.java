package com.example.mqdemo.seckill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * stage4/01 秒杀：MQ 消费端（削峰的"放水口"）。
 * 按 Semaphore 限速模拟 DB 只能承受固定速率——演示"洪峰进队列、匀速出队列"。
 */
@Component
public class OrderConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    /** 模拟 DB 订单表（幂等：orderId 唯一） */
    public static final Map<Long, Boolean> ORDER_TABLE = new ConcurrentHashMap<>();
    /** 模拟 DB 每笔订单耗时（ms），代表 DB 峰值能力 */
    static final int DB_COST_MS = 2;

    @RabbitListener(queues = "seckill.order.queue")
    public void createOrder(SeckillMsg msg) throws InterruptedException {
        Thread.sleep(DB_COST_MS);   // 模拟 DB 写入耗时

        // 幂等落库（生产：订单号唯一索引，DuplicateKeyException 直接忽略）
        ORDER_TABLE.putIfAbsent(msg.getOrderId(), true);
        log.info("[秒杀-消费] 订单已落库 orderId={}，当前已落库 {} 单",
                msg.getOrderId(), ORDER_TABLE.size());
    }
}
