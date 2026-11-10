package com.example.mqmall.order;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 订单表读写。状态机：10待支付 → 20已支付 / 50超时关闭（教程 stage4/02） */
@Repository
public class OrderDao {

    /** 订单状态常量 */
    public static final int PENDING_PAY = 10;
    public static final int PAID = 20;
    public static final int CLOSED = 50;

    private final NamedParameterJdbcTemplate jdbc;

    public OrderDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record OrderRow(Long id, String orderNo, Long userId, Long skuId,
                           Integer count, Integer amount, Integer status) {}

    public void insert(String orderNo, Long userId, Long skuId, int count, int amount, Integer seckillFlag) {
        jdbc.update("""
                INSERT INTO t_order (order_no, user_id, sku_id, count, amount, status, seckill_flag)
                VALUES (:no, :uid, :sku, :cnt, :amt, :st, :flag)
                """, new MapSqlParameterSource()
                .addValue("no", orderNo).addValue("uid", userId).addValue("sku", skuId)
                .addValue("cnt", count).addValue("amt", amount)
                .addValue("st", PENDING_PAY).addValue("flag", seckillFlag));
    }

    public Optional<OrderRow> find(String orderNo) {
        List<OrderRow> rows = jdbc.getJdbcTemplate().query("""
                        SELECT id, order_no, user_id, sku_id, count, amount, status
                        FROM t_order WHERE order_no = ?
                        """,
                (rs, i) -> new OrderRow(rs.getLong("id"), rs.getString("order_no"),
                        rs.getLong("user_id"), rs.getLong("sku_id"),
                        rs.getInt("count"), rs.getInt("amount"), rs.getInt("status")),
                orderNo);
        return rows.stream().findFirst();
    }

    /** CAS 支付：只有待支付才能置为已支付（教程 stage4/02 并发竞态的核心防线） */
    public int casPay(String orderNo) {
        return jdbc.getJdbcTemplate().update(
                "UPDATE t_order SET status = 20, paid_at = NOW(3) WHERE order_no = ? AND status = 10", orderNo);
    }

    /** CAS 关单：只有待支付才能关闭，与支付回调天然互斥 */
    public int casClose(String orderNo) {
        return jdbc.getJdbcTemplate().update(
                "UPDATE t_order SET status = 50, closed_at = NOW(3) WHERE order_no = ? AND status = 10", orderNo);
    }

    /** 扫表兜底：超时未支付的普通订单号（教程 stage4/02 双保险）。
     *  秒杀单排除：演示环境秒杀单无支付环节，关单会造成 Redis/DB 漂移；
     *  真实系统秒杀单有短支付窗口 + 超时释放（Redis 归还），留作扩展练习。 */
    public List<String> findTimeoutPending(int beforeMinutes, int limit) {
        return jdbc.getJdbcTemplate().queryForList("""
                SELECT order_no FROM t_order
                WHERE status = 10 AND seckill_flag IS NULL AND created_at < NOW(3) - INTERVAL ? MINUTE
                ORDER BY id LIMIT ?
                """, String.class, beforeMinutes, limit);
    }

    public Map<String, Long> countByStatus() {
        Map<String, Long> result = new java.util.HashMap<>();
        jdbc.queryForList("SELECT status, COUNT(*) c FROM t_order GROUP BY status",
                        new MapSqlParameterSource())
                .forEach(row -> result.put("status_" + row.get("status"), ((Number) row.get("c")).longValue()));
        return result;
    }

    /** 秒杀订单数（一人一单兜底后的实际建单量，与 Redis 预扣数对账） */
    public long countSeckillOrders() {
        Long c = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM t_order WHERE seckill_flag = 1", Long.class);
        return c == null ? 0 : c;
    }
}
