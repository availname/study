package com.example.mqmall.order;

import com.example.mqmall.common.BizException;
import com.example.mqmall.common.Ids;
import com.example.mqmall.messaging.Messages.OrderCreatedEvent;
import com.example.mqmall.messaging.Messages.OrderPaidEvent;
import com.example.mqmall.messaging.ReliableMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单核心服务（教程 stage3/01 本地消息表 + stage4/02 超时关单）。
 * 事务边界：锁库存 + 建订单 + 写流水 + 写消息表，同一个本地事务原子提交；
 * 事务提交后才发 MQ（afterCommit），发送失败由补偿任务兜底。
 */
@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final int UNIT_PRICE = 100;   // 演示单价 1 元（100 分）

    private final OrderDao orderDao;
    private final StockDao stockDao;
    private final ReliableMessagePublisher publisher;

    public OrderService(OrderDao orderDao, StockDao stockDao,
                        ReliableMessagePublisher publisher) {
        this.orderDao = orderDao;
        this.stockDao = stockDao;
        this.publisher = publisher;
    }

    /** 下单：锁库存 + 建订单 + 发"order.created"事件 + 发延迟消息（超时自动关单） */
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(long userId, long skuId, int count) {
        if (stockDao.lock(skuId, count) == 0) {
            throw new BizException("库存不足");
        }
        String orderNo = Ids.orderNo();
        int amount = count * UNIT_PRICE;
        orderDao.insert(orderNo, userId, skuId, count, amount, null);
        stockDao.log(orderNo, skuId, count, "LOCK");

        // ① 订单创建事件（消费者：审计/缓存重建等，演示幂等消费）
        //    直接传对象，publisher 内部统一序列化——不要自己 toJson（会双重序列化）
        publisher.sendInTransaction("mall.order", "order.created",
                "order-created-" + orderNo, new OrderCreatedEvent(orderNo, userId, skuId, count, amount, false));

        // ② 超时关单延迟消息：TTL 沉睡 → 死信转发 → order.timeout.queue 消费（stage4/02）
        publisher.sendInTransaction("", "order.delay.queue",
                "order-timeout-" + orderNo, orderNo);

        log.info("[下单成功] orderNo={} userId={} skuId={} count={}", orderNo, userId, skuId, count);
        return orderNo;
    }

    /** 支付：CAS 状态机 10→20，与超时关单并发互斥（教程 stage4/02 竞态核心） */
    @Transactional(rollbackFor = Exception.class)
    public void pay(String orderNo) {
        int n = orderDao.casPay(orderNo);
        if (n == 0) {
            OrderDao.OrderRow current = orderDao.find(orderNo)
                    .orElseThrow(() -> new BizException("订单不存在"));
            if (current.status() == OrderDao.PAID) {
                log.info("[支付幂等] 订单已支付，忽略重复支付 orderNo={}", orderNo);
                return;
            }
            if (current.status() == OrderDao.CLOSED) {
                throw new BizException("订单已超时关闭，若已扣款将自动退款（演示环境直接提示）");
            }
            throw new BizException("订单状态不允许支付");
        }
        OrderDao.OrderRow order = orderDao.find(orderNo).orElseThrow();
        stockDao.log(orderNo, order.skuId(), order.count(), "DEDUCT");   // 实扣流水

        publisher.sendInTransaction("mall.order", "order.paid",
                "order-paid-" + orderNo, new OrderPaidEvent(orderNo, order.userId(), order.skuId(), order.count(), order.amount()));

        log.info("[支付成功] orderNo={}", orderNo);
    }

    /** 超时关单（延迟消息消费者和扫表兜底共用，CAS 幂等） */
    @Transactional(rollbackFor = Exception.class)
    public boolean closeIfTimeout(String orderNo) {
        int n = orderDao.casClose(orderNo);
        if (n == 0) {
            return false;   // 已支付/已关闭 → 延迟消息天然幂等，什么都不做
        }
        OrderDao.OrderRow order = orderDao.find(orderNo).orElseThrow();
        stockDao.release(order.skuId(), order.count());
        stockDao.log(orderNo, order.skuId(), order.count(), "RELEASE");
        log.info("[超时关单] orderNo={} 库存已释放", orderNo);
        return true;
    }
}
