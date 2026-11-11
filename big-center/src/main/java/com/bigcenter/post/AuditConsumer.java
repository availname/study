package com.bigcenter.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 审核管道消费者（community AuditConsumer 总装版，MQ 管道模式）：
 * post.submitted → AI 审核（规则模拟）→ 通过/拒绝/人工卡点。
 * 通过 = CAS 状态推进 + 热榜入榜 + 积分奖励（唯一索引幂等）。
 */
@Component
public class AuditConsumer {
    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);
    static final int POST_REWARD = 10;

    private final PostMapper postMapper;
    private final PostService postService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public AuditConsumer(PostMapper postMapper, PostService postService,
                         org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.postMapper = postMapper;
        this.postService = postService;
        this.jdbc = jdbc;
    }

    @RabbitListener(queues = "bc.post.submitted.queue")
    public void onSubmitted(Map<String, Object> msg) {
        long postId = ((Number) msg.get("postId")).longValue();
        long authorId = ((Number) msg.get("authorId")).longValue();
        String title = String.valueOf(msg.get("title"));

        try {
            Thread.sleep(300);   // 模拟 AI 审核耗时（教学演示）
            if (title.contains("BLOCK")) {
                casOrSkip(postId, Post.PENDING, Post.REJECTED, "AI审核：涉嫌违规内容");
                log.info("[审核] 拒绝 postId={}", postId);
                return;
            }
            if (title.contains("HUMAN")) {
                casOrSkip(postId, Post.PENDING, Post.HUMAN_AUDIT, null);
                log.info("[审核] AI 不确定 → 待人工 postId={}", postId);
                return;
            }
            approve(postId, authorId, Post.PENDING);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            log.error("[审核] 失败 postId={} err={}", postId, e.getMessage(), e);
            throw e;    // 本地重试 3 次 → 死信
        }
    }

    /** 审核通过：委托 PostService.approveFrom（CAS 推进+热榜入榜+积分奖励，人工审核共用同一链路） */
    public void approve(long postId, long authorId, int fromStatus) {
        postService.approveFrom(postId, authorId, fromStatus);
        log.info("[审核] 通过链路执行完成 postId={} 作者={}", postId, authorId);
    }

    private int casOrSkip(long postId, int expect, int next, String reason) {
        return postMapper.casStatus(postId, expect, next, reason);
    }
}
