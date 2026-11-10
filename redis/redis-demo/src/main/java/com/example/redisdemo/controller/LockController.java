package com.example.redisdemo.controller;

import com.example.redisdemo.common.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * stage3/03：分布式锁（手写教学版，理解原理；生产用 Redisson）。
 * 三要点：①SET NX EX 原子抢锁 ②Lua 校验 requestId 再释放（防误删他人锁） ③看门狗续期（此处手动续期演示）。
 */
@RestController
@RequestMapping("/lock")
public class LockController {

    /** Lua 原子释放：比对身份再删——get+del 两步不能拆，否则可能误删别人的锁 */
    private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """, Long.class);

    /** Lua 原子续期：锁还在且是自己持有的，把 TTL 续回满（看门狗的手动简化版） */
    private static final DefaultRedisScript<Long> RENEW = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('expire', KEYS[1], ARGV[2])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public LockController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 抢锁：SET NX EX 一条命令原子完成（NX=不存在才写，EX=10 秒自动释放防死锁） */
    @PostMapping("/try/{resource}")
    public ApiResponse<String> tryLock(@PathVariable String resource,
                                       @RequestParam(defaultValue = "10") int ttlSeconds) {
        String requestId = UUID.randomUUID().toString();   // 每次持锁的唯一身份
        Boolean ok = redis.opsForValue()
                .setIfAbsent("lock:" + resource, requestId, Duration.ofSeconds(ttlSeconds));
        if (Boolean.TRUE.equals(ok)) {
            return ApiResponse.ok(requestId);   // 拿到 requestId，释放/续期时带上（证明锁是我的）
        }
        return ApiResponse.error("锁被占用（另一实例正在执行）");
    }

    /** 释放：Lua 校验 requestId——只解自己的锁 */
    @PostMapping("/release/{resource}")
    public ApiResponse<Boolean> release(@PathVariable String resource,
                                        @RequestParam String requestId) {
        Long r = redis.execute(UNLOCK, List.of("lock:" + resource), requestId);
        return ApiResponse.ok(r != null && r == 1);
    }

    /** 续期（看门狗的手动演示）：持锁者把 TTL 续回满 */
    @PostMapping("/renew/{resource}")
    public ApiResponse<Boolean> renew(@PathVariable String resource,
                                      @RequestParam String requestId,
                                      @RequestParam(defaultValue = "10") int ttlSeconds) {
        Long r = redis.execute(RENEW, List.of("lock:" + resource), requestId, String.valueOf(ttlSeconds));
        return ApiResponse.ok(r != null && r == 1);
    }

    /** 锁内业务演示：抢到锁后做一件"慢事"，观察另一客户端抢不到 */
    @PostMapping("/execute/{resource}")
    public ApiResponse<String> execute(@PathVariable String resource) throws Exception {
        String requestId = UUID.randomUUID().toString();
        Boolean ok = redis.opsForValue()
                .setIfAbsent("lock:" + resource, requestId, Duration.ofSeconds(10));
        if (!Boolean.TRUE.equals(ok)) return ApiResponse.error("锁被占用");
        try {
            Thread.sleep(3000);   // 模拟业务 3 秒——期间另开终端抢锁会失败
            return ApiResponse.ok("业务执行完成，持有者=" + requestId);
        } finally {
            redis.execute(UNLOCK, List.of("lock:" + resource), requestId);
        }
    }
}
