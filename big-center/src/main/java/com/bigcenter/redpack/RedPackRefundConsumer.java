package com.bigcenter.redpack;

import com.bigcenter.messaging.MessageDedupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 红包过期退款消费者（24h TTL+DLX 延迟链终点）：
 * 消费幂等两层防线（MessageDedupService）挡重投 + refundIfExpired 内部 CAS 状态机天然幂等。
 * 退款消息发支付中心走可靠消息门面（本地消息表，事务内原子）。
 */
@Component
public class RedPackRefundConsumer {
    private static final Logger log = LoggerFactory.getLogger(RedPackRefundConsumer.class);

    private final RedPackService redPackService;
    private final MessageDedupService dedup;

    public RedPackRefundConsumer(RedPackService redPackService, MessageDedupService dedup) {
        this.redPackService = redPackService;
        this.dedup = dedup;
    }

    @RabbitListener(queues = "bc.redpack.refund.queue")
    public void onRefund(Map<String, Object> msg) {
        long packId = ((Number) msg.get("packId")).longValue();
        String messageId = "redpack-refund-" + packId;   // 同一红包只有一条退款消息（幂等键）
        if (!dedup.tryConsume("redpack-refund", messageId)) {
            log.info("[退款] 重复消息跳过 packId={}", packId);
            return;
        }
        try {
            boolean refunded = redPackService.refundIfExpired(packId);
            log.info("[退款] packId={} 处理完成 refunded={}", packId, refunded);
        } catch (Exception e) {
            dedup.fail(messageId);   // 撤去重键：重投才能重新处理
            throw e;                 // 本地重试 3 次 → bc.dlx 死信落库
        }
    }
}
