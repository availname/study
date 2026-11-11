package com.bigcenter.pay;

import java.util.Map;

/**
 * 支付渠道策略接口（java-core/06 策略模式 + 15 篇追问①"多渠道"）：
 * 主流程只依赖本接口，新增渠道零改动——微信/支付宝/模拟渠道各自实现。
 */
public interface PaymentChannel {

    /** 渠道标识（路由用） */
    String channelCode();

    /** 渠道下单：返回渠道预支付凭据（真实场景为 prepay_id/支付表单串；模拟渠道直接造一个） */
    Map<String, String> prepay(String payOrderNo, long amountFen, String subject);

    /** 主动查单（掉单补偿用）：返回渠道侧状态字符串 */
    String queryOrder(String payOrderNo);

    /** 对账账单下载（T+1 三单对账用）：返回 渠道流水号 → 金额（分） */
    Map<String, Long> downloadBill(String date);
}
