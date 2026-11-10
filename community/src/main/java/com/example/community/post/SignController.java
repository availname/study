package com.example.community.post;

import com.example.community.common.ApiResponse;
import com.example.community.common.BizException;
import com.example.community.user.UserController;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 每日签到（Bitmap，stage5/06 实战版）：
 * SETBIT 幂等签到；读侧用 GET 原始位串 + 应用层解位（避免 ValueOperations 无 bitCount/bitField
 * 的 API 版本差异坑，教程踩坑实录有记录）。积分经 t_points_log 唯一键幂等发放。
 */
@RestController
@RequestMapping("/api/sign")
public class SignController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SignController.class);
    private static final int SIGN_POINTS = 1;

    private final StringRedisTemplate redis;
    private final UserController userController;
    private final NamedParameterJdbcTemplate jdbc;

    public SignController(StringRedisTemplate redis, UserController userController,
                          NamedParameterJdbcTemplate jdbc) {
        this.redis = redis;
        this.userController = userController;
        this.jdbc = jdbc;
    }

    /** 今日签到：SETBIT 返回旧值 → 天然幂等（重复签到不发重复积分） */
    @PostMapping
    public ApiResponse<Map<String, Object>> sign(@RequestParam String token) {
        long userId = userController.auth(token);
        LocalDate today = LocalDate.now();
        String key = signKey(userId, today);

        Boolean first = redis.opsForValue().setBit(key, today.getDayOfMonth() - 1, true);
        Map<String, Object> view = new LinkedHashMap<>();
        // ⚠ SETBIT 返回的是"设置前的旧值"：0=之前没签（本次首次），1=已签过 —— 判断千万别写反
        if (!Boolean.TRUE.equals(first)) {
            String messageId = "sign-" + userId + "-" + today;
            try {
                jdbc.update("""
                        INSERT INTO t_points_log (user_id, points, reason, message_id)
                        VALUES (:uid, :p, '每日签到', :mid)
                        """, new MapSqlParameterSource()
                        .addValue("uid", userId).addValue("p", SIGN_POINTS).addValue("mid", messageId));
                jdbc.update("UPDATE t_user SET points = points + :p WHERE id = :uid",
                        new MapSqlParameterSource().addValue("uid", userId).addValue("p", SIGN_POINTS));
                redis.opsForZSet().incrementScore("community:rank:points",
                        String.valueOf(userId), SIGN_POINTS);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                log.info("[积分] 幂等拦截 uid={} date={}", userId, today);
            }
            view.put("结果", "签到成功，+" + SIGN_POINTS + "积分");
        } else {
            view.put("结果", "今天已签过（BIT 幂等拦截）");
        }
        int[] bits = bitsOf(key, today.lengthOfMonth());
        int days = 0;
        for (int b : bits) days += b;
        view.put("本月签到天数", days);
        view.put("连续签到天数", continuousDays(bits, today.getDayOfMonth()));
        return ApiResponse.ok(view);
    }

    /** 月历视图：每天签没签（1/0 数组，前端渲染日历打勾） */
    @GetMapping("/calendar")
    public ApiResponse<Map<String, Object>> calendar(@RequestParam String token) {
        long userId = userController.auth(token);
        LocalDate today = LocalDate.now();
        int[] bits = bitsOf(signKey(userId, today), today.lengthOfMonth());
        int days = 0;
        for (int b : bits) days += b;

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("月", today.getYear() + "-" + today.getMonthValue());
        view.put("签到位(1号开始)", bits);
        view.put("合计", days);
        return ApiResponse.ok(view);
    }

    // ============ 位串解析：GET 原始串 → 按字节解位（Redis SETBIT 的 offset i = 第 i/8 字节的第 7-i%8 位）============

    private int[] bitsOf(String key, int len) {
        String value = redis.opsForValue().get(key);   // 二进制安全：GET 原样字节
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.ISO_8859_1);
        int[] bits = new int[len];
        for (int i = 0; i < len; i++) {
            int byteIdx = i / 8;
            if (byteIdx < bytes.length) {
                bits[i] = (bytes[byteIdx] >> (7 - (i % 8))) & 1;
            }
        }
        return bits;
    }

    /** 连续签到：从今天往回数连续的 1，断签即停（跨月拼上月末位，教学简化为本月内） */
    private int continuousDays(int[] bits, int todayOffsetFrom1) {
        int continuous = 0;
        for (int i = todayOffsetFrom1 - 1; i >= 0; i--) {
            if (bits[i] == 1) continuous++;
            else break;
        }
        return continuous;
    }

    private String signKey(long userId, LocalDate date) {
        return "community:sign:" + userId + ":" + date.getYear() + String.format("%02d", date.getMonthValue());
    }
}
