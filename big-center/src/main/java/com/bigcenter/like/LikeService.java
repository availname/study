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
    private final com.bigcenter.messaging.MessageDedupService dedup;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final com.bigcenter.messaging.ReliableMessagePublisher publisher;
    private final DefaultRedisScript<Long> likeScript;
    private final DefaultRedisScript<Long> unlikeScript;

    public LikeService(StringRedisTemplate redis,
                       com.bigcenter.messaging.MessageDedupService dedup,
                       org.springframework.jdbc.core.JdbcTemplate jdbc,
                       com.bigcenter.messaging.ReliableMessagePublisher publisher) {
        this.redis = redis;
        this.dedup = dedup;
        this.jdbc = jdbc;
        this.publisher = publisher;
        likeScript = new DefaultRedisScript<>();
        likeScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/like.lua")));
        likeScript.setResultType(Long.class);
        unlikeScript = new DefaultRedisScript<>();
        unlikeScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/unlike.lua")));
        unlikeScript.setResultType(Long.class);
    }

    /** 点赞：1=成功；-1=已点过（幂等）。Lua 保证判重+计数原子——并发下不会重复计数；成功发 MQ 异步归档 */
    public long like(long targetId, long userId) {
        Long r = redis.execute(likeScript,
                java.util.List.of(SET_PREFIX + targetId, CNT_PREFIX + targetId), String.valueOf(userId));
        if (r != null && r == 1L) {
            publisher.sendFast("bc.like", "like.flushed",
                    java.util.Map.of("targetId", targetId, "userId", userId, "delta", 1,
                            "messageId", "like-flush-" + targetId + "-" + userId));
        }
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

    /**
     * MQ 刷库消费者入口：异步归档到 t_like_archive（削峰）。
     * 消费幂等两层防线（MessageDedupService）——Redis setnx 挡 99%，重投不重复归档。
     */
    public void onFlushQueue(long targetId, long userId, long delta, String messageId) {
        if (!dedup.tryConsume("like-flush", messageId)) {
            log.info("点赞刷库重复消息跳过 targetId={} messageId={}", targetId, messageId);
            return;
        }
        try {
            jdbc.update("INSERT INTO t_like_archive (target_id, user_id, delta) VALUES (?, ?, ?)",
                    targetId, userId, delta);
            log.info("点赞刷库（MQ 异步归档）targetId={} userId={} delta={}", targetId, userId, delta);
        } catch (Exception e) {
            dedup.fail(messageId);
            throw e;
        }
    }

    /**
     * 定时对账（community CounterFlushJob / 支付对账思想总装）：
     * 以明细账 t_like_archive 的 SUM(delta) 为准，修正 Redis 计数漂移（刷库丢消息/重复的兜底）。
     * 与 set 明细一致性：Set 大小是"当前点过的人数"，计数=SUM(delta)（含取消的 -1）。
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 600_000)
    public void reconcileCounts() {
        // 取有归档记录的目标（教学简化：全表聚合；量大时分页/增量水位，见 mq-mall 对账篇）
        jdbc.query("SELECT target_id, COALESCE(SUM(delta),0) AS total FROM t_like_archive GROUP BY target_id",
                rs -> {
                    long targetId = rs.getLong("target_id");
                    long truth = rs.getLong("total");
                    String cnt = redis.opsForValue().get(CNT_PREFIX + targetId);
                    long current = cnt == null ? 0 : Long.parseLong(cnt);
                    if (current != truth) {
                        redis.opsForValue().set(CNT_PREFIX + targetId, String.valueOf(truth));
                        log.warn("[点赞对账] 计数修正 targetId={} redis={} 明细真相={}", targetId, current, truth);
                    }
                });
    }
}
