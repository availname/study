package com.example.community.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 热榜时间衰减（stage5/05 热搜同款）：每小时全榜热度 ×0.98，旧帖自然沉底 */
@Component
public class HeatDecayJob {
    private static final Logger log = LoggerFactory.getLogger(HeatDecayJob.class);

    private final StringRedisTemplate redis;

    public HeatDecayJob(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Scheduled(fixedDelay = 3600_000)
    public void decay() {
        Set<ZSetOperations.TypedTuple<String>> all =
                redis.opsForZSet().rangeWithScores("community:post:hot", 0, -1);
        if (all == null || all.isEmpty()) return;
        int n = 0;
        for (ZSetOperations.TypedTuple<String> t : all) {
            Double score = t.getScore();
            if (score == null) continue;
            double decayed = score * 0.98;
            if (decayed < 0.5) {
                redis.opsForZSet().remove("community:post:hot", t.getValue());   // 归零出榜
            } else {
                redis.opsForZSet().add("community:post:hot", t.getValue(), decayed);
            }
            n++;
        }
        log.info("[热榜衰减] 处理 {} 帖，×0.98，低于0.5出榜", n);
    }
}
