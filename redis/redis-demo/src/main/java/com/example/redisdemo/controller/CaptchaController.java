package com.example.redisdemo.controller;

import com.example.redisdemo.common.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

/**
 * stage1/05 + stage4：验证码（String+TTL）。
 * 校验用 Lua 原子"比对+删除"——对比 GET-then-DEL 两步操作在并发下可能被双消费。
 */
@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    private static final DefaultRedisScript<Long> VERIFY = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                redis.call('del', KEYS[1])
                return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public CaptchaController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 发送验证码：5 分钟自动过期（EX 一步到位） */
    @PostMapping("/send/{phone}")
    public ApiResponse<String> send(@PathVariable String phone) {
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        redis.opsForValue().set("captcha:login:" + phone, code, Duration.ofMinutes(5));
        return ApiResponse.ok(code);   // 教学直接返回；生产走短信网关
    }

    /** 校验验证码：Lua 原子比对+删除，一次性使用 */
    @PostMapping("/verify/{phone}/{code}")
    public ApiResponse<Boolean> verify(@PathVariable String phone, @PathVariable String code) {
        Long r = redis.execute(VERIFY, List.of("captcha:login:" + phone), code);
        return ApiResponse.ok(r != null && r == 1);
    }
}
