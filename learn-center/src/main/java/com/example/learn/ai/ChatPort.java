package com.example.learn.ai;

import reactor.core.publisher.Flux;

/** 模型访问层接口（策略模式——mock/real 可切换，业务代码零改动） */
public interface ChatPort {

    /** 同步问答 */
    String ask(String system, String user);

    /** 流式输出（逐 token 推送，SSE 打字机效果的数据源） */
    Flux<String> stream(String system, String user);
}
