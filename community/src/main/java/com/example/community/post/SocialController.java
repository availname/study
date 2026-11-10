package com.example.community.post;

import com.example.community.common.ApiResponse;
import com.example.community.common.BizException;
import com.example.community.user.UserController;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 社交模块：关注/取关/Feed 时间线/通知收件箱。
 * 关注关系：Redis Set 双写（我关注的人 + 他的粉丝，发布时读粉丝 Set 推 Feed）
 *          + DB 落关系真源（教学简化：仅 Redis，README 注明生产需 DB 主 + 对账）。
 */
@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final StringRedisTemplate redis;
    private final UserController userController;
    private final PostDao postDao;
    private final NamedParameterJdbcTemplate jdbc;

    public SocialController(StringRedisTemplate redis, UserController userController,
                            PostDao postDao, NamedParameterJdbcTemplate jdbc) {
        this.redis = redis;
        this.userController = userController;
        this.postDao = postDao;
        this.jdbc = jdbc;
    }

    /** 关注 / 取关 */
    @PostMapping("/follow/{targetId}")
    public ApiResponse<String> follow(@RequestParam String token, @PathVariable long targetId) {
        long me = userController.auth(token);
        if (me == targetId) throw new BizException("不能关注自己");
        String action = redis.opsForSet().isMember("community:follow:" + me, String.valueOf(targetId))
                ? "off" : "on";
        if ("on".equals(action)) {
            redis.opsForSet().add("community:follow:" + me, String.valueOf(targetId));
            redis.opsForSet().add("community:fans:" + targetId, String.valueOf(me));
        } else {
            redis.opsForSet().remove("community:follow:" + me, String.valueOf(targetId));
            redis.opsForSet().remove("community:fans:" + targetId, String.valueOf(me));
        }
        return ApiResponse.ok("已" + ("on".equals(action) ? "关注" : "取关") + "用户 " + targetId);
    }

    /** 我关注了谁 */
    @GetMapping("/following")
    public ApiResponse<Set<String>> following(@RequestParam String token) {
        long me = userController.auth(token);
        return ApiResponse.ok(redis.opsForSet().members("community:follow:" + me));
    }

    /** 我的 Feed 时间线：读收件箱（推模式的成果，O(1) 直读） + 批量补全帖子信息 */
    @GetMapping("/feed")
    public ApiResponse<List<Map<String, Object>>> feed(@RequestParam String token) {
        long me = userController.auth(token);
        List<String> ids = redis.opsForList().range("community:inbox:" + me, 0, 19);
        List<Map<String, Object>> result = new ArrayList<>();
        if (ids != null) {
            for (String idStr : ids) {
                postDao.find(Long.parseLong(idStr)).ifPresent(p -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("postId", p.id());
                    item.put("title", p.title());
                    item.put("authorId", p.authorId());
                    result.add(item);
                });
            }
        }
        return ApiResponse.ok(result);
    }

    /** 通知收件箱（审核结果、系统通知） */
    @GetMapping("/notifications")
    public ApiResponse<List<String>> notifications(@RequestParam String token) {
        long me = userController.auth(token);
        List<String> list = redis.opsForList().range("community:notif:" + me, 0, 19);
        return ApiResponse.ok(list == null ? List.of() : list);
    }

    /** 积分榜（ZSet）：签到/发帖奖励的积分都会进来 */
    @GetMapping("/rank/points")
    public ApiResponse<Object> pointsRank() {
        return ApiResponse.ok(redis.opsForZSet()
                .reverseRangeWithScores("community:rank:points", 0, 9));
    }

    /** 粉丝数 */
    @GetMapping("/fans/{userId}")
    public ApiResponse<Long> fans(@PathVariable long userId) {
        Long size = redis.opsForSet().size("community:fans:" + userId);
        return ApiResponse.ok(size == null ? 0 : size);
    }
}
