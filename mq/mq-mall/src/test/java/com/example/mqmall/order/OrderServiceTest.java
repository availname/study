package com.example.mqmall.order;

import com.example.mqmall.common.BizException;
import com.example.mqmall.messaging.Messages;
import com.example.mqmall.messaging.ReliableMessagePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 订单 CAS 状态机单测（教程 stage4/02 支付 vs 关单竞态的行为锁定）。
 * 核心不变量：10→20 / 10→50 只能由 CAS 一方赢，输家绝不许碰库存和消息——
 * 这是防资损的关键防线（重复扣库存/给已关单发货都是生产事故）。
 */
class OrderServiceTest {

    private OrderDao orderDao;
    private StockDao stockDao;
    private ReliableMessagePublisher publisher;
    private OrderService service;

    @BeforeEach
    void setUp() {
        orderDao = mock(OrderDao.class);
        stockDao = mock(StockDao.class);
        publisher = mock(ReliableMessagePublisher.class);
        service = new OrderService(orderDao, stockDao, publisher);
    }

    private OrderDao.OrderRow row(String orderNo, int status) {
        return new OrderDao.OrderRow(1L, orderNo, 9L, 1L, 2, 200, status);
    }

    // ---------- 下单 ----------

    @Test
    void 下单_库存足够_锁库存建单并发两条消息() {
        when(stockDao.lock(1L, 2)).thenReturn(1);

        String orderNo = service.createOrder(9L, 1L, 2);

        verify(orderDao).insert(orderNo, 9L, 1L, 2, 200, null);
        verify(stockDao).log(orderNo, 1L, 2, "LOCK");
        // ① order.created 业务事件 ② order.delay.queue 超时关单延迟消息，缺一不可
        verify(publisher).sendInTransaction(eq("mall.order"), eq("order.created"),
                eq("order-created-" + orderNo), any(Messages.OrderCreatedEvent.class));
        verify(publisher).sendInTransaction(eq(""), eq("order.delay.queue"),
                eq("order-timeout-" + orderNo), eq(orderNo));
    }

    @Test
    void 下单_库存不足_抛异常且不建单不发消息() {
        when(stockDao.lock(anyLong(), anyInt())).thenReturn(0);

        assertThrows(BizException.class, () -> service.createOrder(9L, 1L, 2));

        verify(orderDao, never()).insert(anyString(), anyLong(), anyLong(), anyInt(), anyInt(), any());
        verifyNoInteractions(publisher);
    }

    // ---------- 支付（CAS 10→20） ----------

    @Test
    void 支付_CAS成功_记实扣流水并发orderPaid事件() {
        when(orderDao.casPay("O1")).thenReturn(1);
        when(orderDao.find("O1")).thenReturn(java.util.Optional.of(row("O1", OrderDao.PENDING_PAY)));

        service.pay("O1");

        verify(stockDao).log("O1", 1L, 2, "DEDUCT");
        verify(publisher).sendInTransaction(eq("mall.order"), eq("order.paid"),
                eq("order-paid-O1"), any(Messages.OrderPaidEvent.class));
    }

    @Test
    void 支付_重复支付_幂等返回不再发消息() {
        when(orderDao.casPay("O1")).thenReturn(0);   // CAS 输了 = 别的请求已支付
        when(orderDao.find("O1")).thenReturn(java.util.Optional.of(row("O1", OrderDao.PAID)));

        service.pay("O1");   // 不抛异常

        verifyNoInteractions(stockDao, publisher);   // 绝不二次扣库存、二次发货
    }

    @Test
    void 支付_订单已被超时关闭_抛异常提示退款() {
        when(orderDao.casPay("O1")).thenReturn(0);
        when(orderDao.find("O1")).thenReturn(java.util.Optional.of(row("O1", OrderDao.CLOSED)));

        BizException e = assertThrows(BizException.class, () -> service.pay("O1"));

        assertTrue(e.getMessage().contains("超时关闭"));
        verifyNoInteractions(stockDao, publisher);   // 给已关单发货 = 资损
    }

    @Test
    void 支付_订单不存在_抛异常() {
        when(orderDao.casPay("O404")).thenReturn(0);
        when(orderDao.find("O404")).thenReturn(java.util.Optional.empty());

        assertThrows(BizException.class, () -> service.pay("O404"));
    }

    @Test
    void 支付_未知状态_拒绝并兜底提示() {
        when(orderDao.casPay("O1")).thenReturn(0);
        when(orderDao.find("O1")).thenReturn(java.util.Optional.of(row("O1", 99)));

        assertThrows(BizException.class, () -> service.pay("O1"));
        verifyNoInteractions(publisher);
    }

    // ---------- 超时关单（CAS 10→50） ----------

    @Test
    void 关单_CAS成功_释放库存并记RELEASE流水() {
        when(orderDao.casClose("O1")).thenReturn(1);
        when(orderDao.find("O1")).thenReturn(java.util.Optional.of(row("O1", OrderDao.PENDING_PAY)));

        boolean closed = service.closeIfTimeout("O1");

        assertTrue(closed);
        verify(stockDao).release(1L, 2);
        verify(stockDao).log("O1", 1L, 2, "RELEASE");
    }

    @Test
    void 关单_CAS失败_返回false且绝不动库存() {
        when(orderDao.casClose("O1")).thenReturn(0);   // 已支付或已关闭 → 延迟消息天然幂等

        boolean closed = service.closeIfTimeout("O1");

        assertFalse(closed);
        verifyNoInteractions(stockDao);   // 已支付单释放库存 = 超卖事故
        verifyNoInteractions(publisher);
    }
}
