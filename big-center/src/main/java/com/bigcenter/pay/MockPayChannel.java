package com.bigcenter.pay;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟渠道（教学环境无真实微信/支付宝）：
 * prepay 直接回凭据、queryOrder 按"回调已写入流水"判定状态、账单从内存流水生成——
 * 让状态机/幂等/对账四关卡全部可在本地闭环验证。
 */
@Component
public class MockPayChannel implements PaymentChannel {

    /** 内存流水（渠道侧视角）：channelTradeNo → 金额分 */
    private final Map<String, Long> tradeBook = new LinkedHashMap<>();

    @Override
    public String channelCode() {
        return "MOCK";
    }

    @Override
    public Map<String, String> prepay(String payOrderNo, long amountFen, String subject) {
        Map<String, String> credential = new LinkedHashMap<>();
        credential.put("channelCode", channelCode());
        credential.put("payOrderNo", payOrderNo);
        credential.put("amountFen", String.valueOf(amountFen));
        credential.put("prepayId", "mock-prepay-" + payOrderNo);
        return credential;
    }

    /** 渠道收到回调模拟入账（由 CallbackController 的模拟渠道动作调用） */
    public void recordTrade(String channelTradeNo, long amountFen) {
        tradeBook.put(channelTradeNo, amountFen);
    }

    @Override
    public String queryOrder(String payOrderNo) {
        // 渠道侧：该支付单有流水即"SUCCESS"
        return tradeBook.keySet().stream()
                .filter(no -> no.contains(payOrderNo))
                .findFirst().map(k -> "SUCCESS").orElse("WAITING");
    }

    @Override
    public Map<String, Long> downloadBill(String date) {
        return Map.copyOf(tradeBook);
    }
}
