package com.example.community.job;

import com.example.community.post.PostDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Write Behind 计数刷库单测（stage4 缓存模式的行为固定）：
 * ① 有增量 → DECR 已落库增量（不删 key，保间隙增量）+ 刷 DB；
 * ② 帖子不存在 → 不刷库；③ 零值/缺值 → 跳过。
 */
class CounterFlushJobTest {

    private static final String KEY = "community:post:view:9";

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private SetOperations<String, String> setOps;
    private PostDao postDao;
    private CounterFlushJob job;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        setOps = mock(SetOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.opsForSet()).thenReturn(setOps);
        postDao = mock(PostDao.class);
        job = new CounterFlushJob(redis, postDao);
    }

    /** SCAN 游标 mock：只返回一个 key（本类用例均单 key） */
    private void scanReturns(String key) {
        @SuppressWarnings("unchecked")
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(key);
        when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);
    }

    @Test
    void 有阅读增量_先DECR再刷库_点赞数取Set基数() {
        scanReturns(KEY);
        when(valueOps.get(KEY)).thenReturn("37");                       // Redis 里待落库 37
        when(setOps.size("community:post:likes:9")).thenReturn(5L);
        when(postDao.find(9L)).thenReturn(Optional.of(new PostDao.PostRow(
                9, "t", "c", 1L, 1, null, 100, 5)));                    // DB 已有 100 阅读

        job.flush();

        verify(valueOps).decrement(KEY, 37L);                           // 关键行为：DECR 已落库增量，不删 key
        verify(redis, never()).delete(anyString());                     // 不删 key，保间隙增量
        verify(postDao).flushCounters(9L, 137L, 5);                     // 100+37 落库
    }

    @Test
    void 帖子不存在_不刷库但增量已减() {
        scanReturns(KEY);
        when(valueOps.get(KEY)).thenReturn("3");
        when(postDao.find(anyLong())).thenReturn(Optional.empty());

        job.flush();

        verify(valueOps).decrement(KEY, 3L);
        verify(postDao, never()).flushCounters(anyLong(), anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void 零值与缺值跳过() {
        scanReturns(KEY);
        when(valueOps.get(KEY)).thenReturn(null);                       // key 被并发删除

        job.flush();

        verify(valueOps, never()).decrement(anyString(), anyLong());
        verify(postDao, never()).find(anyLong());
    }
}
