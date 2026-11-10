package com.example.community.post;

import com.example.community.common.ApiResponse;
import com.example.community.common.BizException;
import com.example.community.messaging.SendRecordDao;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理端：人工审核卡点 + 全局大盘（Redis/DB/MQ 三方状态一屏可见） */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PostDao postDao;
    private final AuditConsumer auditConsumer;
    private final NamedParameterJdbcTemplate jdbc;
    private final SendRecordDao sendRecordDao;
    private final org.springframework.data.redis.core.StringRedisTemplate redis;

    public AdminController(PostDao postDao, AuditConsumer auditConsumer,
                           NamedParameterJdbcTemplate jdbc, SendRecordDao sendRecordDao,
                           org.springframework.data.redis.core.StringRedisTemplate redis) {
        this.postDao = postDao;
        this.auditConsumer = auditConsumer;
        this.jdbc = jdbc;
        this.sendRecordDao = sendRecordDao;
        this.redis = redis;
    }

    /** 待人工审核列表（HUMAN 关键词的帖子停在这里） */
    @GetMapping("/pending")
    public ApiResponse<List<Map<String, Object>>> pending() {
        return ApiResponse.ok(postDao.listByStatus(PostDao.HUMAN_AUDIT, 50));
    }

    /** 人工审核：pass=true 走完整发布链路（积分+fanout），false 拒绝 */
    @PostMapping("/audit/{postId}")
    public ApiResponse<Void> audit(@PathVariable long postId,
                                   @RequestParam(defaultValue = "true") boolean pass) {
        auditConsumer.humanAudit(postId, pass);
        return ApiResponse.ok(null);
    }

    /** 全局大盘：订单外的三方状态对照（Redis热账 / DB底账 / MQ管道） */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("帖子状态分布", jdbc.queryForList(
                "SELECT status, COUNT(*) n FROM t_post GROUP BY status", new MapSqlParameterSource()));
        view.put("待人工审核", jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_post WHERE status = 3", new MapSqlParameterSource(), Long.class));
        view.put("积分流水数", jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_points_log", new MapSqlParameterSource(), Long.class));
        view.put("发送记录(本地消息表)", sendRecordDao.countByStatus());
        Long hot = redis.opsForZSet().zCard("community:post:hot");
        view.put("热榜帖子数(Redis)", hot == null ? 0 : hot);
        String uvToday = redis.opsForHyperLogLog().size(
                "community:uv:home:" + java.time.LocalDate.now()).toString();
        view.put("今日UV(HLL估算)", uvToday);
        return ApiResponse.ok(view);
    }

    /** 死信存档查看 */
    @GetMapping("/dead-letters")
    public ApiResponse<List<Map<String, Object>>> deadLetters() {
        return ApiResponse.ok(jdbc.queryForList("""
                SELECT id, message_id, source_queue, reason, payload, created_at
                FROM t_mq_dead_letter ORDER BY id DESC LIMIT 50
                """, new MapSqlParameterSource()));
    }
}
