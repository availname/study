package com.example.mqmall.admin;

import com.example.mqmall.common.ApiResponse;
import com.example.mqmall.messaging.Messages.OrderCreatedEvent;
import com.example.mqmall.messaging.SendRecordDao;
import com.example.mqmall.order.OrderDao;
import com.example.mqmall.order.StockDao;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运维/演示端点：死信查看、消息状态、秒杀对账、可靠链路演练。
 * 对应教程 stage3（问题解决）与 stage4（预案）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final NamedParameterJdbcTemplate jdbc;
    private final OrderDao orderDao;
    private final StockDao stockDao;
    private final SendRecordDao sendRecordDao;
    private final RabbitTemplate rabbit;

    public AdminController(NamedParameterJdbcTemplate jdbc, OrderDao orderDao, StockDao stockDao,
                           SendRecordDao sendRecordDao, RabbitTemplate rabbit) {
        this.jdbc = jdbc;
        this.orderDao = orderDao;
        this.stockDao = stockDao;
        this.sendRecordDao = sendRecordDao;
        this.rabbit = rabbit;
    }

    /** 死信存档（stage3/05：失败可见可控） */
    @GetMapping("/dead-letters")
    public ApiResponse<List<Map<String, Object>>> deadLetters() {
        return ApiResponse.ok(jdbc.queryForList("""
                SELECT id, message_id, source_queue, reason, payload, created_at
                FROM t_mq_dead_letter ORDER BY id DESC LIMIT 50
                """, new MapSqlParameterSource()));
    }

    /** 发送记录状态分布（本地消息表健康度） */
    @GetMapping("/send-records")
    public ApiResponse<Map<String, Long>> sendRecords() {
        return ApiResponse.ok(sendRecordDao.countByStatus());
    }

    /** 秒杀/订单库存对账：DB 已锁定 = 秒杀单 + 普通活跃单（stage4/01 兜底） */
    @GetMapping("/reconcile")
    public ApiResponse<Map<String, Object>> reconcile() {
        Map<String, Object> stock = stockDao.getStock(1);
        long dbSeckillOrders = orderDao.countSeckillOrders();
        Long normalActive = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COALESCE(SUM(count),0) FROM t_order WHERE seckill_flag IS NULL AND status IN (10, 20)",
                Long.class);
        long normalCount = normalActive == null ? 0 : normalActive;
        long total = ((Number) stock.get("total")).longValue();
        long remain = ((Number) stock.get("stock")).longValue();

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("DB库存总量", total);
        view.put("DB剩余库存", remain);
        view.put("DB已锁定(总量-剩余)", total - remain);
        view.put("秒杀订单数", dbSeckillOrders);
        view.put("普通活跃订单数(count合计)", normalCount);
        view.put("流水汇总", stockDao.sumByAction());
        view.put("一致", (total - remain) == (dbSeckillOrders + normalCount));
        return ApiResponse.ok(view);
    }

    /** 可靠链路演练：发一条故意失败的消息，观察 重试3次 → 死信落库（约 3.5 秒后查 /dead-letters） */
    @PostMapping("/test-dlq")
    public ApiResponse<Void> testDlq() {
        OrderCreatedEvent poison = new OrderCreatedEvent("POISON-" + System.currentTimeMillis(),
                0L, 0L, 0, 0, true);
        // 直接投到队列（绕过本地消息表，纯演练消费端重试/死信链路）
        rabbit.convertAndSend("", "mall.order.created.queue", poison);
        return ApiResponse.ok(null);
    }

    /** 全局统计一览 */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("订单分布", orderDao.countByStatus());
        view.put("发送记录", sendRecordDao.countByStatus());
        view.put("死信数", jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_mq_dead_letter", new MapSqlParameterSource(), Long.class));
        view.put("消费去重记录数", jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_mq_consume_record", new MapSqlParameterSource(), Long.class));
        view.put("积分账户数", jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_points_account", new MapSqlParameterSource(), Long.class));
        return ApiResponse.ok(view);
    }
}
