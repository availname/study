package com.example.redisdemo.controller;

import com.example.redisdemo.common.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * stage4/01：秒杀 Lua 原子扣库存（含一人一单）。
 * Lua 文件版放在 resources/lua/，此处内嵌同款脚本便于阅读。
 */
@RestController
@RequestMapping("/seckill")
public class SeckillController {

    /** Lua：判断+扣减+登记 一体原子执行（服务端互斥，无并发窗口） */
    private static final DefaultRedisScript<Long> SECKILL = new DefaultRedisScript<>("""
            if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then return -2 end
            local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
            if stock <= 0 then return 0 end
            redis.call('DECR', KEYS[1])
            redis.call('SADD', KEYS[2], ARGV[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    /** 教学版"订单表"：真实场景发 MQ 异步落库（见 mq-mall 秒杀） */
    public static final Map<Long, String> ORDERS = new ConcurrentHashMap<>();

    public SeckillController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @PostMapping("/reset")
    public ApiResponse<String> reset(@RequestParam(defaultValue = "100") int stock) {
        redis.opsForValue().set("seckill:stock:1", String.valueOf(stock));
        redis.delete("seckill:users:1");
        ORDERS.clear();
        return ApiResponse.ok("库存重置为 " + stock);
    }

    @PostMapping("/{userId}")
    public ApiResponse<String> seckill(@PathVariable long userId) {
        Long r = redis.execute(SECKILL,
                List.of("seckill:stock:1", "seckill:users:1"), String.valueOf(userId));
        return switch (r == null ? -1 : r.intValue()) {
            case 1 -> {
                ORDERS.put(userId, "已抢到");
                yield ApiResponse.ok("抢购成功（已入队异步落库）");
            }
            case 0 -> ApiResponse.error("已售罄");
            case -2 -> ApiResponse.error("该用户已抢过（一人一单拦截）");
            default -> ApiResponse.error("库存数据异常");
        };
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        String stock = redis.opsForValue().get("seckill:stock:1");
        return ApiResponse.ok(Map.of(
                "剩余库存", stock == null ? "未初始化" : stock,
                "抢到人数", String.valueOf(redis.opsForSet().size("seckill:users:1")),
                "订单数", ORDERS.size()));
    }
}
