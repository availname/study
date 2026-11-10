package com.example.learn.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Mock 实现（默认）：无 LLM API Key 也能全流程跑通——管道/SSE/降级照常验证，
 * 只是回复是预设文本。生产/有 Key 时配置 lc.ai.provider=real 切换真实模型。
 */
@Component
@ConditionalOnProperty(name = "lc.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockChatPort implements ChatPort {

    @Override
    public String ask(String system, String user) {
        return "【Mock 回复】已理解问题：" + user + "。配置真实 API Key 后即可获得模型回答。";
    }

    @Override
    public Flux<String> stream(String system, String user) {
        return Flux.just("【Mock 流式】", "已收到：", user)
                .delayElements(Duration.ofMillis(300));
    }
}
