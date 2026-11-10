package com.example.mqmall.seckill;

import com.example.mqmall.messaging.Messages.SeckillOrderRequest;
import com.example.mqmall.order.OrderDao;
import com.example.mqmall.order.StockDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 秒杀建单事务边界（独立 bean —— @Transactional 自调用不会走代理，必须拆出来）。
 * 事务内容：建单 + DB 扣库存（同事务原子）。
 */
@Service
public class SeckillOrderTxWorker {
    private static final Logger log = LoggerFactory.getLogger(SeckillOrderTxWorker.class);
    private static final int DB_COST_MS = 3;   // 模拟 DB 单笔写入耗时（削峰演示关键参数）

    private final OrderDao orderDao;
    private final StockDao stockDao;
    private final SeckillService seckillService;
    private final StringRedisTemplate redis;

    public SeckillOrderTxWorker(OrderDao orderDao, StockDao stockDao,
                                SeckillService seckillService, StringRedisTemplate redis) {
        this.orderDao = orderDao;
        this.stockDao = stockDao;
        this.seckillService = seckillService;
        this.redis = redis;
    }

    @Transactional(rollbackFor = Exception.class)
    public void doCreate(SeckillOrderRequest req) throws InterruptedException {
        Thread.sleep(DB_COST_MS);   // 模拟 DB 写入耗时

        try {
            // ① 建单：(user,sku,seckill_flag) 唯一索引 = 一人一单 DB 兜底 + 幂等
            orderDao.insert("S" + req.orderId(), req.userId(), req.skuId(), 1, 100, 1);
        } catch (DuplicateKeyException e) {
            seckillService.markResult(req.orderId(), "FAILED", "一人一单拦截");
            log.warn("[秒杀消费] user={} 一人一单拦截 orderId={}", req.userId(), req.orderId());
            restoreRedis(req);   // 预扣作废，归还库存
            return;
        }

        // ② DB 层扣库存：防超卖的最后防线（0 行受影响 = Redis/DB 漂移）
        if (stockDao.lock(req.skuId(), 1) == 0) {
            seckillService.markResult(req.orderId(), "FAILED", "DB库存不足，预扣已归还");
            log.error("[秒杀消费] DB库存不足！Redis/DB漂移 orderId={} 已归还预扣", req.orderId());
            restoreRedis(req);
            return;
        }
        stockDao.log("S" + req.orderId(), req.skuId(), 1, "LOCK");   // 流水完整，对账用

        seckillService.markResult(req.orderId(), "SUCCESS", null);
        if (req.orderId() % 200 == 0) {
            log.info("[秒杀消费] 进度：已处理到 orderId={}", req.orderId());
        }
    }

    /** 归还预扣库存（教学演示；生产用 Lua 归还脚本保证原子 + 状态位防重复归还） */
    private void restoreRedis(SeckillOrderRequest req) {
        redis.opsForValue().increment("seckill:stock:" + req.skuId());
        redis.opsForSet().remove("seckill:users:" + req.skuId(), String.valueOf(req.userId()));
    }
}
