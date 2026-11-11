package com.bigcenter.pay;

import com.bigcenter.common.BizException;
import com.bigcenter.pay.entity.PayOrder;
import com.bigcenter.pay.mapper.PayCallbackMapper;
import com.bigcenter.pay.mapper.PayOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 支付四关卡：状态机 CAS/回调幂等/金额防线/验签——java-core/15 期末考试验收 */
class PaymentServiceTest {

    private PayOrderMapper orderMapper;
    private PayCallbackMapper callbackMapper;
    private PaymentFacade facade;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        orderMapper = Mockito.mock(PayOrderMapper.class);
        callbackMapper = Mockito.mock(PayCallbackMapper.class);
        facade = Mockito.mock(PaymentFacade.class);
        service = new PaymentService(orderMapper, callbackMapper, facade);
    }

    private PayOrder order(int status, long amountFen) {
        PayOrder o = new PayOrder();
        o.setPayOrderNo("P1001");
        o.setUserId(1001L);
        o.setChannelCode("MOCK");
        o.setAmountFen(amountFen);
        o.setStatus(status);
        return o;
    }

    @Test
    void 回调验签失败_直接拒_不落流水不推状态() {
        BizException e = assertThrows(BizException.class, () ->
                service.handleCallback("trade-1", "P1001", 100, "bad-sign"));
        assertEquals(4004, e.getErrorCode().code());
        verify(callbackMapper, never()).insert(Mockito.<com.bigcenter.pay.entity.PayCallback>any());
        verify(orderMapper, never()).casPaid(any());
    }

    @Test
    void 回调金额与本地订单不一致_拒处理_防一分钱支付一百元订单() {
        when(orderMapper.selectOne(any())).thenReturn(order(PayOrder.WAIT_PAY, 9900));
        String sign = PaymentService.MockSign.of("P1001", 1);
        BizException e = assertThrows(BizException.class, () ->
                service.handleCallback("trade-1", "P1001", 1, sign));    // 回调 1 分，订单 99 元
        assertEquals(4004, e.getErrorCode().code());
        verify(orderMapper, never()).casPaid(any());
    }

    @Test
    void 重复回调_流水唯一索引冲突_幂等跳过不推状态() {
        when(orderMapper.selectOne(any())).thenReturn(order(PayOrder.WAIT_PAY, 100));
        // 幂等第一层：流水表唯一索引冲突
        when(callbackMapper.insert(Mockito.<com.bigcenter.pay.entity.PayCallback>any()))
                .thenThrow(new DuplicateKeyException("uk_channel_trade"));
        String result = service.handleCallback("trade-1", "P1001", 100,
                PaymentService.MockSign.of("P1001", 100));
        assertEquals("DUPLICATE", result);
        // 幂等生效：不碰状态机
        verify(orderMapper, never()).casPaid(any());
    }

    @Test
    void 正常回调_CAS成功_返回SUCCESS() {
        when(orderMapper.selectOne(any())).thenReturn(order(PayOrder.WAIT_PAY, 100));
        when(callbackMapper.insert(Mockito.<com.bigcenter.pay.entity.PayCallback>any())).thenReturn(1);
        when(orderMapper.casPaid("P1001")).thenReturn(1);        // CAS 赢

        String result = service.handleCallback("trade-1", "P1001", 100,
                PaymentService.MockSign.of("P1001", 100));
        assertEquals("SUCCESS", result);
        verify(orderMapper).casPaid("P1001");
    }

    @Test
    void CAS输方分支_状态已是已支付_幂等忽略() {
        when(orderMapper.selectOne(any())).thenReturn(order(PayOrder.PAID, 100));
        when(callbackMapper.insert(Mockito.<com.bigcenter.pay.entity.PayCallback>any())).thenReturn(1);
        when(orderMapper.casPaid("P1001")).thenReturn(0);        // CAS 输（已被并发回调改成已支付）

        String result = service.handleCallback("trade-2", "P1001", 100,
                PaymentService.MockSign.of("P1001", 100));
        assertEquals("DUPLICATE", result);
    }

    @Test
    void CAS输方分支_关单赢了支付_返回需退款() {
        // 第一次查（金额比对）返回待支付；CAS 输后第二次查返回已关闭——thenReturn 连续返回
        when(orderMapper.selectOne(any()))
                .thenReturn(order(PayOrder.WAIT_PAY, 100))
                .thenReturn(order(PayOrder.CLOSED, 100));
        when(callbackMapper.insert(Mockito.<com.bigcenter.pay.entity.PayCallback>any())).thenReturn(1);
        when(orderMapper.casPaid("P1001")).thenReturn(0);

        String result = service.handleCallback("trade-3", "P1001", 100,
                PaymentService.MockSign.of("P1001", 100));
        assertEquals("CLOSED_NEED_REFUND", result);              // 竞态输方走退款分支，不丢钱
    }

    @Test
    void 掉单补偿_渠道查到已支付_补状态成功() {
        PaymentFacade realFacade = Mockito.mock(PaymentFacade.class);
        when(realFacade.queryOrder("MOCK", "P1001")).thenReturn("SUCCESS");   // 补偿走 facade.queryOrder 门面入口
        when(orderMapper.selectOne(any())).thenReturn(order(PayOrder.WAIT_PAY, 100));
        when(orderMapper.casPaid("P1001")).thenReturn(1);

        PaymentService svc = new PaymentService(orderMapper, callbackMapper, realFacade);
        assertEquals("COMPENSATED", svc.compensateCheck("P1001"));
        verify(orderMapper).casPaid("P1001");
    }

    @Test
    void 掉单补偿_已有终态_不再查渠道() {
        when(orderMapper.selectOne(any())).thenReturn(order(PayOrder.PAID, 100));
        PaymentFacade realFacade = Mockito.mock(PaymentFacade.class);
        PaymentService svc = new PaymentService(orderMapper, callbackMapper, realFacade);

        assertEquals("NO_NEED", svc.compensateCheck("P1001"));
        verify(realFacade, never()).route(any());                // 终态不查渠道
        verify(orderMapper, never()).casPaid(any());
    }

    @Test
    void 退款CAS_仅已支付可退_状态冲突报4002() {
        when(orderMapper.casRefund("P1001")).thenReturn(1);
        service.refund("P1001");                                 // 已支付→已退款成功

        when(orderMapper.casRefund("P1001")).thenReturn(0);
        BizException e = assertThrows(BizException.class, () -> service.refund("P1001"));
        assertEquals(4002, e.getErrorCode().code());
    }
}
