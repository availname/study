package com.bigcenter.shortlink;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bigcenter.auth.UserContext;
import com.bigcenter.common.BizException;
import com.bigcenter.common.ErrorCode;
import com.bigcenter.shortlink.entity.ShortLink;
import com.bigcenter.shortlink.mapper.ShortLinkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 短链服务（java-core/12 题一落地）：
 * 发号 = 雪花 ID → Base62（唯一性由发号器保证，不用哈希截断——会碰撞）；
 * 跳转 = 布隆拦截 → Redis 缓存 → MySQL 回源（缓存穿透三件套）；
 * 统计 = Redis INCR 累积 + 阈值回写 MySQL（Write Behind，community 同款）。
 */
@Service
public class ShortLinkService {
    private static final Logger log = LoggerFactory.getLogger(ShortLinkService.class);

    static final String CACHE_PREFIX = "bc:shortlink:url:";
    static final String CLICK_PREFIX = "bc:shortlink:click:";
    static final long FLUSH_THRESHOLD = 100;       // 累计 100 次回写一次库
    static final Duration CACHE_TTL = Duration.ofHours(24);

    private final ShortLinkMapper mapper;
    private final StringRedisTemplate redis;
    private final BloomFilter bloom = new BloomFilter(1 << 20);   // 百万级短码位图

    public ShortLinkService(ShortLinkMapper mapper, StringRedisTemplate redis) {
        this.mapper = mapper;
        this.redis = redis;
    }

    /** 发号：同一长 URL 唯一索引防重——重复创建返回已有短码 */
    public String create(String longUrl) {
        ShortLink exist = mapper.selectOne(new LambdaQueryWrapper<ShortLink>().eq(ShortLink::getLongUrl, longUrl));
        if (exist != null) {
            return exist.getShortCode();
        }
        ShortLink link = new ShortLink();
        link.setShortCode(Base62.encode(com.bigcenter.common.Ids.nextId()));
        link.setLongUrl(longUrl);
        link.setCreatorId(UserContext.get());
        link.setClickCount(0L);
        try {
            mapper.insert(link);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BizException(ErrorCode.SHORT_LINK_NOT_FOUND, "短码冲突，请重试");
        }
        bloom.add(link.getShortCode());
        redis.opsForValue().set(CACHE_PREFIX + link.getShortCode(), longUrl, CACHE_TTL);
        return link.getShortCode();
    }

    /** 跳转解析：布隆 → 缓存 → 库；未命中返回 null（Controller 转 404） */
    public String resolve(String shortCode) {
        // ① 布隆过滤器：一定不存在则直接拒绝——恶意短码打不到库（穿透防护第一层）
        if (!bloom.mightContain(shortCode)) {
            return null;
        }
        // ② Redis 缓存
        String cached = redis.opsForValue().get(CACHE_PREFIX + shortCode);
        if (cached != null) {
            countClick(shortCode);
            return cached;
        }
        // ③ 库回源 + 回填缓存
        ShortLink link = mapper.selectOne(new LambdaQueryWrapper<ShortLink>().eq(ShortLink::getShortCode, shortCode));
        if (link == null) {
            log.warn("traceId={} 短链库中也未命中 code={}", MDC.get("traceId"), shortCode);
            return null;
        }
        redis.opsForValue().set(CACHE_PREFIX + shortCode, link.getLongUrl(), CACHE_TTL);
        countClick(shortCode);
        return link.getLongUrl();
    }

    /** 统计：Redis INCR 累积；达到阈值异步回写 MySQL 并清零（Write Behind） */
    private void countClick(String shortCode) {
        Long total = redis.opsForValue().increment(CLICK_PREFIX + shortCode);
        if (total != null && total % FLUSH_THRESHOLD == 0) {
            long delta = FLUSH_THRESHOLD;
            mapper.incrClick(shortCode, delta);
            redis.delete(CLICK_PREFIX + shortCode);
            log.info("traceId={} 短链统计回写 code={} delta={}", MDC.get("traceId"), shortCode, delta);
        }
    }

    /** 累积计数读取（对账用：Redis 累积 + 库已落之和） */
    public long clickCount(String shortCode) {
        String pending = redis.opsForValue().get(CLICK_PREFIX + shortCode);
        ShortLink link = mapper.selectOne(new LambdaQueryWrapper<ShortLink>().eq(ShortLink::getShortCode, shortCode));
        long dbCount = link == null ? 0 : link.getClickCount();
        return dbCount + (pending == null ? 0 : Long.parseLong(pending));
    }
}
