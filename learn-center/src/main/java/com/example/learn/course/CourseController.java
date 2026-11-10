package com.example.learn.course;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.learn.common.ApiResponse;
import com.example.learn.common.BizException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 课程接口：分页列表（MP 物理分页）+ 详情缓存（互斥锁防击穿，Redis stage3/01 方案 1 落地）。
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    /** Lua：互斥锁释放（校验身份，防误删他人锁——并发篇03/redis stage3/03 同款） */
    private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0""", Long.class);

    private final com.example.learn.course.CourseMapper courseMapper;
    private final NamedParameterJdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public CourseController(com.example.learn.course.CourseMapper courseMapper,
                            NamedParameterJdbcTemplate jdbc, StringRedisTemplate redis) {
        this.courseMapper = courseMapper;
        this.jdbc = jdbc;
        this.redis = redis;
    }

    /** 课程分页列表（MP 物理分页：教程 09 的 PaginationInnerInterceptor） */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        Page<Course> result = courseMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Course>().eq(Course::getStatus, 1).orderByDesc(Course::getId));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("total", result.getTotal());
        view.put("pages", result.getPages());
        view.put("records", result.getRecords());
        return ApiResponse.ok(view);
    }

    /** 课程详情：缓存击穿防护——互斥锁重建（Redis stage3/01 方案 1 落地） */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long id) {
        String cacheKey = "lc:cache:course:" + id;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return ApiResponse.ok(Map.of("来源", "Redis 缓存", "data", cached));
        }

        // 抢重建锁：只有持锁者回源 DB，其余请求短暂等待后重读缓存
        String lockKey = "lc:lock:course:" + id;
        String requestId = UUID.randomUUID().toString();
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, requestId, Duration.ofSeconds(10));
        if (Boolean.TRUE.equals(locked)) {
            try {
                Map<String, Object> row = jdbc.getJdbcTemplate().queryForMap(
                        "SELECT id, title, teacher, price, quota, enrolled FROM t_course WHERE id = ? AND status = 1", id);
                String json = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(row);
                redis.opsForValue().set(cacheKey, json, Duration.ofHours(1));
                return ApiResponse.ok(Map.of("来源", "DB 回源（互斥锁重建）", "data", row));
            } catch (Exception e) {
                throw new BizException("课程不存在");
            } finally {
                redis.execute(UNLOCK, List.of(lockKey), requestId);
            }
        }
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        String after = redis.opsForValue().get(cacheKey);
        return ApiResponse.ok(Map.of("来源", after != null ? "等待重建后读缓存" : "仍在重建", "data", after));
    }

    /** 管理端：新增课程（造数据用） */
    @PostMapping("/admin")
    public ApiResponse<Long> add(@RequestParam String title, @RequestParam String teacher,
                                 @RequestParam(defaultValue = "9900") int price,
                                 @RequestParam(defaultValue = "30") int quota) {
        jdbc.update("""
                INSERT INTO t_course (title, teacher, price, quota, enrolled, status)
                VALUES (:t, :tc, :p, :q, 0, 1)
                """, new MapSqlParameterSource()
                .addValue("t", title).addValue("tc", teacher)
                .addValue("p", price).addValue("q", quota));
        Long id = jdbc.getJdbcTemplate().queryForObject(
                "SELECT id FROM t_course WHERE title = ?", Long.class, title);
        redis.delete("lc:cache:course:" + id);
        return ApiResponse.ok(id);
    }
}
