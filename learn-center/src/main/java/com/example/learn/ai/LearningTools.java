package com.example.learn.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.learn.interview.Question;
import com.example.learn.interview.QuestionMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习助手的工具集（stage7/06 Agent 落地）。
 * ⚠ 安全底线：工具只读、无删除/支付类操作；用户身份由服务端注入（UserContext），
 *   工具 description 是 LLM 选择工具的依据——写得越清楚，Agent 决策越准。
 */
public class LearningTools {

    private final QuestionMapper questionMapper;
    private final NamedParameterJdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final long userId;

    public LearningTools(QuestionMapper questionMapper, NamedParameterJdbcTemplate jdbc,
                         StringRedisTemplate redis, long userId) {
        this.questionMapper = questionMapper;
        this.jdbc = jdbc;
        this.redis = redis;
        this.userId = userId;
    }

    /** 工具①：按关键词搜索题库 */
    public List<Map<String, Object>> searchQuestions(String keyword) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Question q : questionMapper.selectList(
                new LambdaQueryWrapper<Question>().like(Question::getTitle, keyword).last("LIMIT 10"))) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", q.getId());
            v.put("title", q.getTitle());
            v.put("category", q.getCategory());
            v.put("difficulty", q.getDifficulty());
            result.add(v);
        }
        return result;
    }

    /** 工具②：当前用户的学习统计（身份服务端注入，防越权查他人数据） */
    public Map<String, Object> studyStats() {
        Long days = redis.execute((org.springframework.data.redis.core.RedisCallback<Long>) con ->
                con.bitCount(("lc:sign:" + userId + ":"
                        + java.time.LocalDate.now().getYear()
                        + String.format("%02d", java.time.LocalDate.now().getMonthValue()))
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String minutes = redis.opsForValue().get("lc:study:minutes:" + userId);
        Long wrong = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM t_wrong_book WHERE user_id = ?", Long.class, userId);
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("本月签到天数", days == null ? 0 : days);
        v.put("累计学习分钟", minutes == null ? 0 : Long.parseLong(minutes));
        v.put("错题数", wrong == null ? 0 : wrong);
        return v;
    }

    /** 工具③：课程名额查询 */
    public Map<String, Object> courseQuota(long courseId) {
        Map<String, Object> row = jdbc.getJdbcTemplate().queryForMap(
                "SELECT title, quota, enrolled FROM t_course WHERE id = ?", courseId);
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("课程", row.get("title"));
        v.put("剩余名额", ((Number) row.get("quota")).intValue()
                - ((Number) row.get("enrolled")).intValue());
        return v;
    }
}
