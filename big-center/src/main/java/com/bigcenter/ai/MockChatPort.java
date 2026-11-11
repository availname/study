package com.bigcenter.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock 实现（默认）：无 LLM API Key 也能全流程跑通（learn-center MockChatPort 总装版）。
 * 配置 big-center.ai.provider=real 切换真实模型。
 */
@Component
@ConditionalOnProperty(name = "big-center.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockChatPort implements ChatPort {

    @Override
    public String ask(String system, String user) {
        return "【Mock 报告】已基于你的学习数据生成建议：" + user.strip()
                + "。配置真实 API Key 后即可获得模型个性化表述。";
    }
}
