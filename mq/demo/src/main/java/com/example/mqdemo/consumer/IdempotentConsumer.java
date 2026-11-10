package com.example.mqdemo.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * stage3/02 幂等消费者。
 * 教学版用内存 Map 模拟 Redis setnx 去重；
 * 生产版换 Redis setIfAbsent(24h 过期)，并在数据库加唯一索引做最后防线（两层幂等）。
 */
@Component
public class IdempotentConsumer {
    private static final Logger log = LoggerFactory.getLogger(IdempotentConsumer.class);

    /** 模拟 Redis 去重键存储 */
    private static final Map<String, Long> DEDUP = new ConcurrentHashMap<>();

    @RabbitListener(queues = "idempotent.queue")
    public void handle(Map<String, Object> msg) {
        String messageId = String.valueOf(msg.get("messageId"));

        // 第一道闸：setnx 语义——已存在说明重复消息，直接跳过
        Long first = DEDUP.putIfAbsent(messageId, System.currentTimeMillis());
        if (first != null) {
            log.info("[幂等] 重复消息，跳过（第 N 次重复） messageId={}", messageId);
            return;   // 正常返回 = ack。重复消息也算"处理成功"
        }

        // 真正的业务（只执行一次）
        log.info("[幂等] 业务执行! messageId={}, amount={}", messageId, msg.get("amount"));
        // 生产里这里抛异常时要记得 DEDUP.remove(messageId) 回滚去重键（Redis 版同理）
    }

    /** 演示用：清空去重表 */
    public static void clear() { DEDUP.clear(); }
}
