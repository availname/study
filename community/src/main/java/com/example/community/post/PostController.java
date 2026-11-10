package com.example.community.post;

import com.example.community.common.ApiResponse;
import com.example.community.common.BizException;
import com.example.community.messaging.ReliableMessagePublisher;
import com.example.community.user.UserController;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

/**
 * 帖子模块：发帖（限流+防重+事务消息）/ 详情（阅读计数+UV+热度）/ 点赞 / 热榜。
 * Redis 侧承担全部高频写（计数、热度、点赞去重、UV），MQ 承担审核管道与发布广播，DB 存真源。
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    /** Lua：INCR + 首次设 TTL 原子执行（stage5/01 防刷计数，避免 INCR/EXPIRE 两步竞态残留） */
    private static final DefaultRedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>("""
            local c = redis.call('INCR', KEYS[1])
            if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return c
            """, Long.class);

    private final PostDao postDao;
    private final ReliableMessagePublisher publisher;
    private final StringRedisTemplate redis;
    private final NamedParameterJdbcTemplate jdbc;
    private final UserController userController;

    public PostController(PostDao postDao, ReliableMessagePublisher publisher,
                          StringRedisTemplate redis, NamedParameterJdbcTemplate jdbc,
                          UserController userController) {
        this.postDao = postDao;
        this.publisher = publisher;
        this.redis = redis;
        this.jdbc = jdbc;
        this.userController = userController;
    }

    // ============ 发帖：防刷 → 防重 → 落库 → 事务消息进审核管道 ============

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> submit(@RequestParam String token,
                                      @RequestParam String title,
                                      @RequestParam(defaultValue = "") String content) {
        long userId = userController.auth(token);

        // ① 每日发帖限 5 篇（Lua：INCR+首次TTL 原子，防"残留计数器"）
        String limitKey = "community:post:limit:" + userId + ":" + LocalDate.now();
        Long count = redis.execute(INCR_WITH_TTL, List.of(limitKey), "86400");
        if (count != null && count > 5) {
            throw new BizException("今日发帖已达上限（5篇）");
        }

        // ② 内容去重（MD5 指纹，SET NX EX——防灌水重复帖）
        String md5 = md5(title + "|" + content);
        Boolean fresh = redis.opsForValue()
                .setIfAbsent("community:post:hash:" + md5, "1", Duration.ofHours(24));
        if (Boolean.FALSE.equals(fresh)) {
            throw new BizException("请勿重复发布相同内容");
        }

        // ③ 落库（待审核）+ ④ 本地消息表 + 提交后发审核消息（同一个事务，stage3/01）
        long postId = postDao.insert(userId, title, content);
        publisher.sendInTransaction("community.post", "post.submitted",
                "post-submitted-" + postId,
                Map.of("postId", postId, "title", title, "authorId", userId));
        return ApiResponse.ok("已提交，等待审核。postId=" + postId);
    }

    // ============ 详情：DB 真源 + Redis 高频写三连（阅读计数/热度/UV） ============

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long id) {
        PostDao.PostRow post = postDao.find(id).orElseThrow(() -> new BizException("帖子不存在"));
        if (post.status() != PostDao.APPROVED) {
            throw new BizException("帖子审核中或不可见，状态=" + post.status());
        }
        // Redis 高频写三连：都不直接碰 DB（削峰，CounterFlushJob 异步刷底账）
        redis.opsForValue().increment("community:post:view:" + id);            // 阅读计数（String INCR）
        redis.opsForZSet().incrementScore("community:post:hot",                // 热榜热度
                String.valueOf(id), 1);
        String today = LocalDate.now().toString();
        redis.opsForHyperLogLog().add("community:uv:home:" + today, id + ":" + UUID.randomUUID()); // UV（教学模拟访客）

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", post.id());
        view.put("title", post.title());
        view.put("content", post.content());
        view.put("authorId", post.authorId());
        view.put("实时阅读(Redis)", redis.opsForValue().get("community:post:view:" + id));
        view.put("DB阅读(底账)", post.viewCount());
        view.put("实时点赞(Set SCARD)", redis.opsForSet().size("community:post:likes:" + id));
        return ApiResponse.ok(view);
    }

    // ============ 点赞：Set 去重 + 热榜加分（重复点赞被 SADD 返回值拦截） ============

    @PostMapping("/{id}/like")
    public ApiResponse<String> like(@RequestParam String token, @PathVariable long id) {
        long userId = userController.auth(token);
        postDao.find(id).orElseThrow(() -> new BizException("帖子不存在"));
        Long added = redis.opsForSet().add("community:post:likes:" + id, String.valueOf(userId));
        if (added == null || added == 0) {
            return ApiResponse.error("已经赞过了（Set 去重拦截）");
        }
        redis.opsForZSet().incrementScore("community:post:hot", String.valueOf(id), 3);   // 点赞热度+3
        return ApiResponse.ok("点赞成功（redis-cli: SCARD community:post:likes:" + id + "）");
    }

    /** 取消点赞：SREM + 热度回退 */
    @DeleteMapping("/{id}/like")
    public ApiResponse<String> unlike(@RequestParam String token, @PathVariable long id) {
        long userId = userController.auth(token);
        Long removed = redis.opsForSet().remove("community:post:likes:" + id, String.valueOf(userId));
        if (removed != null && removed > 0) {
            redis.opsForZSet().incrementScore("community:post:hot", String.valueOf(id), -3);
            return ApiResponse.ok("已取消点赞");
        }
        return ApiResponse.error("你还没赞过");
    }

    // ============ 热榜：ZSet 热度算法（阅读+1 点赞+3 发帖初始+5），每小时衰减见 HeatDecayJob ============

    @GetMapping("/hot")
    public ApiResponse<Map<String, Object>> hot() {
        Set<ZSetOperations.TypedTuple<String>> top =
                redis.opsForZSet().reverseRangeWithScores("community:post:hot", 0, 9);
        List<Long> ids = top == null ? List.of() :
                top.stream().map(t -> Long.parseLong(Objects.requireNonNull(t.getValue()))).toList();
        Map<Long, Map<String, Object>> posts = new HashMap<>();
        postDao.listByIds(ids).forEach(row -> posts.put(((Number) row.get("id")).longValue(), row));

        List<Map<String, Object>> list = new ArrayList<>();
        if (top != null) {
            for (ZSetOperations.TypedTuple<String> t : top) {
                Map<String, Object> item = new LinkedHashMap<>();
                long id = Long.parseLong(Objects.requireNonNull(t.getValue()));
                item.put("postId", id);
                item.put("热度", Objects.requireNonNull(t.getScore()).doubleValue());
                Map<String, Object> p = posts.get(id);
                item.put("标题", p == null ? "(已删除)" : p.get("title"));
                list.add(item);
            }
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("榜单", list);
        view.put("说明", "阅读+1 / 点赞+3 / 发帖初始+5；HeatDecayJob 每小时×0.98 衰减");
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
