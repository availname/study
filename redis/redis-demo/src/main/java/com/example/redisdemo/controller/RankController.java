package com.example.redisdemo.controller;

import com.example.redisdemo.common.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/** stage4/02：排行榜（ZSet），按月分 key 防大 Key 的意识体现在 key 设计里 */
@RestController
@RequestMapping("/rank")
public class RankController {

    private final StringRedisTemplate redis;

    public RankController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @PostMapping("/{player}/{delta}")
    public ApiResponse<String> add(@PathVariable String player, @PathVariable long delta) {
        redis.opsForZSet().incrementScore("rank:game:2026", player, delta);   // 原子加分
        return ApiResponse.ok(player + " +" + delta);
    }

    /** 前三名 */
    @GetMapping("/top")
    public ApiResponse<Object> top() {
        Set<ZSetOperations.TypedTuple<String>> top3 =
                redis.opsForZSet().reverseRangeWithScores("rank:game:2026", 0, 2);
        return ApiResponse.ok(top3);
    }

    /** 我的实时名次（0 开始） */
    @GetMapping("/{player}/rank")
    public ApiResponse<Object> rank(@PathVariable String player) {
        return ApiResponse.ok(redis.opsForZSet().reverseRank("rank:game:2026", player));
    }
}
