package com.example.mqmall.messaging;

import java.io.Serializable;

/** 所有 MQ 消息体定义（record 不可变，Jackson 原生支持） */
public final class Messages {
    private Messages() {}

    /** 订单已创建（下单成功） */
    public record OrderCreatedEvent(String orderNo, Long userId, Long skuId,
                                    Integer count, Integer amount,
                                    boolean failOnPurpose) implements Serializable {}

    /** 订单已支付 */
    public record OrderPaidEvent(String orderNo, Long userId, Long skuId,
                                 Integer count, Integer amount) implements Serializable {}

    /** 秒杀异步建单请求 */
    public record SeckillOrderRequest(Long orderId, Long userId, Long skuId) implements Serializable {}

    /** 媒体管道事件：提交/转码完成/审核完成/发布完成共用一个载体（stage5） */
    public record VideoEvent(String videoId, String title) implements Serializable {}
}
