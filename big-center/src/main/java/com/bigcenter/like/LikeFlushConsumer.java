package com.bigcenter.like;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 点赞刷库消费者：like.flushed 消息 → t_like_archive 异步归档（幂等在 LikeService.onFlushQueue） */
@Component
public class LikeFlushConsumer {
    private static final Logger log = LoggerFactory.getLogger(LikeFlushConsumer.class);

    private final LikeService likeService;

    public LikeFlushConsumer(LikeService likeService) {
        this.likeService = likeService;
    }

    @RabbitListener(queues = "bc.like.flush.queue")
    public void onFlush(Map<String, Object> msg) {
        long targetId = ((Number) msg.get("targetId")).longValue();
        long userId = ((Number) msg.get("userId")).longValue();
        long delta = ((Number) msg.get("delta")).longValue();
        String messageId = String.valueOf(msg.get("messageId"));
        try {
            likeService.onFlushQueue(targetId, userId, delta, messageId);
        } catch (Exception e) {
            log.error("[点赞刷库] 失败 targetId={} err={}", targetId, e.getMessage(), e);
            throw e;    // 本地重试 3 次 → 死信
        }
    }
}
