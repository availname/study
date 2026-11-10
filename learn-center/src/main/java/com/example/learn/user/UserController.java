package com.example.learn.user;

import com.example.learn.common.ApiResponse;
import com.example.learn.common.BizException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户模块：注册/登录（Redis token 登录态，7 天 TTL）。
 * 拦截器统一鉴权：token → userId 放入 UserContext（ThreadLocal）。
 * 生产升级方向：JWT 无状态 token（教学 README 扩展）。
 */
@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final NamedParameterJdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public UserController(NamedParameterJdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    @PostMapping("/register")
    public ApiResponse<Long> register(@RequestParam String username, @RequestParam String password) {
        try {
            jdbc.update("INSERT INTO t_user (username, password) VALUES (:u, MD5(:p))",
                    new MapSqlParameterSource().addValue("u", username).addValue("p", password));
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BizException("用户名已存在");
        }
        Long id = jdbc.getJdbcTemplate().queryForObject(
                "SELECT id FROM t_user WHERE username = ?", Long.class, username);
        return ApiResponse.ok(id);
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestParam String username, @RequestParam String password) {
        Map<String, Object> row;
        try {
            row = jdbc.getJdbcTemplate().queryForMap("SELECT id, password FROM t_user WHERE username = ?", username);
        } catch (Exception e) {
            throw new BizException("用户名或密码错误");
        }
        if (!String.valueOf(row.get("password")).equalsIgnoreCase(md5(password))) {
            throw new BizException("用户名或密码错误");
        }
        long userId = ((Number) row.get("id")).longValue();
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set("lc:token:" + token, String.valueOf(userId), Duration.ofDays(7));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("token", token);
        view.put("userId", userId);
        return ApiResponse.ok(view);
    }

    /** 当前登录用户信息（userId 由 AuthInterceptor 从 token 解析并放入 UserContext） */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Long uid = UserContext.get();
        Map<String, Object> row = jdbc.getJdbcTemplate().queryForMap(
                "SELECT id, username, points FROM t_user WHERE id = ?", uid);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("userId", row.get("id"));
        view.put("username", row.get("username"));
        view.put("points", row.get("points"));
        return ApiResponse.ok(view);
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
