package com.example.mqdemo.seckill;

/** 秒杀异步下单消息 */
public class SeckillMsg {
    private Long orderId;
    private Long userId;
    private Long skuId;

    public SeckillMsg() {}

    public SeckillMsg(Long orderId, Long userId, Long skuId) {
        this.orderId = orderId;
        this.userId = userId;
        this.skuId = skuId;
    }

    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Long getSkuId() { return skuId; }
}
