package com.bigcenter.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审核管道验收（community AuditConsumer 语义钉子）：
 * AI 三分支（通过/拒绝/人工）+ CAS 状态推进 + 积分唯一索引幂等 + 热榜入榜。
 */
class AuditConsumerTest {

    private PostMapper postMapper;
    private PostService postService;
    private JdbcTemplate jdbc;
    private AuditConsumer consumer;

    @BeforeEach
    void setUp() {
        postMapper = Mockito.mock(PostMapper.class);
        postService = Mockito.mock(PostService.class);
        jdbc = Mockito.mock(JdbcTemplate.class);
        consumer = new AuditConsumer(postMapper, postService, jdbc);
    }

    @Test
    void 正常帖_CAS从待审推进到通过_入热榜_发积分() {
        when(postMapper.casStatus(1L, Post.PENDING, Post.APPROVED, null)).thenReturn(1);
        consumer.approve(1L, 1001L, Post.PENDING);
        // 过审入热榜（ZSet 基础分）
        verify(postService).onApproved(1L);
        // 积分流水：message_id = post-reward-{postId}（唯一索引幂等的载体）
        verify(jdbc).update(anyString(), eq(1001L), eq(AuditConsumer.POST_REWARD), eq("post-reward-1"));
    }

    @Test
    void CAS输方_状态不符跳过_不发积分不入榜() {
        when(postMapper.casStatus(1L, Post.PENDING, Post.APPROVED, null)).thenReturn(0);
        consumer.approve(1L, 1001L, Post.PENDING);
        verify(postService, never()).onApproved(anyLong());
        verify(jdbc, never()).update(anyString(), anyLong(), anyInt(), anyString());
    }

    @Test
    void 重复积分消息_唯一索引冲突幂等吞掉() {
        when(postMapper.casStatus(1L, Post.PENDING, Post.APPROVED, null)).thenReturn(1);
        Mockito.doThrow(new org.springframework.dao.DuplicateKeyException("uk_message_id"))
                .when(jdbc).update(anyString(), anyLong(), anyInt(), anyString());
        consumer.approve(1L, 1001L, Post.PENDING);   // 不应抛出
        verify(postService).onApproved(1L);
    }

    @Test
    void 违规帖_CAS推进到拒绝() {
        PostMapper pm = postMapper;
        consumer.onSubmitted(Map.of("postId", 2L, "authorId", 1001L, "title", "卖枪BLOCK广告"));
        verify(pm).casStatus(eq(2L), eq(Post.PENDING), eq(Post.REJECTED), anyString());
        verify(jdbc, never()).update(anyString(), anyLong(), anyInt(), anyString());
    }

    @Test
    void 存疑帖_转人工卡点_不发积分() {
        consumer.onSubmitted(Map.of("postId", 3L, "authorId", 1001L, "title", "HUMAN特批测试"));
        verify(postMapper).casStatus(eq(3L), eq(Post.PENDING), eq(Post.HUMAN_AUDIT), any());
        verify(jdbc, never()).update(anyString(), anyLong(), anyInt(), anyString());
    }

    @Test
    void 热榜衰减_低于阈值出榜_高于则降权() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zOps = Mockito.mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zOps);
        java.util.LinkedHashSet<ZSetOperations.TypedTuple<String>> tuples = new java.util.LinkedHashSet<>();
        tuples.add(ZSetOperations.TypedTuple.of("9", 0.4));      // 将出榜
        tuples.add(ZSetOperations.TypedTuple.of("8", 100.0));    // 降权到 98
        when(zOps.rangeWithScores(PostService.HOT_KEY, 0, -1)).thenReturn(tuples);

        new HeatDecayJob(redis).decay();
        verify(zOps).remove(PostService.HOT_KEY, "9");
        verify(zOps).add(PostService.HOT_KEY, "8", 98.0);
    }
}
