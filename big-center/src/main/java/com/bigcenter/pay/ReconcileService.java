package com.bigcenter.pay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bigcenter.pay.entity.PayCallback;
import com.bigcenter.pay.entity.PayOrder;
import com.bigcenter.pay.mapper.PayCallbackMapper;
import com.bigcenter.pay.mapper.PayOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * T+1 三单对账（java-core/15 关卡④：平台流水 vs 渠道账单 vs 订单状态）。
 * 差错分类：渠道有平台无→掉单补单；平台有渠道无→伪造回调（验签事故）告警；
 * 两边有状态没改→补状态。差错进差错池（演示：日志+返回结构，生产入表+告警）。
 */
@Service
public class ReconcileService {
    private static final Logger log = LoggerFactory.getLogger(ReconcileService.class);

    private final PayCallbackMapper callbackMapper;
    private final PayOrderMapper orderMapper;
    private final PaymentService paymentService;
    private final PaymentFacade facade;

    public ReconcileService(PayCallbackMapper callbackMapper, PayOrderMapper orderMapper,
                            PaymentService paymentService, PaymentFacade facade) {
        this.callbackMapper = callbackMapper;
        this.orderMapper = orderMapper;
        this.paymentService = paymentService;
        this.facade = facade;
    }

    /** 对账结果（差错池） */
    public record Diff(String type, String payOrderNo, String detail) {}

    public List<Diff> reconcile() {
        List<Diff> diffs = new ArrayList<>();
        // 三单之渠道账单
        Map<String, Long> channelBill = facade.route("MOCK").downloadBill("");
        // 三单之平台流水（回调流水表）
        Map<String, PayCallback> platformFlows = callbackMapper.selectList(null).stream()
                .collect(java.util.stream.Collectors.toMap(PayCallback::getPayOrderNo, c -> c,
                        (a, b) -> a));
        // 三单之订单状态
        Map<String, PayOrder> orders = orderMapper.selectList(null).stream()
                .collect(java.util.stream.Collectors.toMap(PayOrder::getPayOrderNo, o -> o));

        // ① 渠道有、平台无 → 掉单（回调+查单全漏）：补单
        for (String channelOrderNo : channelBill.keySet()) {
            String payOrderNo = extractPayOrderNo(channelOrderNo);
            if (!platformFlows.containsKey(payOrderNo)) {
                diffs.add(new Diff("MISSING_PLATFORM_FLOW", payOrderNo, "渠道有流水平台无——掉单，补查单"));
                paymentService.compensateCheck(payOrderNo);
            }
        }
        // ② 平台有、渠道无 → 严重：钱没收却标已支付（多半是伪造回调绕过验签）
        for (Map.Entry<String, PayCallback> e : platformFlows.entrySet()) {
            if (!channelBill.containsKey(e.getKey())) {
                diffs.add(new Diff("PLATFORM_ONLY", e.getKey(), "平台有渠道无——疑似伪造回调，人工核查"));
                log.error("traceId={} 对账严重差错：平台有渠道无 payOrderNo={}", MDC.get("traceId"), e.getKey());
            }
        }
        // ③ 两边都有、状态没改 → 补状态 + 告警（为什么补偿任务也漏了）
        for (Map.Entry<String, PayCallback> e : platformFlows.entrySet()) {
            PayOrder order = orders.get(e.getKey());
            if (order != null && order.getStatus() == PayOrder.WAIT_PAY) {
                diffs.add(new Diff("STATUS_LAG", e.getKey(), "已收钱状态未推进——补状态"));
                paymentService.compensateCheck(e.getKey());
            }
        }
        log.info("traceId={} 对账完成 单数={} 差错={}", MDC.get("traceId"), orders.size(), diffs.size());
        return diffs;
    }

    /** 渠道流水号规则：mock-trade-{payOrderNo}（与回调构造对齐） */
    static String extractPayOrderNo(String channelTradeNo) {
        int i = channelTradeNo.indexOf("P");
        return i >= 0 ? channelTradeNo.substring(i) : channelTradeNo;
    }
}
