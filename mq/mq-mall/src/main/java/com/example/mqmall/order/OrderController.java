package com.example.mqmall.order;

import com.example.mqmall.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** 订单接口 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderDao orderDao;

    public OrderController(OrderService orderService, OrderDao orderDao) {
        this.orderService = orderService;
        this.orderDao = orderDao;
    }

    /** 下单（锁定库存 + 事务消息 + 超时关单延迟消息） */
    @PostMapping
    public ApiResponse<String> create(@RequestParam long userId,
                                      @RequestParam long skuId,
                                      @RequestParam(defaultValue = "1") int count) {
        return ApiResponse.ok(orderService.createOrder(userId, skuId, count));
    }

    /** 支付（CAS 状态机 + 事务消息，幂等） */
    @PostMapping("/{orderNo}/pay")
    public ApiResponse<Void> pay(@PathVariable String orderNo) {
        orderService.pay(orderNo);
        return ApiResponse.ok(null);
    }

    /** 查询订单 */
    @GetMapping("/{orderNo}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String orderNo) {
        return orderDao.find(orderNo)
                .<ApiResponse<Map<String, Object>>>map(o -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("orderNo", o.orderNo());
                    view.put("userId", o.userId());
                    view.put("skuId", o.skuId());
                    view.put("count", o.count());
                    view.put("amount", o.amount());
                    view.put("status", o.status());
                    view.put("statusName", statusName(o.status()));
                    return ApiResponse.ok(view);
                })
                .orElseGet(() -> ApiResponse.error(404, "订单不存在"));
    }

    private static String statusName(int status) {
        return switch (status) {
            case OrderDao.PENDING_PAY -> "待支付";
            case OrderDao.PAID -> "已支付";
            case OrderDao.CLOSED -> "超时关闭";
            default -> "未知";
        };
    }
}
