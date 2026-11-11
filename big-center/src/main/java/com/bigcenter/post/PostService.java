package com.bigcenter.post;

import com.bigcenter.auth.UserContext;
import com.bigcenter.common.BizException;
import com.bigcenter.common.ErrorCode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 帖子服务（community 模式总装）：
 * 发帖 = 落库待审 + 发 MQ 进审核管道（AI 审核 → 通过/拒绝/人工卡点，AuditConsumer）；
 * 热榜 = ZSet（过审入榜基础分），HeatDecayJob 每小时 ×0.98 时间衰减让旧帖自然沉底。
 */
@Service
public class PostService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PostService.class);

    /** 状态机：0待审 1通过 2拒绝 3待人工（community PostDao 同款） */
    public static final int PENDING = 0;
    public static final int APPROVED = 1;
    public static final int REJECTED = 2;
    public static final int HUMAN_AUDIT = 3;

    static final String HOT_KEY = "bc:post:hot";

    private final PostMapper postMapper;
    private final RabbitTemplate rabbit;
    private final StringRedisTemplate redis;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public PostService(PostMapper postMapper, RabbitTemplate rabbit, StringRedisTemplate redis,
                       org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.postMapper = postMapper;
        this.rabbit = rabbit;
        this.redis = redis;
        this.jdbc = jdbc;
    }

    /** 发帖：落库待审 + 发审核消息（MQ 管道模式——审核耗时隔离在消费者侧） */
    public long publish(String title, String content) {
        Post post = new Post();
        post.setAuthorId(UserContext.get());
        post.setTitle(title);
        post.setContent(content);
        post.setStatus(PENDING);
        postMapper.insert(post);
        rabbit.convertAndSend("bc.post", "post.submitted",
                Map.of("postId", post.getId(), "authorId", post.getAuthorId(), "title", title));
        return post.getId();
    }

    /** 帖子详情 */
    public Post get(long id) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new BizException(ErrorCode.SYSTEM_ERROR);
        return post;
    }

    /** 热榜 TopN（ZSet score=热度分，衰减后自然排序） */
    public List<Long> hotTopN(int n) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(HOT_KEY, 0, n - 1L);
        if (tuples == null) return List.of();
        return tuples.stream().map(t -> Long.parseLong(Objects.requireNonNull(t.getValue()))).toList();
    }

    /** 审核通过后的热度入榜（AuditConsumer 调用：基础热度=10） */
    public void onApproved(long postId) {
        redis.opsForZSet().add(HOT_KEY, String.valueOf(postId), 10.0);
        log.info("[热榜] 帖子过审入榜 postId={}", postId);
    }

    /**
     * 人工审核（community AuditConsumer.humanAudit 总装版）：
     * 通过 = 从 HUMAN_AUDIT 推进（CAS 期望状态必须匹配来源！）走完整发布链；
     * 拒绝 = HUMAN_AUDIT → REJECTED。卡点无人处理的问题由此接口闭环。
     */
    public void humanAudit(long postId, boolean pass) {
        Post post = get(postId);
        if (pass) {
            approveFrom(postId, post.getAuthorId(), Post.HUMAN_AUDIT);
        } else {
            postMapper.casStatus(postId, Post.HUMAN_AUDIT, Post.REJECTED, "人工审核：不通过");
            log.info("[人工审核] 拒绝 postId={}", postId);
        }
    }

    /** 过审发布链：CAS 推进 + 热榜入榜 + 积分奖励（AuditConsumer.approve 与本方法共用） */
    public void approveFrom(long postId, long authorId, int fromStatus) {
        if (postMapper.casStatus(postId, fromStatus, Post.APPROVED, null) == 0) {
            log.info("[审核] 状态不符合预期({}→{})，跳过 postId={}", fromStatus, Post.APPROVED, postId);
            return;
        }
        onApproved(postId);
        try {
            jdbc.update("INSERT INTO t_points_log (user_id, points, reason, message_id) VALUES (?, ?, '发帖奖励', ?)",
                    authorId, 10, "post-reward-" + postId);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.info("[积分] 幂等拦截（已发过）postId={}", postId);
        }
    }
}
