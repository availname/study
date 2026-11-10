package com.example.mqmall.compensation;

import com.example.mqmall.order.OrderDao;
import com.example.mqmall.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 超时订单扫表兜底（教程 stage4/02 的第二道网）。
 * 延迟消息负责及时性；本任务防止消息丢失/延迟组件故障导致的漏单。
 * 与延迟消费者共用 closeIfTimeout（CAS 幂等），重复执行无害。
 */
@Component
public class OrderSweepJob {
    private static final Logger log = LoggerFactory.getLogger(OrderSweepJob.class);

    private final OrderDao orderDao;
    private final OrderService orderService;
    private final long timeoutMinutes;

    public OrderSweepJob(OrderDao orderDao, OrderService orderService,
                         @Value("${mq.mall.order-timeout-ms:120000}") long timeoutMs) {
        this.orderDao = orderDao;
        this.orderService = orderService;
        this.timeoutMinutes = Math.max(1, timeoutMs / 60_000);
    }

    @Scheduled(fixedDelay = 20_000)
    public void sweep() {
        List<String> overdue = orderDao.findTimeoutPending((int) timeoutMinutes, 50);
        for (String orderNo : overdue) {
            orderService.closeIfTimeout(orderNo);
        }
        if (!overdue.isEmpty()) {
            log.info("[扫表兜底] 处理超时订单 {} 笔", overdue.size());
        }
    }
}
