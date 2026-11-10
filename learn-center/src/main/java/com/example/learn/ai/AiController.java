package com.example.learn.ai;

import com.example.learn.common.ApiResponse;
import com.example.learn.user.UserContext;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 接口：流式答疑（SSE）/ AI 出题入库 / 学习报告。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final com.example.learn.interview.QuestionMapper questionMapper;
    private final AiService aiService;

    public AiController(com.example.learn.interview.QuestionMapper questionMapper, AiService aiService) {
        this.questionMapper = questionMapper;
        this.aiService = aiService;
    }

    /** AI 答疑（流式 SSE）：基于题库题目上下文回答学生追问 */
    @GetMapping(value = "/answer/{questionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> answer(@PathVariable long questionId,
                                                @RequestParam(defaultValue = "") String followUp) {
        com.example.learn.interview.Question q = questionMapper.selectById(questionId);
        if (q == null) return Flux.just(ServerSentEvent.builder("[题目不存在]").event("error").build());
        return aiService.answerStream(questionId, followUp)
                .map(token -> ServerSentEvent.builder(token).event("message").build())
                .concatWith(Flux.just(ServerSentEvent.builder("[DONE]").event("done").build()));
    }

    /** AI 出题入库：生成 count 道 category 分类的题（去重后入库） */
    @PostMapping("/generate")
    public ApiResponse<String> generate(@RequestParam String category,
                                        @RequestParam(defaultValue = "3") int count) {
        int n = aiService.generateQuestions(category, count);
        return ApiResponse.ok("已生成并入库 " + n + " 道题");
    }

    /** 学习报告（聚合真实学习数据 → AI 个性化表述） */
    @GetMapping("/report")
    public ApiResponse<Map<String, Object>> report() {
        long userId = UserContext.get();
        String report = aiService.report(userId);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("userId", userId);
        view.put("report", report);
        return ApiResponse.ok(view);
    }
}
