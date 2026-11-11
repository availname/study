package com.bigcenter.pay;

import com.bigcenter.auth.UserContext;
import com.bigcenter.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 支付接口（/api/pay/**，JWT 保护；回调接口 /api/pay/callback 渠道调用——教学演示放行） */
@RestController
@RequestMapping("/api/pay")
public class PayController {

    private final PaymentService paymentService;
    private final MockPayChannel mockChannel;

    public PayController(PaymentService paymentService, MockPayChannel mockChannel) {
        this.paymentService = paymentService;
        this.mockChannel = mockChannel;
    }

    /** 创建支付单（金额单位：分——15 篇铁律：钱上不允许 double） */
    @PostMapping("/order")
    public ApiResponse<Map<String, Object>> create(@RequestParam String channelCode,
                                                   @RequestParam long amountFen,
                                                   @RequestParam String subject) {
        return ApiResponse.ok(paymentService.createPay(channelCode, UserContext.get(), amountFen, subject));
    }

    /** 渠道异步回调（真实由微信/支付宝服务器调用；演示用模拟渠道触发） */
    @PostMapping("/callback")
    public ApiResponse<String> callback(@RequestParam String channelTradeNo,
                                        @RequestParam String payOrderNo,
                                        @RequestParam long amountFen,
                                        @RequestParam String sign) {
        String result = paymentService.handleCallback(channelTradeNo, payOrderNo, amountFen, sign);
        if ("SUCCESS".equals(result)) {
            mockChannel.recordTrade(channelTradeNo, amountFen);   // 模拟渠道入账（对账数据源）
        }
        return ApiResponse.ok(result);
    }

    /** 主动查单（掉单补偿演示入口） */
    @PostMapping("/{payOrderNo}/compensate")
    public ApiResponse<String> compensate(@PathVariable String payOrderNo) {
        return ApiResponse.ok(paymentService.compensateCheck(payOrderNo));
    }

    /** 我的支付单（越权防护：SQL where user_id） */
    @GetMapping("/{payOrderNo}")
    public ApiResponse<Object> get(@PathVariable String payOrderNo) {
        return ApiResponse.ok(paymentService.findByNo(payOrderNo, UserContext.get()));
    }
}
