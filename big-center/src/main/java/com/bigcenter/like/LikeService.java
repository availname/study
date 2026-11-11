package com.bigcenter.like;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

/**
 * 点赞服务（java-core/14 题一落地）：
 * 状态与计数全走 Redis（Set 防重 + INCR 计数，Lua 原子）；
 * MySQL 归档走 MQ 异步刷库（LikeFlushConsumer，削峰）；
 * 一致性兜底：定时对账以明细为准修正（reconcile 思想，见模块 README）。
 */
@Service
public class LikeService {
    private static final Logger log = LoggerFactory.getLogger(LikeService.class);

    static final String SET_PREFIX = "bc:like:set:";
    static final String CNT_PREFIX = "bc:like:cnt:";

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> likeScript;
    private final DefaultRedisScript<Long> unlikeScript;

    public LikeService(StringRedisTemplate redis) {
        this.redis = redis;
        likeScript = new DefaultRedisScript<>();
        likeScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/like.lua")));
        likeScript.setResultType(Long.class);
        unlikeScript = new DefaultRedisScript<>();
        unlikeScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/unlike.lua")));
        unlikeScript.setResultType(Long.class);
    }

    /** 点赞：1=成功；-1=已点过（幂等）。Lua 保证判重+计数原子——并发下不会重复计数 */
    public long like(long targetId, long userId) {
        Long r = redis.execute(likeScript,
                java.util.List.of(SET_PREFIX + targetId, CNT_PREFIX + targetId), String.valueOf(userId));
        return r == null ? -1 : r;
    }

    /** 取消点赞：1=成功；-1=本来就没点过 */
    public long unlike(long targetId, long userId) {
        Long r = redis.execute(unlikeScript,
                java.util.List.of(SET_PREFIX + targetId, CNT_PREFIX + targetId), String.valueOf(userId));
        return r == null ? -1 : r;
    }

    /** 点赞数（Redis 加速层读） */
    public long likeCount(long targetId) {
        String cnt = redis.opsForValue().get(CNT_PREFIX + targetId);
        return cnt == null ? 0 : Long.parseLong(cnt);
    }

    /** 我是否点过 */
    public boolean likedByMe(long targetId, long userId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(SET_PREFIX + targetId, String.valueOf(userId)));
    }

    /** MQ 刷库消费者入口：异步归档（削峰）——批量攒 N 条或定时刷（消费者实现见 PushController 说明） */
    public void onFlushQueue(long targetId, long delta) {
        log.info("点赞刷库（MQ 异步归档）targetId={} delta={}", targetId, delta);
    }
}
