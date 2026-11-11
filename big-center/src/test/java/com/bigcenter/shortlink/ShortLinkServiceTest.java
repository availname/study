package com.bigcenter.shortlink;

import com.bigcenter.auth.UserContext;
import com.bigcenter.shortlink.entity.ShortLink;
import com.bigcenter.shortlink.mapper.ShortLinkMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 短链三层解析（布隆→缓存→库）+ Write Behind 统计——java-core/12 题一验收 */
class ShortLinkServiceTest {

    private ShortLinkMapper mapper;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private ShortLinkService service;

    @BeforeEach
    void setUp() {
        mapper = Mockito.mock(ShortLinkMapper.class);
        redis = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new ShortLinkService(mapper, redis);
        UserContext.set(1001L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void 布隆拦截_不存在的短码不触缓存不查库() {
        String ghost = "ZZZZZZ9";           // 从未发过号——布隆一定不包含
        assertNull(service.resolve(ghost), "布隆应拦截未发号的短码");
        verify(valueOps, never()).get(anyString());
        verify(mapper, never()).selectOne(any());
    }

    @Test
    void 缓存命中_返回长链并计数_不查库() {
        String code = "abc123";
        // 先发一个号把短码写进布隆
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(Mockito.<ShortLink>any())).thenReturn(1);
        service.create("https://example.com/a");
        String created = getLastCreatedCode();

        // 缓存命中
        when(valueOps.get(ShortLinkService.CACHE_PREFIX + created)).thenReturn("https://example.com/a");
        when(valueOps.increment(ShortLinkService.CLICK_PREFIX + created)).thenReturn(1L);
        assertEquals("https://example.com/a", service.resolve(created));
        verify(mapper, Mockito.times(1)).selectOne(any());   // 只有 create 时查过重，resolve 未查库
        verify(valueOps).increment(ShortLinkService.CLICK_PREFIX + created);
    }

    @Test
    void 缓存未命中_库回源并回填缓存() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(Mockito.<ShortLink>any())).thenReturn(1);
        String code = service.create("https://example.com/b");

        // 缓存失效场景：get 返回 null，库里有
        when(valueOps.get(ShortLinkService.CACHE_PREFIX + code)).thenReturn(null);
        // 库查询：第一次 create 查重 null，第二次 resolve 查到记录
        ShortLink link = new ShortLink();
        link.setShortCode(code);
        link.setLongUrl("https://example.com/b");
        link.setClickCount(0L);
        Mockito.reset(mapper);
        when(mapper.selectOne(any())).thenReturn(link);
        when(valueOps.increment(anyString())).thenReturn(1L);

        assertEquals("https://example.com/b", service.resolve(code));
        // 回填缓存（TTL 24h）——create 时预写 1 次 + resolve 回源回填 1 次 = 共 2 次
        verify(valueOps, Mockito.times(2)).set(eq(ShortLinkService.CACHE_PREFIX + code),
                eq("https://example.com/b"), Mockito.<Duration>any());
    }

    @Test
    void 同一长URL重复发号_返回已有短码不重复入库() {
        ShortLink exist = new ShortLink();
        exist.setShortCode("exist1");
        exist.setLongUrl("https://example.com/c");
        when(mapper.selectOne(any())).thenReturn(exist);

        String code = service.create("https://example.com/c");
        assertEquals("exist1", code);
        verify(mapper, never()).insert(Mockito.<ShortLink>any());
    }

    @Test
    void 统计WriteBehind_累计到阈值100次_回写库并清零() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(Mockito.<ShortLink>any())).thenReturn(1);
        String code = service.create("https://example.com/d");

        when(valueOps.get(anyString())).thenReturn(null);            // resolve 走库回源
        ShortLink link = new ShortLink();
        link.setShortCode(code);
        link.setLongUrl("https://example.com/d");
        link.setClickCount(0L);
        Mockito.reset(mapper);
        when(mapper.selectOne(any())).thenReturn(link);
        // 连续返回：第 99 次（不刷库）、第 100 次（触发刷库）——thenReturn 连续返回做两段路径
        when(valueOps.increment(anyString())).thenReturn(99L, 100L);

        service.resolve(code);       // 99 次：不回写
        verify(mapper, never()).incrClick(anyString(), Mockito.anyLong());

        service.resolve(code);       // 100 次：%100==0 触发回写+清零
        verify(mapper).incrClick(code, ShortLinkService.FLUSH_THRESHOLD);
        verify(redis).delete(ShortLinkService.CLICK_PREFIX + code);
    }

    @Test
    void 发号人_记录的是登录态userId() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(Mockito.<ShortLink>any())).thenReturn(1);
        service.create("https://example.com/e");
        org.mockito.ArgumentCaptor<ShortLink> captor = org.mockito.ArgumentCaptor.forClass(ShortLink.class);
        verify(mapper).insert(captor.capture());
        assertEquals(1001L, captor.getValue().getCreatorId());
    }

    private String getLastCreatedCode() {
        org.mockito.ArgumentCaptor<ShortLink> captor = org.mockito.ArgumentCaptor.forClass(ShortLink.class);
        verify(mapper, Mockito.atLeastOnce()).insert(captor.capture());
        return captor.getValue().getShortCode();
    }
}
