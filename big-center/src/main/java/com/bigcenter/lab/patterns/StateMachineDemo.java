package com.bigcenter.lab.patterns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 状态机演示（教程: java-core/06-设计模式/05-责任链与状态机）：
 * 合法迁移表驱动 + 违规迁移拒绝——big-center 支付单/红包生命周期同款。
 * 生产实现一条 UPDATE ... WHERE status=from（CAS）——内存版演示迁移逻辑。
 */
public class StateMachineDemo {
    private static final Logger log = LoggerFactory.getLogger(StateMachineDemo.class);

    enum State { WAIT_PAY, PAID, CLOSED, REFUNDED }

    /** 迁移表：状态 → (事件 → 下一状态)。表驱动：新增状态只加一行 */
    static final Map<State, Map<String, State>> TRANSITIONS = Map.of(
            State.WAIT_PAY, Map.of("pay", State.PAID, "close", State.CLOSED),
            State.PAID, Map.of("refund", State.REFUNDED),
            State.CLOSED, Map.of("refund", State.REFUNDED));

    static State fire(State current, String event) {
        State next = TRANSITIONS.getOrDefault(current, Map.of()).get(event);
        if (next == null) {
            throw new IllegalStateException("非法迁移: " + current + " + " + event);
        }
        return next;
    }

    public static void main(String[] args) {
        State s = State.WAIT_PAY;
        log.info("初始：{}", s);

        s = fire(s, "pay");
        log.info("pay → {}（CAS 语义：WHERE status=WAIT_PAY 只生效一次）", s);

        try {
            fire(s, "pay");     // 已支付再支付——重复回调幂等场景
        } catch (IllegalStateException e) {
            log.info("重复支付被拒：{}", e.getMessage());
        }

        s = fire(s, "refund");
        log.info("refund → {}", s);

        try {
            fire(s, "close");   // 已退款不能关单
        } catch (IllegalStateException e) {
            log.info("非法迁移被拒：{}", e.getMessage());
        }
        log.info("要点：状态机的价值=把'哪些迁移合法'收拢成一张表，业务代码只发事件不写 if-else 泥球");
    }
}
