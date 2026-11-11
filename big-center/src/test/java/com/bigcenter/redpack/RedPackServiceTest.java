package com.bigcenter.redpack;

import com.bigcenter.auth.UserContext;
import com.bigcenter.common.BizException;
import com.bigcenter.redpack.entity.RedPack;
import com.bigcenter.redpack.entity.RedPackDetail;
import com.bigcenter.redpack.mapper.RedPackDetailMapper;
import com.bigcenter.redpack.mapper.RedPackMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 抢红包核心语义：SADD 防重/LPOP 原子认领不超发/过期退款 CAS——14 题二验收 */
class RedPackServiceTest {

    private RedPackMapper packMapper;
    private RedPackDetailMapper detailMapper;
    private StringRedisTemplate redis;
    private ListOperations<String, String> listOps;
    private SetOperations<String, String> setOps;
    private ValueOperations<String, String> valueOps;
    private RedPackService service;

    @BeforeEach
    void setUp() {
        packMapper = Mockito.mock(RedPackMapper.class);
        detailMapper = Mockito.mock(RedPackDetailMapper.class);
        redis = Mockito.mock(StringRedisTemplate.class);
        listOps = Mockito.mock(ListOperations.class);
        setOps = Mockito.mock(SetOperations.class);
        valueOps = Mockito.mock(ValueOperations.class);
        // 链式 mock：先固定各 ops 返回同一实例
        when(redis.opsForList()).thenReturn(listOps);
        when(redis.opsForSet()).thenReturn(setOps);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(packMapper.insert(Mockito.<RedPack>any())).thenAnswer(inv -> {
            inv.getArgument(0, RedPack.class).setId(1L);
            return 1;
        });
        service = new RedPackService(packMapper, detailMapper, redis);
        UserContext.set(1001L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void 发红包_预拆份数入List并设置24hTTL() {
        long packId = service.create(10000, 7);
        assertEquals(1L, packId);
        // LPUSH 了 7 份
        verify(listOps).rightPushAll(eq(RedPackService.PKG_PREFIX + "1"), any(List.class));
        verify(redis).expire(eq(RedPackService.PKG_PREFIX + "1"), Mockito.any());
        // 剩余份数计数
        verify(valueOps).set(eq(RedPackService.COUNT_PREFIX + "1"), eq("7"), Mockito.any());
    }

    @Test
    void 抢红包_SADD防重_第二次抢抛3003() {
        when(setOps.add(RedPackService.OWNER_PREFIX + "1", "2001")).thenReturn(1L, 0L);
        when(listOps.leftPop(RedPackService.PKG_PREFIX + "1")).thenReturn("300", "300");

        Long first = service.grab(1L, 2001L);
        assertEquals(300L, first);
        // 第二次抢：SADD 返回 0 → 已抢过
        BizException e = assertThrows(BizException.class, () -> service.grab(1L, 2001L));
        assertEquals(3003, e.getErrorCode().code());
    }

    @Test
    void 抢红包_LPOP返回nil_抢完抛3002_并把名单退回() {
        when(setOps.add(anyString(), anyString())).thenReturn(1L);
        when(listOps.leftPop(RedPackService.PKG_PREFIX + "1")).thenReturn(null);

        BizException e = assertThrows(BizException.class, () -> service.grab(1L, 2002L));
        assertEquals(3002, e.getErrorCode().code());
        // 关键：没抢到必须从"已抢名单"退回，否则该用户永远无法抢下一轮
        verify(setOps).remove(RedPackService.OWNER_PREFIX + "1", "2002");
        verify(detailMapper, never()).insert(Mockito.<RedPackDetail>any());
    }

    @Test
    void 抢红包_成功认领_明细落库_抢完推进状态机() {
        when(setOps.add(anyString(), anyString())).thenReturn(1L);
        when(listOps.leftPop(RedPackService.PKG_PREFIX + "1")).thenReturn("300");
        when(valueOps.get(RedPackService.COUNT_PREFIX + "1")).thenReturn("0");   // 递减后剩余 0

        Long amount = service.grab(1L, 2003L);
        assertEquals(300L, amount);
        ArgumentCaptor<RedPackDetail> captor = ArgumentCaptor.forClass(RedPackDetail.class);
        verify(detailMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getPackId());         // Long 装箱：packId 存 1L
        assertEquals(300L, captor.getValue().getAmountFen());
        // 抢完 → 状态机 CAS ONGOING→FINISHED
        verify(packMapper).casStatus(1L, RedPack.ONGOING, RedPack.FINISHED);
    }

    @Test
    void 抢红包_未抢完_不推状态机() {
        when(setOps.add(anyString(), anyString())).thenReturn(1L);
        when(listOps.leftPop(RedPackService.PKG_PREFIX + "1")).thenReturn("300");
        when(valueOps.get(RedPackService.COUNT_PREFIX + "1")).thenReturn("5");   // 还有 5 份

        service.grab(1L, 2004L);
        verify(packMapper, never()).casStatus(any(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    void 过期退款_已抢完的红包_幂等跳过() {
        RedPack finished = new RedPack();
        finished.setId(1L);
        finished.setTotalFen(10000L);
        finished.setStatus(RedPack.FINISHED);
        when(packMapper.selectById(1L)).thenReturn(finished);

        assertFalse(service.refundIfExpired(1L));
        verify(packMapper, never()).casStatus(any(), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    void 过期退款_未抢完_退差额为总额减已领明细() {
        RedPack ongoing = new RedPack();
        ongoing.setId(1L);
        ongoing.setTotalFen(10000L);
        ongoing.setStatus(RedPack.ONGOING);
        when(packMapper.selectById(1L)).thenReturn(ongoing);
        when(packMapper.casStatus(1L, RedPack.ONGOING, RedPack.REFUNDING)).thenReturn(1);   // CAS 赢
        RedPackDetail d = new RedPackDetail();
        d.setPackId(1L);
        d.setUserId(2001L);
        d.setAmountFen(3000L);
        when(detailMapper.selectList(any())).thenReturn(List.of(d));

        assertTrue(service.refundIfExpired(1L));
        // 退款推进 REFUNDING→REFUNDED（实际退款金额 7000 由支付中心执行，教学断言状态机）
        verify(packMapper).casStatus(1L, RedPack.REFUNDING, RedPack.REFUNDED);
    }

    @Test
    void 过期退款_CAS输给并发_返回false不重复退() {
        RedPack ongoing = new RedPack();
        ongoing.setId(1L);
        ongoing.setStatus(RedPack.ONGOING);
        when(packMapper.selectById(1L)).thenReturn(ongoing);
        when(packMapper.casStatus(1L, RedPack.ONGOING, RedPack.REFUNDING)).thenReturn(0);   // CAS 输

        assertFalse(service.refundIfExpired(1L));
        verify(packMapper, never()).casStatus(1L, RedPack.REFUNDING, RedPack.REFUNDED);
    }
}
