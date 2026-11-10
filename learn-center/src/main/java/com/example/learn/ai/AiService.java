package com.example.learn.ai;

import com.example.learn.common.BizException;
import com.example.learn.interview.Question;
import com.example.learn.interview.QuestionMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

/**
 * AI 服务门面：答疑（RAG 简化版：题目上下文注入）/ 出题入库 / 学习报告。
 * 设计原则（stage7/04）：事实由系统提供，措辞由 AI 生成，失败有降级路径。
 */
@Service
public class AiService {

    private final ChatPort chatPort;
    private final QuestionMapper questionMapper;
    private final AiQuestionGenerator generator;
    private final NamedParameterJdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public AiService(ChatPort chatPort, QuestionMapper questionMapper,
                     AiQuestionGenerator generator, NamedParameterJdbcTemplate jdbc,
                     StringRedisTemplate redis) {
        this.chatPort = chatPort;
        this.questionMapper = questionMapper;
        this.generator = generator;
        this.jdbc = jdbc;
        this.redis = redis;
    }

    /** AI 答疑（流式）：基于题目上下文回答学生追问（RAG 简化版：题目+要点作为参考资料） */
    public Flux<String> answerStream(long questionId, String followUp) {
        Question q = questionMapper.selectById(questionId);
        if (q == null) return Flux.just("题目不存在");

        String system = """
                你是 Java 后端学习助教。基于【题目】和【参考要点】回答学生追问；
                要点之外的扩展可以讲，但不要编造不存在的 API 或结论。回答用中文。
                """;
        String user = """
                【题目】%s
                【参考要点】%s
                【学生追问】%s
                """.formatted(q.title, q.answerMd, followUp);
        return chatPort.stream(system, user);
    }

    /** AI 出题：结构化生成 → 去重入库，返回入库数 */
    public int generateQuestions(String category, int count) {
        return generator.generate(category, count);
    }

    /** 学习报告：聚合真实学习数据（数字由系统提供，AI 只做表述——防幻觉的黄金分工） */
    public String report(long userId) {
        LocalDate now = LocalDate.now();
        String signKey = "lc:sign:" + userId + ":" + now.getYear() + String.format("%02d", now.getMonthValue());

        long signDays = redisBitCount(signKey);
        long studyMinutes = studyMinutes(userId);
        long wrongCount = wrongCount(userId);
        long enrollCount = enrollCount(userId);

        String prompt = """
                根据以下学习数据写一段 150 字左右的个性化学习建议（中文，友善鼓励语气，给出具体可执行建议）：
                - 本月签到 %d 天
                - 累计学习 %d 分钟
                - 错题 %d 道
                - 已抢课程 %d 门
                """.formatted(signDays, studyMinutes, wrongCount, enrollCount);
        return chatPort.ask("你是 Java 后端学习顾问，根据数据给出具体、可执行的建议", prompt);
    }

    // ---------- 数据聚合（真实查询） ----------

    private long signDays(long userId, String signKey, int dayOfMonth) {
        Long c = redisBitCount(signKey);
        return c == null ? 0 : c;
    }

    private long studyMinutes(long userId) {
        String v = redis.opsForValue().get("lc:study:minutes:" + userId);
        return v == null ? 0 : Long.parseLong(v);
    }

    private long wrongCount(long userId) {
        Long c = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM t_wrong_book WHERE user_id = ?", Long.class, userId);
        return c == null ? 0 : c;
    }

    private long enrollCount(long userId) {
        Long c = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM t_enroll_order WHERE user_id = ? AND status IN (0, 1)",
                Long.class, userId);
        return c == null ? 0 : c;
    }

    private Long redisBitCount(String key) {
        return redis.execute((org.springframework.data.redis.core.RedisCallback<Long>) con ->
                con.bitCount(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
