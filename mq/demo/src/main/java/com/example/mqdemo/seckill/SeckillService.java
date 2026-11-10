package com.example.mqdemo.seckill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * stage4/01 秒杀接入层（教学版）：
 * 用 AtomicInteger 模拟 Redis Lua 预扣库存，抢到的请求发 MQ 异步落库。
 * 接口立即返回"排队中"——洪峰被队列吸走，DB 按消费速率匀速建单。
 */
@Service
public class SeckillService {
    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    private static final int TOTAL_STOCK = 1000;

    /** 模拟 Redis 库存（生产：Lua 脚本 get+decr 原子操作） */
    private final AtomicInteger stock = new AtomicInteger(TOTAL_STOCK);
    private final AtomicLong orderIdGen = new AtomicLong(0);
    private long seckillStartAt = 0;

    private final RabbitTemplate rabbit;

    public SeckillService(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    /** POST /seckill?count=N：模拟 N 个并发用户同时抢 */
    public Map<String, Object> seckill(int count) {
        int soldNow = 0;
        seckillStartAt = System.currentTimeMillis();

        long t0 = System.nanoTime();
        for (int i = 0; i < count; i++) {
            // ===== Redis 预扣库存（Lua 原子版的内存等价物）=====
            // AtomicInteger.decrementAndGet 会把库存扣成负数（超卖），
            // 所以用 CAS 循环，严格等价于 Lua 的 "if stock<=0 then return 0 end; decr"
            int s;
            do {
                s = stock.get();
                if (s <= 0) break;
            } while (!stock.compareAndSet(s, s - 1));
            if (s <= 0) continue;   // 售罄，直接拒绝

            // ===== 预扣成功 → 发 MQ，异步建单 =====
            long orderId = orderIdGen.incrementAndGet();
            SeckillMsg msg = new SeckillMsg(orderId, 10000L + orderId, 1L);
            rabbit.convertAndSend("seckill.exchange", "order.create", msg);
            soldNow++;
        }
        long costMs = (System.nanoTime() - t0) / 1_000_000;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("请求总数", count);
        result.put("抢购成功(已入队)", soldNow);
        result.put("已售罄拒绝", count - soldNow);
        result.put("剩余库存", stock.get());
        result.put("接入层耗时ms", costMs);   // 会远小于 count×DB耗时——这就是削峰
        result.put("提示", "订单正在由消费者按 DB 可承受速率落库，稍后查 /seckill/stats");
        return result;
    }

    /** GET /seckill/stats：轮询结果（模拟前端轮询订单状态） */
    public Map<String, Object> stats() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("库存总量", TOTAL_STOCK);
        r.put("Redis预扣成功", TOTAL_STOCK - stock.get());
        r.put("DB已落库订单", OrderConsumer.ORDER_TABLE.size());
        r.put("队列未消化(预扣-落库)", TOTAL_STOCK - stock.get() - OrderConsumer.ORDER_TABLE.size());
        r.put("超卖了吗", (OrderConsumer.ORDER_TABLE.size() > TOTAL_STOCK) ? "是!" : "否");
        return r;
    }
}
