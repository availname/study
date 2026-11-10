package com.example.learn.enroll;

import com.example.learn.user.UserContext;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.learn.common.ApiResponse;
import com.example.learn.common.BizException;
import com.example.learn.common.Ids;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 我的抢课订单：轮询结果、支付（CAS）、我的课程列表。
 * 支付与超时取消的竞态由 CAS 状态机保证（教程 stage4/02）。
 */
@RestController
@RequestMapping("/api/enroll/orders")
public class EnrollOrderController {

    private final NamedParameterJdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public EnrollOrderController(NamedParameterJdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> my(@RequestHeader("Authorization") String token) {
        long userId = com.example.learn.user.UserContext.get();
        return ApiResponse.ok(jdbc.queryForList("""
                SELECT o.order_no, o.course_id, c.title, o.status, o.created_at
                FROM t_enroll_order o JOIN t_course c ON c.id = o.course_id
                WHERE o.user_id = :uid ORDER BY o.id DESC
                """, new MapSqlParameterSource("uid", userId)));
    }

    /** 支付抢课订单：CAS 待支付→已支付（与超时取消并发互斥，教程 stage4/02） */
    @PostMapping("/{orderNo}/pay")
    public ApiResponse<String> pay(@RequestHeader("Authorization") String token,
                                   @PathVariable String orderNo) {
        long userId = com.example.learn.user.UserContext.get();
        int n = jdbc.getJdbcTemplate().update(
                "UPDATE t_enroll_order SET status = 1, paid_at = NOW(3) WHERE order_no = ? AND user_id = ? AND status = 0",
                orderNo, userId);
        if (n == 0) throw new BizException("订单不存在或已取消/已支付");
        jdbc.getJdbcTemplate().update("UPDATE t_course c JOIN t_enroll_order o ON o.course_id = c.id " +
                "SET c.enrolled = c.enrolled + 1 WHERE o.order_no = ?", orderNo);
        return ApiResponse.ok("支付成功");
    }

    /** 取消抢课订单：CAS 待支付→已取消 + Redis 名额回补（Lua 防重复回补） */
    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<String> cancel(@RequestHeader("Authorization") String token,
                                      @PathVariable String orderNo) {
        long userId = com.example.learn.user.UserContext.get();
        int n = jdbc.getJdbcTemplate().update(
                "UPDATE t_enroll_order SET status = 2, cancel_at = NOW(3) WHERE order_no = ? AND user_id = ? AND status = 0",
                orderNo, userId);
        if (n == 0) throw new BizException("订单不存在或状态不允许取消");
        Map<String, Object> order = jdbc.getJdbcTemplate().queryForMap(
                "SELECT course_id FROM t_enroll_order WHERE order_no = ?", orderNo);
        long courseId = ((Number) order.get("course_id")).longValue();
        // Redis 名额回补：INCR + SREM（原子回补用 Lua；教学简化为两步）
        redis.opsForValue().increment("lc:enroll:quota:" + courseId);
        redis.opsForSet().remove("lc:enroll:users:" + courseId, String.valueOf(userId));
        return ApiResponse.ok("已取消，名额已回补");
    }
}
