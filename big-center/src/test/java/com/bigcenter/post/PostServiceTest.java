package com.bigcenter.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 帖子服务验收：人工审核闭环（HUMAN_AUDIT 卡点处理）/ CAS 输方跳过 / 积分唯一索引幂等。
 */
class PostServiceTest {

    private PostMapper postMapper;
    private RabbitTemplate rabbit;
    private StringRedisTemplate redis;
    @SuppressWarnings("unchecked")
    private final ZSetOperations<String, String> zOps = Mockito.mock(ZSetOperations.class);
    private JdbcTemplate jdbc;
    private PostService service;

    @BeforeEach
    void setUp() {
        postMapper = Mockito.mock(PostMapper.class);
        rabbit = Mockito.mock(RabbitTemplate.class);
        redis = Mockito.mock(StringRedisTemplate.class);
        jdbc = Mockito.mock(JdbcTemplate.class);
        when(redis.opsForZSet()).thenReturn(zOps);
        service = new PostService(postMapper, rabbit, redis, jdbc);
    }

    @Test
    void 人工审核通过_从HUMAN_AUDIT推进_入热榜_发积分() {
        Post post = new Post();
        post.setId(7L);
        post.setAuthorId(1001L);
        post.setStatus(Post.HUMAN_AUDIT);
        when(postMapper.selectById(7L)).thenReturn(post);
        when(postMapper.casStatus(7L, Post.HUMAN_AUDIT, Post.APPROVED, null)).thenReturn(1);

        service.humanAudit(7L, true);
        verify(zOps).add(PostService.HOT_KEY, "7", 10.0);
        verify(jdbc).update(contains("t_points_log"), eq(1001L), eq(10), eq("post-reward-7"));
    }

    @Test
    void 人工审核拒绝_CAS到REJECTED_不入热榜不发积分() {
        Post post = new Post();
        post.setId(7L);
        post.setAuthorId(1001L);
        post.setStatus(Post.HUMAN_AUDIT);
        when(postMapper.selectById(7L)).thenReturn(post);

        service.humanAudit(7L, false);
        verify(postMapper).casStatus(eq(7L), eq(Post.HUMAN_AUDIT), eq(Post.REJECTED), anyString());
        verify(zOps, never()).add(anyString(), anyString(), org.mockito.ArgumentMatchers.anyDouble());
        verify(jdbc, never()).update(anyString(), anyLong(), anyInt(), anyString());
    }

    @Test
    void 发布链_CAS输方_全链跳过() {
        when(postMapper.casStatus(1L, Post.PENDING, Post.APPROVED, null)).thenReturn(0);
        service.approveFrom(1L, 1001L, Post.PENDING);
        verify(zOps, never()).add(anyString(), anyString(), org.mockito.ArgumentMatchers.anyDouble());
        verify(jdbc, never()).update(anyString(), anyLong(), anyInt(), anyString());
    }

    @Test
    void 发布链_积分重复_唯一索引冲突幂等吞掉() {
        when(postMapper.casStatus(1L, Post.PENDING, Post.APPROVED, null)).thenReturn(1);
        doThrow(new org.springframework.dao.DuplicateKeyException("uk_message_id"))
                .when(jdbc).update(anyString(), anyLong(), anyInt(), anyString());
        service.approveFrom(1L, 1001L, Post.PENDING);   // 不应抛出
        verify(zOps).add(PostService.HOT_KEY, "1", 10.0);
    }

    @Test
    void 发帖_落库待审并发送审核消息() {
        com.bigcenter.auth.UserContext.set(1001L);
        Mockito.doAnswer(inv -> {
            inv.getArgument(0, Post.class).setId(9L);
            return 1;
        }).when(postMapper).insert(Mockito.<Post>any());
        long id = service.publish("hello", "world");
        org.junit.jupiter.api.Assertions.assertEquals(9L, id);
        verify(rabbit).convertAndSend(eq("bc.post"), eq("post.submitted"),
                Mockito.<java.util.Map<String, Object>>any());
        com.bigcenter.auth.UserContext.clear();
    }
}
