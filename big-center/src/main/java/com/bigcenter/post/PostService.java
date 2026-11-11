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

    public PostService(PostMapper postMapper, RabbitTemplate rabbit, StringRedisTemplate redis) {
        this.postMapper = postMapper;
        this.rabbit = rabbit;
        this.redis = redis;
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
}
