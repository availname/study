package com.example.community.post;

import com.example.community.messaging.MessageDedupService;
import com.example.community.messaging.ReliableMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 审核管道消费者（MQ 管道模式，mq 教学 stage5 同款）：
 * post.submitted → AI 审核（sleep 模拟）→ 通过/拒绝/人工卡点
 * 通过 → 发积分（DB 唯一索引幂等 + ZSet 积分榜）→ fanout 广播（Feed 推送 + 通知）
 */
@Component
public class AuditConsumer {
    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);
    private static final int POST_REWARD = 10;

    private final PostDao postDao;
    private final MessageDedupService dedup;
    private final ReliableMessagePublisher publisher;
    private final NamedParameterJdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public AuditConsumer(PostDao postDao, MessageDedupService dedup,
                         ReliableMessagePublisher publisher, NamedParameterJdbcTemplate jdbc,
                         StringRedisTemplate redis) {
        this.postDao = postDao;
        this.dedup = dedup;
        this.publisher = publisher;
        this.jdbc = jdbc;
        this.redis = redis;
    }

    @org.springframework.amqp.rabbit.annotation.RabbitListener(
            queues = "community.post.submitted.queue", containerFactory = "auditFactory")
    public void onSubmitted(Map<String, Object> msg) {
        long postId = ((Number) msg.get("postId")).longValue();
        long authorId = ((Number) msg.get("authorId")).longValue();
        String title = String.valueOf(msg.get("title"));

        String messageId = "post-submitted-" + postId;
        if (!dedup.tryConsume("post-audit", messageId)) {
            log.info("[审核] 重复消息跳过 postId={}", postId);
            return;
        }
        try {
            Thread.sleep(800);   // 模拟 AI 审核耗时
            if (title.contains("BLOCK")) {
                postDao.casStatus(postId, PostDao.PENDING, PostDao.REJECTED, "AI审核：涉嫌违规内容");
                notify(authorId, "你的帖子《" + title + "》未通过审核");
                log.info("[审核] 拒绝 postId={}", postId);
                return;
            }
            if (title.contains("HUMAN")) {
                postDao.casStatus(postId, PostDao.PENDING, PostDao.HUMAN_AUDIT, null);
                log.info("[审核] AI 不确定 → 待人工审核 postId={}（POST /api/admin/audit/{} 处理）", postId, postId);
                return;
            }
            approve(postId, authorId, title, PostDao.PENDING);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            dedup.fail(messageId);
            throw e;   // 本地重试 3 次 → 死信落库
        }
    }

    /** 审核通过：状态推进（来源可以是"待审"或"人工卡点"——CAS 期望状态必须匹配来源！）+ 奖励 + fanout 广播 */
    public void approve(long postId, long authorId, String title, int fromStatus) {
        if (postDao.casStatus(postId, fromStatus, PostDao.APPROVED, null) == 0) {
            log.info("[审核] 状态不符合预期({}→{})，跳过 postId={}", fromStatus, PostDao.APPROVED, postId);
            return;
        }
        // 发帖奖励积分：t_points_log.message_id 唯一索引幂等（stage3/02 两层防线中的 DB 层）
        String messageId = "post-reward-" + postId;
        try {
            jdbc.update("""
                    INSERT INTO t_points_log (user_id, points, reason, message_id)
                    VALUES (:uid, :p, '发帖奖励', :mid)
                    """, new MapSqlParameterSource()
                    .addValue("uid", authorId).addValue("p", POST_REWARD).addValue("mid", messageId));
            jdbc.update("""
                    UPDATE t_user SET points = points + :p WHERE id = :uid
                    """, new MapSqlParameterSource().addValue("uid", authorId).addValue("p", POST_REWARD));
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.info("[积分] 幂等拦截（已发过）postId={}", postId);
        }
        // ZSet 积分榜（Redis 权威，DB points 是同步副本）
        redis.opsForZSet().incrementScore("community:rank:points", String.valueOf(authorId), POST_REWARD);

        // fanout 广播：Feed 推送 + 作者通知（消费者各自独立，互不影响）
        publisher.sendFast("community.published.fanout", "",
                "post-published-" + postId,
                Map.of("postId", postId, "title", title, "authorId", authorId));
        log.info("[审核] 通过并广播 postId={} 作者={} 奖励{}积分", postId, authorId, POST_REWARD);
    }

    /** 人工审核端点调用：通过（从 HUMAN_AUDIT 推进，走完整发布链路）或拒绝 */
    public void humanAudit(long postId, boolean pass) {
        PostDao.PostRow post = postDao.find(postId).orElseThrow();
        if (pass) {
            approve(postId, post.authorId(), post.title(), PostDao.HUMAN_AUDIT);
        } else {
            postDao.casStatus(postId, PostDao.HUMAN_AUDIT, PostDao.REJECTED, "人工审核：不通过");
            notify(post.authorId(), "你的帖子《" + post.title() + "》未通过人工审核");
        }
    }

    private void notify(long userId, String text) {
        redis.opsForList().leftPush("community:notif:" + userId, text);
        redis.opsForList().trim("community:notif:" + userId, 0, 49);   // 收件箱只留 50 条
    }
}
