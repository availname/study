package com.example.learn.interview;

import com.example.learn.common.ApiResponse;
import com.example.learn.user.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 面试刷题模块（教学元闭环：把 java-core 面试速查做成刷题系统）。
 * 每日一题：按日期做随机种子选题（全站同一题）；错题本：同一题重复错只加次数（唯一索引幂等）。
 */
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final com.example.learn.interview.QuestionMapper questionMapper;
    private final NamedParameterJdbcTemplate jdbc;

    public InterviewController(com.example.learn.interview.QuestionMapper questionMapper,
                               NamedParameterJdbcTemplate jdbc) {
        this.questionMapper = questionMapper;
        this.jdbc = jdbc;
    }

    /** 题库列表（按分类筛选） */
    @GetMapping("/questions")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String category) {
        var qw = new LambdaQueryWrapper<Question>();
        if (category != null && !category.isBlank()) qw.eq(Question::getCategory, category);
        List<Map<String, Object>> list = new ArrayList<>();
        questionMapper.selectList(qw).forEach(q -> {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", q.id); v.put("category", q.category); v.put("title", q.title);
            v.put("difficulty", q.difficulty);
            list.add(v);
        });
        return ApiResponse.ok(list);
    }

    /** 查看题目与答题要点 */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) throw new com.example.learn.common.BizException("题目不存在");
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", q.id); view.put("category", q.category);
        view.put("title", q.title); view.put("answerMd", q.answerMd);
        view.put("difficulty", q.difficulty);
        return ApiResponse.ok(view);
    }

    /** 每日一题：按日期随机（全站同一题——日期 作为随机种子） */
    @GetMapping("/daily")
    public ApiResponse<Map<String, Object>> daily() {
        List<Question> all = questionMapper.selectList(null);
        if (all.isEmpty()) throw new com.example.learn.common.BizException("题库为空");
        long seed = LocalDate.now().toEpochDay();
        Question q = all.get((int) (seed % all.size()));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", q.id); view.put("title", q.title);
        view.put("answerMd", q.answerMd); view.put("category", q.category);
        return ApiResponse.ok(view);
    }

    /** 标记错题（刷题答错时调用）：唯一索引(user_id,question_id) 幂等，重复错只加次数 */
    @PostMapping("/{id}/wrong")
    public ApiResponse<String> markWrong(@PathVariable long id) {
        long userId = UserContext.get();
        jdbc.update("""
                INSERT INTO t_wrong_book (user_id, question_id, wrong_count)
                VALUES (:uid, :qid, 1)
                ON DUPLICATE KEY UPDATE wrong_count = wrong_count + 1, last_wrong_at = NOW(3)
                """, new MapSqlParameterSource()
                .addValue("uid", userId).addValue("qid", id));
        return ApiResponse.ok("已加入错题本");
    }

    /** 我的错题本 */
    @GetMapping("/wrongbook")
    public ApiResponse<List<Map<String, Object>>> wrongbook() {
        long userId = UserContext.get();
        return ApiResponse.ok(jdbc.queryForList("""
                SELECT q.id, q.title, q.category, w.wrong_count, w.last_wrong_at
                FROM t_wrong_book w JOIN t_question q ON q.id = w.question_id
                WHERE w.user_id = :uid ORDER BY w.wrong_count DESC, w.last_wrong_at DESC
                """, new MapSqlParameterSource("uid", userId)));
    }
}
