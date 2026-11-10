package com.example.redisdemo.controller;

import com.example.redisdemo.common.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * stage3/01：缓存击穿——热点 key 过期瞬间万级并发冲 DB 的防线（互斥锁重建）。
 * 未命中 → 抢重建锁 → 只有持锁者查"DB" → 回写缓存；其余请求短暂等待后重读缓存。
 */
@RestController
@RequestMapping("/cache")
public class CacheDemoController {

    private final StringRedisTemplate redis;

    public CacheDemoController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 模拟 DB 查询（1 秒慢查询，统计被调次数观察"穿透量"） */
    private String mockDb(String id) {
        DB_CALLS.incrementAndGet();
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return "商品" + id + "详情(来自DB)";
    }
    private static final java.util.concurrent.atomic.AtomicLong DB_CALLS = new java.util.concurrent.atomic.AtomicLong();

    @PostMapping("/reset")
    public ApiResponse<String> reset() {
        redis.delete("product:cache:1001");
        redis.delete("product:lock:1001");
        DB_CALLS.set(0);
        return ApiResponse.ok("缓存已清空，DB 调用计数归零（现在并发打 /cache/product/1001 观察击穿防护）");
    }

    /** 互斥锁重建缓存（stage3/01 方案1） */
    @GetMapping("/product/{id}")
    public ApiResponse<String> get(@PathVariable String id) throws Exception {
        String key = "product:cache:" + id;
        String json = redis.opsForValue().get(key);
        if (json != null) return ApiResponse.ok(json + " [缓存命中]");

        // 抢重建锁：只有一个人去查 DB（雪崩式并发被锁在门外）
        String lockKey = "product:lock:" + id;
        String requestId = UUID.randomUUID().toString();
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, requestId, Duration.ofSeconds(10));
        if (Boolean.TRUE.equals(locked)) {
            try {
                String dbData = mockDb(id);   // 只有持锁者碰 DB
                redis.opsForValue().set(key, dbData, Duration.ofHours(1));
                return ApiResponse.ok(dbData + " [互斥锁重建, 本请求查了DB]");
            } finally {
                redis.execute(new DefaultRedisScript<>("""
                        if redis.call('get', KEYS[1]) == ARGV[1] then
                            return redis.call('del', KEYS[1])
                        end
                        return 0""", Long.class), List.of(lockKey), requestId);
            }
        }
        // 没抢到锁：等一会儿重读（重建者马上就会填好缓存）
        Thread.sleep(200 + ThreadLocalRandom.current().nextInt(300));
        String retry = redis.opsForValue().get(key);
        return ApiResponse.ok((retry != null ? retry : "仍在重建") + " [等待重建后读取]");
    }

    @GetMapping("/dbcalls")
    public ApiResponse<Long> dbCalls() {
        return ApiResponse.ok(DB_CALLS.get());   // 并发 50 请求后应≈2~3 而不是 50
    }
}
