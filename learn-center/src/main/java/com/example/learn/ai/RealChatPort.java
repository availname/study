package com.example.learn.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 真实模型实现：Spring AI ChatClient 走 OpenAI 兼容端点（DeepSeek/通义/Ollama）。
 * base-url/model/api-key 全部在 application.yml 配置。
 */
@Component
@ConditionalOnProperty(name = "lc.ai.provider", havingValue = "real")
public class RealChatPort implements ChatPort {

    private final ChatClient chatClient;

    public RealChatPort(org.springframework.ai.chat.model.ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public String ask(String system, String user) {
        return chatClient.prompt().system(system).user(user).call().content();
    }

    @Override
    public Flux<String> stream(String system, String user) {
        return chatClient.prompt().system(system).user(user).stream().content();
    }
}
