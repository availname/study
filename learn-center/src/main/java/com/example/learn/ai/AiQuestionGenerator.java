package com.example.learn.ai;

import com.example.learn.common.BizException;
import com.example.learn.interview.Question;
import com.example.learn.interview.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 出题服务：LLM 结构化输出生成面试题 → 去重入库题库（元闭环：用它刷它生成它教的题）。
 * 解析失败兜底：剥 ```json 围栏 → 仍失败抛业务异常（提示词迭代是运维动作）。
 */
@Service
public class AiQuestionGenerator {

    private static final Logger log = LoggerFactory.getLogger(AiQuestionGenerator.class);

    private final ChatPort chatPort;
    private final ObjectMapper om;
    private final QuestionMapper questionMapper;

    public AiQuestionGenerator(ChatPort chatPort, ObjectMapper om, QuestionMapper questionMapper) {
        this.chatPort = chatPort;
        this.om = om;
        this.questionMapper = questionMapper;
    }

    /** 生成 count 道 category 分类的面试题并入库，返回入库数 */
    public int generate(String category, int count) {
        String system = """
                你是 Java 后端资深面试官。生成 %d 道「%s」分类的面试题。
                只输出 JSON 数组，不要多余文字，元素结构：
                {"category":"%s","title":"题目","answerMd":"答题要点 markdown","difficulty":1到3}
                """.formatted(count, category, category);

        String raw = chatPort.ask(system, "开始生成");
        String json = stripFence(raw);

        int inserted = 0;
        try {
            com.fasterxml.jackson.databind.JsonNode arr = om.readTree(json);
            if (!arr.isArray()) throw new IllegalStateException("非数组");
            for (JsonNode n : arr) {
                String title = n.path("title").asText(null);
                if (title == null || title.isBlank()) continue;
                // 去重：同分类同标题不重复入库
                Long exist = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                        .eq(Question::getTitle, title).eq(Question::getCategory, category));
                if (exist != null && exist > 0) continue;
                Question q = new Question();
                q.category = category;
                q.title = title;
                q.answerMd = n.path("answerMd").asText("");
                int diff = n.path("difficulty").asInt(2);
                q.difficulty = Math.min(3, Math.max(1, diff));
                questionMapper.insert(q);
                inserted++;
            }
        } catch (Exception e) {
            log.error("[AI出题] 解析/入库失败 raw={}", json, e);
            throw new com.example.learn.common.BizException("AI 出题解析失败，请重试");
        }
        log.info("[AI出题] category={} 生成 {} 道题入库", category, inserted);
        return inserted;
    }

    private static String stripFence(String s) {
        s = s.trim();
        if (s.startsWith("```")) {
            int start = s.indexOf('\n');
            if (start > 0) s = s.substring(start + 1);
            int end = s.lastIndexOf("```");
            if (end > 0) s = s.substring(0, end);
        }
        return s.trim();
    }
}
