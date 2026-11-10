package com.example.learn.admin;

import com.example.learn.common.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** 管理大盘：用户/课程/订单/Redis 四方统计 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final NamedParameterJdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public AdminController(NamedParameterJdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("用户数", count("t_user"));
        view.put("课程数", count("t_course"));
        view.put("订单状态分布", jdbc.getJdbcTemplate().queryForList(
                "SELECT status, COUNT(*) n FROM t_enroll_order GROUP BY status"));
        view.put("订单总数", count("t_enroll_order"));
        view.put("题库题数", count("t_question"));
        view.put("今日UV(HLL估算)", redis.opsForHyperLogLog().size(
                "lc:uv:" + java.time.LocalDate.now()));
        return ApiResponse.ok(view);
    }

    private long count(String table) {
        Long c = jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return c == null ? 0 : c;
    }
}
