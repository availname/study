package com.example.mqmall.order;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

/** 库存表 + 流水。锁定式库存：下单锁定 → 支付实扣（记流水）→ 超时释放（记流水） */
@Repository
public class StockDao {

    private final NamedParameterJdbcTemplate jdbc;

    public StockDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 锁库存：WHERE stock >= count 是防超卖的最后防线（教程 stage4/01） */
    public int lock(long skuId, int count) {
        return jdbc.update("""
                UPDATE t_stock SET stock = stock - :cnt
                WHERE sku_id = :sku AND stock >= :cnt
                """, new MapSqlParameterSource()
                .addValue("cnt", count).addValue("sku", skuId));
    }

    /** 释放（超时关单/取消） */
    public int release(long skuId, int count) {
        return jdbc.update("UPDATE t_stock SET stock = stock + :cnt WHERE sku_id = :sku",
                new MapSqlParameterSource().addValue("cnt", count).addValue("sku", skuId));
    }

    /** 库存流水：对账的数据来源 */
    public void log(String orderNo, long skuId, int count, String action) {
        jdbc.update("""
                INSERT INTO t_stock_log (order_no, sku_id, count, action) VALUES (:no, :sku, :cnt, :act)
                """, new MapSqlParameterSource()
                .addValue("no", orderNo).addValue("sku", skuId)
                .addValue("cnt", count).addValue("act", action));
    }

    public Map<String, Object> getStock(long skuId) {
        return jdbc.queryForMap("SELECT sku_id, stock, total FROM t_stock WHERE sku_id = :sku",
                new MapSqlParameterSource("sku", skuId));
    }

    /** 按动作汇总流水：LOCK/RELEASE/DEDUCT 各自总量，对账用 */
    public Map<String, Long> sumByAction() {
        Map<String, Long> result = new java.util.HashMap<>();
        jdbc.queryForList("SELECT action, SUM(count) c FROM t_stock_log GROUP BY action",
                        new MapSqlParameterSource())
                .forEach(row -> result.put((String) row.get("action"), ((Number) row.get("c")).longValue()));
        return result;
    }
}
