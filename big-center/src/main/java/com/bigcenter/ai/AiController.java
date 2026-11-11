package com.bigcenter.ai;

import com.bigcenter.auth.UserContext;
import com.bigcenter.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** AI 学习报告接口（Mock 默认可跑通全流程；配置 big-center.ai.provider=real 切真实模型） */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiReportService reportService;

    public AiController(AiReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/report")
    public ApiResponse<Map<String, Object>> report() {
        Long userId = UserContext.get();
        String text = userId == null ? reportService.report(0L) : reportService.report(userId);
        return ApiResponse.ok(Map.of("报告", text));
    }
}
