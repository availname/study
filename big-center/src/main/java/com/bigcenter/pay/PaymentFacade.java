package com.bigcenter.pay;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付门面（java-core/06 门面模式 + 模板方法思想）：
 * 对外暴露统一的 createPay/queryOrder/refund，内部按渠道码路由到策略实现——
 * 调用方（红包退款/课程秒杀）不感知渠道差异。
 */
@Service
public class PaymentFacade {

    private final Map<String, PaymentChannel> channels;

    /** Spring 注入所有 PaymentChannel 实现，按 channelCode 建路由表 */
    public PaymentFacade(List<PaymentChannel> channelImpls) {
        this.channels = channelImpls.stream()
                .collect(Collectors.toMap(PaymentChannel::channelCode, c -> c));
    }

    public PaymentChannel route(String channelCode) {
        PaymentChannel channel = channels.get(channelCode);
        if (channel == null) {
            throw new com.bigcenter.common.BizException(com.bigcenter.common.ErrorCode.PAY_CHANNEL_UNSUPPORTED);
        }
        return channel;
    }

    /** 门面统一入口：下单（模板方法骨架——路由校验/参数组装固定，渠道实现可替换） */
    public Map<String, String> createPay(String channelCode, String payOrderNo, long amountFen, String subject) {
        return route(channelCode).prepay(payOrderNo, amountFen, subject);
    }

    public String queryOrder(String channelCode, String payOrderNo) {
        return route(channelCode).queryOrder(payOrderNo);
    }
}
