package com.example.learn.ai;

import com.example.learn.common.ApiResponse;
import com.example.learn.user.UserContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 端点（stage7/06）：LLM + 工具的自主循环——模型自己决定调用哪个工具。
 * 工具（只读最小权限）：搜索题库 / 学习统计 / 课程名额。
 * ⚠ 需要 lc.ai.provider=real（Mock 不执行工具循环）。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final ChatClient.Builder chatClientBuilder;
    private final NamedParameterJdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public AgentController(ChatClient.Builder chatClientBuilder,
                           NamedParameterJdbcTemplate jdbc,
                           StringRedisTemplate redis) {
        this.chatClientBuilder = chatClientBuilder;
        this.jdbc = jdbc;
        this.redis = redis;
    }

    /** Agent 对话：LLM 自主调用工具（搜索题库/学习统计/课程名额）后回答 */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> agentChat(
            @RequestParam String q) {
        long userId = UserContext.get();

        ChatClient client = chatClientBuilder
                .defaultSystem("""
                        你是 learn-center 学习平台的助手。你可以调用工具查询：面试题库、我的学习统计、我的抢课订单、课程名额。
                        回答用中文；涉及数据时以工具返回为准，不要编造数字。
                        """)
                .build();

        LearningToolsAgent tools = new LearningToolsAgent(jdbc, redis, userId);

        return client.prompt()
                .user(q)
                .tools(tools)
                .stream()
                .content()
                .map(token -> ServerSentEvent.builder(token).event("message").build())
                .concatWith(Flux.just(ServerSentEvent.builder("[DONE]").event("done").build()));
    }

    /** Agent 工具集（stage7/06 三底线：只读、白名单、调用上限由框架 retry 限制） */
    public static class LearningToolsAgent {
        private final NamedParameterJdbcTemplate jdbc;
        private final org.springframework.data.redis.core.StringRedisTemplate redis;
        private final long userId;

        public LearningToolsAgent(NamedParameterJdbcTemplate jdbc,
                                  org.springframework.data.redis.core.StringRedisTemplate redis,
                                  long userId) {
            this.jdbc = jdbc;
            this.redis = redis;
            this.userId = userId;
        }

        @org.springframework.ai.tool.annotation.Tool(description = "按关键词搜索面试题库，返回题目列表")
        public List<Map<String, Object>> searchQuestions(
                @org.springframework.ai.tool.annotation.ToolParam(description = "搜索关键词") String keyword) {
            return jdbc.queryForList("""
                    SELECT id, category, title, difficulty FROM t_question
                    WHERE title LIKE CONCAT('%', :kw, '%') LIMIT 10
                    """, new MapSqlParameterSource().addValue("kw", keyword));
        }

        @org.springframework.ai.tool.annotation.Tool(description = "查询我的学习统计：本月签到天数、累计学习分钟、错题数、抢课数")
        public Map<String, Object> studyStats() {
            Map<String, Object> v = new LinkedHashMap<>();
            String minutes = redis.opsForValue().get("lc:study:minutes:" + userId);
            v.put("累计学习分钟", minutes == null ? 0 : Long.parseLong(minutes));
            Long wrong = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM t_wrong_book WHERE user_id = :uid",
                    new MapSqlParameterSource().addValue("uid", userId), Long.class);
            v.put("错题数", wrong == null ? 0 : wrong);
            Long enrolled = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM t_enroll_order WHERE user_id = :uid AND status IN (0, 1)",
                    new MapSqlParameterSource().addValue("uid", userId), Long.class);
            v.put("抢课数", enrolled == null ? 0 : enrolled);
            return v;
        }

        @org.springframework.ai.tool.annotation.Tool(description = "查询某课程剩余名额")
        public Map<String, Object> courseQuota(
                @org.springframework.ai.tool.annotation.ToolParam(description = "课程ID") long courseId) {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT title, quota, enrolled FROM t_course WHERE id = :cid",
                    new MapSqlParameterSource().addValue("cid", courseId));
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("课程", row.get("title"));
            v.put("剩余名额", ((Number) row.get("quota")).intValue()
                    - ((Number) row.get("enrolled")).intValue());
            return v;
        }
    }
}
