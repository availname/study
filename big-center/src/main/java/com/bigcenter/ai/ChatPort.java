package com.bigcenter.ai;

/**
 * 模型访问层接口（learn-center ChatPort 总装版，策略模式——mock/real 可切换，业务零改动）。
 * 设计原则（stage7/04）：事实由系统提供，措辞由 AI 生成，失败有降级路径。
 */
public interface ChatPort {

    /** 同步问答 */
    String ask(String system, String user);
}
