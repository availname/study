package com.bigcenter.lab.patterns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 策略+模板方法+责任链组合演示（教程: java-core/06-设计模式/03-行为型、05-责任链与状态机）：
 * 与 big-center 支付中心同构——策略（渠道）/门面（PaymentFacade）/模板（prepay 骨架）/责任链（回调校验链）。
 */
public class StrategyTemplateChainDemo {
    private static final Logger log = LoggerFactory.getLogger(StrategyTemplateChainDemo.class);

    // ---- 策略：支付渠道（与 com.bigcenter.pay.PaymentChannel 同构） ----
    interface Channel {
        String code();
        String prepay(String orderNo, long fen);
    }

    static class WechatChannel implements Channel {
        @Override public String code() { return "WECHAT"; }
        @Override public String prepay(String orderNo, long fen) { return "wx-prepay-" + orderNo; }
    }

    static class AlipayChannel implements Channel {
        @Override public String code() { return "ALIPAY"; }
        @Override public String prepay(String orderNo, long fen) { return "ali-form-" + orderNo; }
    }

    // ---- 门面：按渠道码路由（主流程零改动支持新渠道） ----
    static class PayFacade {
        private final List<Channel> channels;
        PayFacade(List<Channel> channels) { this.channels = channels; }
        String createPay(String code, String orderNo, long fen) {
            return channels.stream().filter(c -> c.code().equals(code)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("不支持的渠道: " + code))
                    .prepay(orderNo, fen);
        }
    }

    // ---- 模板方法：固定"下单骨架"，子类填差异步骤 ----
    abstract static class AbstractOrderFlow {
        final String create(String orderNo) {              // final：骨架不可改
            validate(orderNo);                             // ① 钩子
            String no = doCreate(orderNo);                 // ② 差异步骤
            afterCreate(no);                               // ③ 后置钩子
            return no;
        }
        protected void validate(String no) { log.info("通用校验 {}", no); }
        protected abstract String doCreate(String no);     // 子类差异
        protected void afterCreate(String no) { log.info("通用后置（发事件） {}", no); }
    }

    static class SeckillOrderFlow extends AbstractOrderFlow {
        @Override protected String doCreate(String no) { log.info("秒杀单：先扣 Redis 库存再建单 {}", no); return no; }
    }

    // ---- 责任链：回调校验链（与 15 篇支付回调防线同构） ----
    interface Handler {
        boolean handle(String req);                        // 返回 false = 拦截
        Handler next();
    }

    static class SignHandler implements Handler {
        private final Handler next;
        SignHandler(Handler next) { this.next = next; }
        @Override public boolean handle(String req) {
            if (!req.contains("sign=ok")) { log.info("验签失败拦截"); return false; }
            return next == null || next.handle(req);
        }
        @Override public Handler next() { return next; }
    }

    static class AmountHandler implements Handler {
        private final Handler next;
        AmountHandler(Handler next) { this.next = next; }
        @Override public boolean handle(String req) {
            if (req.contains("amount=0")) { log.info("金额为 0 拦截"); return false; }
            return next == null || next.handle(req);
        }
        @Override public Handler next() { return next; }
    }

    public static void main(String[] args) {
        // 策略+门面
        PayFacade facade = new PayFacade(List.of(new WechatChannel(), new AlipayChannel()));
        log.info("① 门面路由 WECHAT：{}", facade.createPay("WECHAT", "P1", 100));
        log.info("① 门面路由 ALIPAY：{}", facade.createPay("ALIPAY", "P1", 100));

        // 模板方法
        new SeckillOrderFlow().create("S1001");

        // 责任链：验签 → 金额 → 业务
        Handler chain = new SignHandler(new AmountHandler(null));
        log.info("③ 链1（合法）：{}", chain.handle("sign=ok&amount=100"));
        log.info("③ 链2（金额 0）：{}", chain.handle("sign=ok&amount=0"));
        log.info("③ 链3（伪造签名）：{}", chain.handle("sign=bad&amount=100"));
        log.info("组合心法：策略解决'多实现怎么选'，模板解决'骨架怎么定'，责任链解决'多道防线怎么串'——big-center 支付中心三者全用");
    }
}
