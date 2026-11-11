package com.bigcenter.rate;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis 分布式限流（java-core/12 题三）：
 * Lua 滑动窗口（ZSET 时间戳）原子执行——多实例天然共享限流状态；
 * 超限快速失败返回 false（Controller/AOP 转 429 + 5001 错误码）。
 */
@Service
public class RateLimiterService {

    static final String KEY_PREFIX = "bc:rate:";

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> script;

    public RateLimiterService(StringRedisTemplate redis) {
        this.redis = redis;
        script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rate_limit.lua")));
        script.setResultType(Long.class);
    }

    /** 尝试获取一次配额：true=放行；false=超限拒绝。可注入 mock 单测（不碰真实 Redis） */
    public boolean tryAcquire(String key, int limit, long windowMs) {
        Long r = redis.execute(script, List.of(KEY_PREFIX + key),
                String.valueOf(System.currentTimeMillis()), String.valueOf(windowMs), String.valueOf(limit));
        return r != null && r == 1;
    }
}
