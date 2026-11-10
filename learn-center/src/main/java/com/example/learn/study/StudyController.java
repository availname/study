package com.example.learn.study;

import com.example.learn.common.ApiResponse;
import com.example.learn.user.UserContext;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 学习打卡（Bitmap，redis stage5/06 实战）+ 学习时长榜（ZSet）+ UV（HLL）。
 * SETBIT 旧值语义正确版：首次 false=首签发积分，重复 true=幂等拦截。
 */
@RestController
@RequestMapping("/api/study")
public class StudyController {

    private final StringRedisTemplate redis;
    private final NamedParameterJdbcTemplate jdbc;

    public StudyController(StringRedisTemplate redis, NamedParameterJdbcTemplate jdbc) {
        this.redis = redis;
        this.jdbc = jdbc;
    }

    private String signKey(long userId, LocalDate d) {
        return "lc:sign:" + userId + ":" + d.getYear() + String.format("%02d", d.getMonthValue());
    }

    /** 每日签到：SETBIT 旧值判断幂等，首签 +1 积分 */
    @PostMapping("/sign")
    public ApiResponse<Map<String, Object>> sign() {
        long userId = UserContext.get();
        LocalDate today = LocalDate.now();
        String key = signKey(userId, today);
        int offset = today.getDayOfMonth() - 1;

        Boolean firstTime = redis.opsForValue().setBit(key, offset, true);
        Map<String, Object> view = new LinkedHashMap<>();
        if (Boolean.FALSE.equals(firstTime)) {
            // 首签：发积分（UPDATE user points + INSERT 流水省略，教学简化）
            redis.opsForZSet().incrementScore("lc:rank:study", String.valueOf(userId), 1);
            jdbc.getJdbcTemplate().update("UPDATE t_user SET points = points + 1 WHERE id = ?", userId);
            view.put("结果", "签到成功，+1 积分");
        } else {
            view.put("结果", "今天已签过（BIT 幂等拦截——SETBIT 旧值语义正确版）");
        }
        int[] bits = bitsOf(signKey(userId, today), today.getDayOfMonth());
        int continuous = 0;
        for (int i = today.getDayOfMonth() - 1; i >= 0; i--) {
            if (bits[i] == 1) continuous++; else break;
        }
        int total = 0;
        for (int b : bits) total += b;
        view.put("本月签到", total);
        view.put("连续签到", continuous);
        return ApiResponse.ok(view);
    }

    /** 月历视图：每天签没签（1/0 数组，前端渲染日历） */
    @GetMapping("/calendar")
    public ApiResponse<Map<String, Object>> calendar() {
        long userId = UserContext.get();
        LocalDate today = LocalDate.now();
        int[] bits = bitsOf(signKey(userId, today), today.lengthOfMonth());
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("月", today.getYear() + "-" + today.getMonthValue());
        view.put("签到位", bits);
        view.put("合计", Arrays.stream(bits).sum());
        return ApiResponse.ok(view);
    }

    /** 学习时长上报：INCR 计数 + ZSet 榜单加分 */
    @PostMapping("/learn")
    public ApiResponse<String> learn(@RequestParam int minutes) {
        long userId = UserContext.get();
        redis.opsForValue().increment("lc:study:minutes:" + userId, minutes);
        redis.opsForZSet().incrementScore("lc:rank:study", String.valueOf(userId), minutes);
        return ApiResponse.ok("学习 " + minutes + " 分钟已记录");
    }

    /** 学习时长排行榜（ZSet 降序前 10） */
    @GetMapping("/rank")
    public ApiResponse<List<Map<String, Object>>> rank() {
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> top =
                redis.opsForZSet().reverseRangeWithScores("lc:rank:study", 0, 9);
        List<Map<String, Object>> list = new ArrayList<>();
        if (top != null) {
            int rank = 1;
            for (var t : top) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("名次", rank++);
                item.put("userId", t.getValue());
                item.put("学习时长(分)", t.getScore());
                list.add(item);
            }
        }
        return ApiResponse.ok(list);
    }

    /** 今日 UV（HLL 估算——每个登录用户算一个独立访客） */
    @PostMapping("/uv")
    public ApiResponse<Long> uvToday() {
        long userId = UserContext.get();
        redis.opsForHyperLogLog().add("lc:uv:" + LocalDate.now(), String.valueOf(userId));
        return ApiResponse.ok(redis.opsForHyperLogLog().size("lc:uv:" + LocalDate.now()));
    }

    /** 位串解析：GET 原始串 → 按字节解位（SETBIT 的 offset i = 第 i/8 字节的第 7-i%8 位） */
    private int[] bitsOf(String key, int len) {
        String value = redis.opsForValue().get(key);
        byte[] bytes = value == null ? new byte[0] : value.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        int[] bits = new int[len];
        for (int i = 0; i < len; i++) {
            int byteIdx = i / 8;
            if (byteIdx < bytes.length) {
                bits[i] = (bytes[byteIdx] >> (7 - (i % 8))) & 1;
            }
        }
        return bits;
    }
}
