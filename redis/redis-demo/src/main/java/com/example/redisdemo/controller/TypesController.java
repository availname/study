package com.example.redisdemo.controller;

import com.example.redisdemo.common.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * stage1/03：五大基础数据类型体验。
 * 启动后按接口逐个玩，用 redis-cli docker exec -it redis-learn redis-cli 对照观察。
 */
@RestController
@RequestMapping("/types")
public class TypesController {

    private final StringRedisTemplate redis;

    public TypesController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** String：验证码（String + TTL） */
    @GetMapping("/string")
    public ApiResponse<String> string(@RequestParam(defaultValue = "8848") String code) {
        redis.opsForValue().set("demo:captcha:13800001111", code, Duration.ofMinutes(5));
        String v = redis.opsForValue().get("demo:captcha:13800001111");
        Long ttl = redis.getExpire("demo:captcha:13800001111");
        return ApiResponse.ok("SET+EX 后 GET=" + v + "，剩余 TTL=" + ttl + "s（用 redis-cli: TTL demo:captcha:13800001111 观察）");
    }

    /** String：原子计数器 */
    @GetMapping("/counter")
    public ApiResponse<Long> counter() {
        return ApiResponse.ok(redis.opsForValue().increment("demo:read:1001"));
    }

    /** Hash：购物车加减（对象字段级读写） */
    @GetMapping("/hash")
    public ApiResponse<Map<Object, Object>> hash(@RequestParam(defaultValue = "sku1") String sku) {
        redis.opsForHash().increment("demo:cart:1001", sku, 1);
        Map<Object, Object> cart = redis.opsForHash().entries("demo:cart:1001");
        return ApiResponse.ok(cart);   // 对照 redis-cli: HGETALL demo:cart:1001
    }

    /** List：最新动态（左插+截断保留 10 条） */
    @GetMapping("/list")
    public ApiResponse<Object> list(@RequestParam(defaultValue = "新动态") String item) {
        redis.opsForList().leftPush("demo:feed:1001", item + "-" + System.currentTimeMillis() % 10000);
        redis.opsForList().trim("demo:feed:1001", 0, 9);   // 只留最近 10 条，防大 Key
        return ApiResponse.ok(redis.opsForList().range("demo:feed:1001", 0, -1));
    }

    /** Set：抽奖池（去重 + 随机抽取） */
    @GetMapping("/set")
    public ApiResponse<Object> set(@RequestParam(defaultValue = "u1") String user) {
        Long added = redis.opsForSet().add("demo:lottery", user);   // 已存在返回 0（去重）
        return ApiResponse.ok(Map.of(
                "重复报名被拦", 0L == added,
                "池子人数", String.valueOf(redis.opsForSet().size("demo:lottery")),
                "随机抽1人", redis.opsForSet().randomMember("demo:lottery")));
    }

    /** ZSet：排行榜（加分/前三/我的名次） */
    @GetMapping("/zset")
    public ApiResponse<Map<String, Object>> zset(@RequestParam(defaultValue = "玩家A") String player,
                                                 @RequestParam(defaultValue = "100") long delta) {
        redis.opsForZSet().incrementScore("demo:rank:game", player, delta);
        Set<ZSetOperations.TypedTuple<String>> top3 = redis.opsForZSet()
                .reverseRangeWithScores("demo:rank:game", 0, 2);
        Map<String, Object> view = new LinkedHashMap<>();
        top3.forEach(t -> view.put(String.valueOf(t.getScore()), t.getValue()));
        view.put("我的名次(0起)", redis.opsForZSet().reverseRank("demo:rank:game", player));
        return ApiResponse.ok(view);
    }
}
