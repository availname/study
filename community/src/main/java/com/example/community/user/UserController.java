package com.example.community.user;

import com.example.community.common.ApiResponse;
import com.example.community.common.BizException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户模块：注册/登录。
 * 登录态 = Redis String：community:token:{token} → userId，TTL 7 天（String 的 Token 用法，stage5/01）。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final NamedParameterJdbcTemplate jdbc;
    private final org.springframework.data.redis.core.StringRedisTemplate redis;

    public UserController(NamedParameterJdbcTemplate jdbc, org.springframework.data.redis.core.StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    public record UserRow(long id, String username, int points) {}

    /** 解析 token → userId（各业务接口共用） */
    public long auth(String token) {
        if (token == null || token.isBlank()) throw new BizException("未登录");
        String uid = redis.opsForValue().get("community:token:" + token);
        if (uid == null) throw new BizException("登录已过期，请重新登录");
        return Long.parseLong(uid);
    }

    @PostMapping("/register")
    public ApiResponse<Long> register(@RequestParam String username, @RequestParam String password) {
        if (username.length() < 3 || password.length() < 4) throw new BizException("用户名≥3位，密码≥4位");
        try {
            jdbc.update("""
                    INSERT INTO t_user (username, password) VALUES (:u, MD5(:p))
                    """, new MapSqlParameterSource().addValue("u", username).addValue("p", password));
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BizException("用户名已存在");
        }
        Long id = jdbc.queryForObject("SELECT id FROM t_user WHERE username = :u",
                new MapSqlParameterSource("u", username), Long.class);
        return ApiResponse.ok(id);
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestParam String username, @RequestParam String password) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, password FROM t_user WHERE username = :u
                """, new MapSqlParameterSource().addValue("u", username));
        if (rows.isEmpty() || !rows.get(0).get("password").equals(md5(password))) {
            throw new BizException("用户名或密码错误");
        }
        long userId = ((Number) rows.get(0).get("id")).longValue();
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set("community:token:" + token, String.valueOf(userId), Duration.ofDays(7));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("token", token);
        view.put("userId", userId);
        view.put("提示", "后续接口用 ?token= 传入；redis-cli: GET community:token:" + token);
        return ApiResponse.ok(view);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> profile(@PathVariable long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, username, points FROM t_user WHERE id = :id
                """, new MapSqlParameterSource("id", id));
        if (rows.isEmpty()) throw new BizException("用户不存在");
        return ApiResponse.ok(rows.get(0));
    }

    private static String md5(String s) {
        try {
            byte[] b = java.security.MessageDigest.getInstance("MD5")
                    .digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
