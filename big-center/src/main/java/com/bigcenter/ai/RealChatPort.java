package com.bigcenter.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 真实模型实现（learn-center RealChatPort 总装版）：OpenAI 兼容端点（DeepSeek/通义/Ollama）。
 * base-url/model/api-key 全在 application.yml 配置；无 Key 时不装配（Mock 兜底）。
 */
@Component
@ConditionalOnProperty(name = "big-center.ai.provider", havingValue = "real")
public class RealChatPort implements ChatPort {

    private final RestClient client;
    private final String model;

    public RealChatPort(org.springframework.core.env.Environment env) {
        String baseUrl = env.getProperty("big-center.ai.base-url", "https://api.deepseek.com");
        this.model = env.getProperty("big-center.ai.model", "deepseek-chat");
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + env.getProperty("big-center.ai.api-key", ""))
                .build();
    }

    @Override
    public String ask(String system, String user) {
        // OpenAI 兼容 chat/completions：最小请求体（教学简化，生产补流式/重试/超时）
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> resp = client.post()
                .uri("/chat/completions")
                .header("Content-Type", "application/json")
                .body(java.util.Map.of(
                        "model", model,
                        "messages", java.util.List.of(
                                java.util.Map.of("role", "system", "content", system),
                                java.util.Map.of("role", "user", "content", user))))
                .retrieve()
                .body(java.util.Map.class);
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> choices =
                resp == null ? null : (java.util.List<java.util.Map<String, Object>>) resp.get("choices");
        if (choices == null || choices.isEmpty()) return "（模型无返回，请稍后再试）";
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> message = (java.util.Map<String, Object>) choices.get(0).get("message");
        return String.valueOf(message.get("content"));
    }
}
