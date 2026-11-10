package com.example.mqmall.seckill;

import com.example.mqmall.common.Ids;
import com.example.mqmall.messaging.Messages.SeckillOrderRequest;
import com.example.mqmall.messaging.ReliableMessagePublisher;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 秒杀接入层（教程 stage4/01）。
 * 流量漏斗：Lua 原子预扣（判断+扣减+一人一单）→ 抢到的发 MQ → 消费者匀速落库。
 * 接口立即返回，用户轮询 /result/{orderId} 获取结果。
 */
@Service
public class SeckillService {
    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    /** 订单结果：QUEUED(排队中) → SUCCESS / FAILED(原因) */
    public record SeckillResult(String status, String reason) {
        static SeckillResult queued() { return new SeckillResult("QUEUED", null); }
    }

    private final StringRedisTemplate redis;
    private final ReliableMessagePublisher publisher;
    private final DefaultRedisScript<Long> seckillScript;
    private final long initStock;

    /**
     * 教学版：结果放内存（生产放 Redis/DB，重启可恢复）。
     * ⚠ 订单号必须用全局唯一发号器（Ids.nextId）：不能用内存计数器——
     *   应用重启后计数器归零，与 Redis 里 24h 内的幂等去重键撞号，
     *   新订单会被自己的幂等层误杀（本次冒烟测试真实踩到的坑）。
     */
    private final Map<Long, SeckillResult> results = new ConcurrentHashMap<>();

    public SeckillService(StringRedisTemplate redis,
                          ReliableMessagePublisher publisher,
                          @Value("${mq.mall.seckill-stock:1000}") long initStock) throws Exception {
        this.redis = redis;
        this.publisher = publisher;
        this.initStock = initStock;
        this.seckillScript = new DefaultRedisScript<>();
        this.seckillScript.setResultType(Long.class);
        this.seckillScript.setScriptText(new String(
                new ClassPathResource("lua/seckill.lua").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8));
    }

    /** 启动时初始化 Redis 预扣库存（幂等：已存在则跳过） */
    @PostConstruct
    public void initStock() {
        redis.opsForValue().setIfAbsent(stockKey(1), String.valueOf(initStock));
        log.info("[秒杀] Redis 预扣库存初始化 sku=1 stock={}", initStock);
    }

    /** 模拟 users 个用户并发抢购（真实场景这些请求来自网关） */
    public Map<String, Object> seckill(long skuId, int users) {
        int success = 0, soldOut = 0, dup = 0;
        long t0 = System.nanoTime();

        for (int i = 0; i < users; i++) {
            long userId = 500_000 + i;   // 模拟不同用户
            Long r = redis.execute(seckillScript,
                    List.of(stockKey(skuId), usersKey(skuId)), String.valueOf(userId));
            if (r == null) r = -1L;
            switch (r.intValue()) {
                case 1 -> {
                    // 预扣成功 → 发 MQ 异步建单（削峰核心洪峰进队列）
                    long orderId = Ids.nextId();   // 全局唯一，重启不撞号
                    results.put(orderId, SeckillResult.queued());
                    publisher.sendFast("mall.seckill", "seckill.order",
                            "seckill-" + orderId, new SeckillOrderRequest(orderId, userId, skuId));
                    success++;
                }
                case -1 -> soldOut++;
                case -2 -> dup++;
                default -> soldOut++;
            }
        }
        long costMs = (System.nanoTime() - t0) / 1_000_000;

        return Map.of(
                "users", users,
                "抢购成功_已入队", success,
                "售罄拒绝", soldOut,
                "重复抢购拦截", dup,
                "接入层耗时ms", costMs,      // 远小于 users×DB耗时 —— 这就是削峰
                "提示", "订单正由消费者匀速落库，稍后 GET /api/seckill/" + skuId + "/stats 查看对账");
    }

    public SeckillResult result(long orderId) {
        return results.getOrDefault(orderId, new SeckillResult("NOT_FOUND", "无此订单号"));
    }

    public void markResult(long orderId, String status, String reason) {
        results.put(orderId, new SeckillResult(status, reason));
    }

    /** 重置演示：Redis 库存回满、清空已购集合与结果（对账数据不动） */
    public void reset() {
        redis.opsForValue().set(stockKey(1), String.valueOf(initStock));
        redis.delete(usersKey(1));
        results.clear();
        log.info("[秒杀] 已重置");
    }

    public Map<String, Object> redisSnapshot(long skuId) {
        String stock = redis.opsForValue().get(stockKey(skuId));
        Long buyers = redis.opsForSet().size(usersKey(skuId));
        return Map.of("redisStock", stock == null ? -1 : Long.parseLong(stock),
                "redisBuyers", buyers == null ? 0 : buyers);
    }

    public int queuedCount() {
        return (int) results.values().stream().filter(r -> "QUEUED".equals(r.status())).count();
    }

    public Map<Long, SeckillResult> resultsView() {
        return results;
    }

    private static String stockKey(long skuId) { return "seckill:stock:" + skuId; }
    private static String usersKey(long skuId) { return "seckill:users:" + skuId; }
}
