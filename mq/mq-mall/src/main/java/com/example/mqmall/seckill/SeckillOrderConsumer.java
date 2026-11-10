package com.example.mqmall.seckill;

import com.example.mqmall.messaging.MessageDedupService;
import com.example.mqmall.messaging.Messages.SeckillOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 秒杀消费端 = 削峰的"放水口"（教程 stage4/01）。
 * 4 并发 × (3ms DB 模拟) ≈ 千级单/秒匀速落库，DB 只承受这个速率，洪峰在队列排队。
 * 事务边界在 {@link SeckillOrderTxWorker}（独立 bean，避免 @Transactional 自调用代理失效）。
 * 失败路径：回滚去重键 + 归还 Redis 预扣 → 本地重试 3 次 → 死信落库（stage3/05）。
 */
@Component
public class SeckillOrderConsumer {
    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    private final SeckillOrderTxWorker worker;
    private final MessageDedupService dedup;
    private final SeckillService seckillService;
    private final StringRedisTemplate redis;

    public SeckillOrderConsumer(SeckillOrderTxWorker worker,
                                MessageDedupService dedup,
                                SeckillService seckillService,
                                StringRedisTemplate redis) {
        this.worker = worker;
        this.dedup = dedup;
        this.seckillService = seckillService;
        this.redis = redis;
    }

    @RabbitListener(queues = "mall.seckill.order.queue", containerFactory = "seckillFactory")
    public void createOrder(SeckillOrderRequest req) throws InterruptedException {
        String messageId = "seckill-" + req.orderId();
        if (!dedup.tryConsume("seckill-order", messageId)) {
            return;   // 重投的重复请求（结果已处理过），跳过
        }
        try {
            worker.doCreate(req);
        } catch (Exception e) {
            dedup.fail(messageId);                                            // 让重试能重新处理
            redis.opsForValue().increment("seckill:stock:" + req.skuId());    // 归还预扣
            redis.opsForSet().remove("seckill:users:" + req.skuId(),
                    String.valueOf(req.userId()));
            seckillService.markResult(req.orderId(), "FAILED", "系统异常: " + e.getMessage());
            throw e;   // 本地重试 3 次 → 耗尽进死信落库
        }
    }
}
