package com.example.community.job;

import com.example.community.post.PostDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Write Behind 计数刷库任务（stage4 缓存模式实战）：
 * Redis 里的阅读/点赞计数是"热账"，本任务周期性把它刷进 DB 底账。
 *
 * 关键细节：view 计数用"DECR 已落库增量"法——刷完不删 key（删了会丢刷库间隙的增量），
 * 而是把已落库的数字从 Redis 里减掉，key 继续承接新增量。
 */
@Component
public class CounterFlushJob {
    private static final Logger log = LoggerFactory.getLogger(CounterFlushJob.class);

    private final StringRedisTemplate redis;
    private final PostDao postDao;

    public CounterFlushJob(StringRedisTemplate redis, PostDao postDao) {
        this.redis = redis;
        this.postDao = postDao;
    }

    @Scheduled(fixedDelay = 10_000)
    public void flush() {
        // 阅读计数：SCAN 遍历（生产禁 KEYS）→ GET → 刷 DB → DECR 已落库部分
        try (Cursor<String> cursor = redis.scan(
                ScanOptions.scanOptions().match("community:post:view:*").count(100).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String value = redis.opsForValue().get(key);
                if (value == null) continue;
                long postId = Long.parseLong(key.substring("community:post:view:".length()));
                long viewInRedis = Long.parseLong(value);
                if (viewInRedis <= 0) continue;

                redis.opsForValue().decrement(key, viewInRedis);          // 已落库的减掉，保增量不丢
                PostDao.PostRow post = postDao.find(postId).orElse(null);
                if (post != null) {
                    postDao.flushCounters(postId, post.viewCount() + viewInRedis,
                            Objects.requireNonNullElse(
                                    redis.opsForSet().size("community:post:likes:" + postId), 0L).intValue());
                }
                log.info("[计数刷库] postId={} 本次落库阅读数={}", postId, viewInRedis);
            }
        }
    }
}
