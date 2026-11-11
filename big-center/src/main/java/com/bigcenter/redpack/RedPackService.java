package com.bigcenter.redpack;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bigcenter.auth.UserContext;
import com.bigcenter.common.BizException;
import com.bigcenter.common.ErrorCode;
import com.bigcenter.redpack.entity.RedPack;
import com.bigcenter.redpack.entity.RedPackDetail;
import com.bigcenter.redpack.mapper.RedPackDetailMapper;
import com.bigcenter.redpack.mapper.RedPackMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 抢红包服务（java-core/14 题二落地）：
 * 发 = 二倍均值预拆 + LPUSH N 份到 Redis List（重活放写少的一侧）；
 * 抢 = Set SADD 防重 + LPOP 原子认领（不超发/不重复，万人并发 O(1) 无锁）；
 * 明细 = MQ 异步落库（削峰，本模块提供 recordDetail 供消费者调用）；
 * 退款 = 24h TTL + 延迟消息（Redis 包剩余份数>0 → 走支付中心原路退款）。
 */
@Service
public class RedPackService {
    private static final Logger log = LoggerFactory.getLogger(RedPackService.class);

    static final String PKG_PREFIX = "bc:redpack:pkg:";
    static final String OWNER_PREFIX = "bc:redpack:owner:";
    static final String COUNT_PREFIX = "bc:redpack:left:";
    static final Duration PKG_TTL = Duration.ofHours(24);       // 24h 未领完 → 退款

    private final RedPackMapper packMapper;
    private final RedPackDetailMapper detailMapper;
    private final StringRedisTemplate redis;

    public RedPackService(RedPackMapper packMapper, RedPackDetailMapper detailMapper, StringRedisTemplate redis) {
        this.packMapper = packMapper;
        this.detailMapper = detailMapper;
        this.redis = redis;
    }

    /** 发红包：预拆 N 份 + LPUSH + TTL 24h（延迟退款由延迟消息消费者调 refundIfExpired） */
    public long create(long totalFen, int count) {
        List<Long> parts = RedPackSplitter.split(totalFen, count);
        RedPack pack = new RedPack();
        pack.setOwnerId(UserContext.get());
        pack.setTotalFen(totalFen);
        pack.setCount(count);
        pack.setGrabbedCount(0);
        pack.setStatus(RedPack.ONGOING);
        packMapper.insert(pack);

        String pkgKey = PKG_PREFIX + pack.getId();
        // LPUSH 逐份入列（重活放发红包这一侧——写少）
        redis.opsForList().rightPushAll(pkgKey, parts.stream().map(String::valueOf).toList());
        redis.expire(pkgKey, PKG_TTL);
        redis.opsForValue().set(COUNT_PREFIX + pack.getId(), String.valueOf(count), PKG_TTL);
        return pack.getId();
    }

    /**
     * 抢红包：返回金额（分）；null 表示红包已抢完。
     * Set SADD 返回 0 = 已抢过（防重复）；LPOP nil = 抢完（防超发）——两条都是单命令原子。
     */
    public Long grab(long packId, long userId) {
        // ① 防重复抢：SADD 返回 0 说明已在"已抢名单"里
        Long added = redis.opsForSet().add(OWNER_PREFIX + packId, String.valueOf(userId));
        if (added == null || added == 0) {
            throw new BizException(ErrorCode.REDPACK_ALREADY_GRABBED);
        }
        // ② 原子认领：LPOP 一次弹出一份，天然不超发
        String amount = redis.opsForList().leftPop(PKG_PREFIX + packId);
        if (amount == null) {
            // 没抢到要把名单退回去（否则这个人永远不能再抢下一轮）
            redis.opsForSet().remove(OWNER_PREFIX + packId, String.valueOf(userId));
            throw new BizException(ErrorCode.REDPACK_GRABBED_OUT);
        }
        redis.opsForValue().decrement(COUNT_PREFIX + packId);
        // ③ 明细 MQ 异步落库（生产走 ReliableMessagePublisher；单测直接钉 recordDetail 语义）
        recordDetail(packId, userId, Long.parseLong(amount));
        if (isFinished(packId)) {
            packMapper.casStatus(packId, RedPack.ONGOING, RedPack.FINISHED);
        }
        return Long.parseLong(amount);
    }

    /** 明细落库（MQ 消费者调用；抢的路径直接同步调用简化演示——削峰语义由消费者测试覆盖） */
    public void recordDetail(long packId, long userId, long amountFen) {
        RedPackDetail detail = new RedPackDetail();
        detail.setPackId(packId);
        detail.setUserId(userId);
        detail.setAmountFen(amountFen);
        try {
            detailMapper.insert(detail);
            packMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RedPack>()
                    .eq(RedPack::getId, packId)
                    .setSql("grabbed_count = grabbed_count + 1"));
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("明细重复消费幂等跳过 packId={} userId={}", packId, userId);
        }
    }

    /** 是否抢完（剩余份数 0） */
    boolean isFinished(long packId) {
        String left = redis.opsForValue().get(COUNT_PREFIX + packId);
        return left != null && Long.parseLong(left) <= 0;
    }

    /**
     * 24h 延迟退款（TTL+DLX 消费者调用）：
     * 红包主体还在"进行中"（=没抢完）→ 剩余份数×单份理论值按明细差额退款——教学简化：
     * 未领完即整包转 REFUNDING→调用方走支付中心 refund 原路退回，明细与总额差额已含在未领份数中。
     */
    public boolean refundIfExpired(long packId) {
        RedPack pack = packMapper.selectById(packId);
        if (pack == null || pack.getStatus() != RedPack.ONGOING) {
            return false;               // 已抢完/已退款——延迟消息天然幂等
        }
        int n = packMapper.casStatus(packId, RedPack.ONGOING, RedPack.REFUNDING);
        if (n == 0) {
            return false;               // 并发竞争输了：别的退款/抢完先到
        }
        // 计算未领金额 = 总额 − 已领明细合计（对账思想：以明细为准）
        long grabbed = detailMapper.selectList(new LambdaQueryWrapper<RedPackDetail>()
                        .eq(RedPackDetail::getPackId, packId)).stream()
                .mapToLong(RedPackDetail::getAmountFen).sum();
        long refundFen = pack.getTotalFen() - grabbed;
        log.info("红包过期退款 packId={} refundFen={}", packId, refundFen);
        packMapper.casStatus(packId, RedPack.REFUNDING, RedPack.REFUNDED);
        return true;
    }

    /** 拆分结果查看（发红包人看手气） */
    public List<RedPackDetail> details(long packId) {
        return detailMapper.selectList(new LambdaQueryWrapper<RedPackDetail>()
                .eq(RedPackDetail::getPackId, packId).orderByAsc(RedPackDetail::getId));
    }
}
