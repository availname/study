package com.bigcenter.pay;

import com.bigcenter.common.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 门面+策略：按渠道码路由、新渠道零改动主流程——java-core/06 策略模式实战验收 */
class PaymentFacadeTest {

    static class FakeChannel implements PaymentChannel {
        private final String code;
        FakeChannel(String code) { this.code = code; }
        @Override public String channelCode() { return code; }
        @Override public Map<String, String> prepay(String no, long amount, String subject) {
            return Map.of("channel", code, "prepayId", "pre-" + no);
        }
        @Override public String queryOrder(String payOrderNo) { return "SUCCESS"; }
        @Override public Map<String, Long> downloadBill(String date) { return Map.of(); }
    }

    @Test
    void 按渠道码路由到对应策略() {
        PaymentFacade facade = new PaymentFacade(List.of(new FakeChannel("MOCK"), new FakeChannel("WECHAT")));
        Map<String, String> c1 = facade.createPay("MOCK", "P1", 100, "课");
        Map<String, String> c2 = facade.createPay("WECHAT", "P1", 100, "课");
        assertEquals("MOCK", c1.get("channel"));
        assertEquals("WECHAT", c2.get("channel"));
        assertEquals("pre-P1", c1.get("prepayId"), "模板方法骨架统一，实现可替换");
    }

    @Test
    void 未知渠道_抛4003_主流程零改动支持新渠道() {
        PaymentFacade facade = new PaymentFacade(List.of(new FakeChannel("MOCK")));
        BizException e = assertThrows(BizException.class, () -> facade.route("ALIPAY"));
        assertEquals(4003, e.getErrorCode().code());
        // 新增渠道只需加一个实现类—— facade 构造注入 List<PaymentChannel> 自动路由
        PaymentFacade extended = new PaymentFacade(List.of(new FakeChannel("MOCK"), new FakeChannel("ALIPAY")));
        assertEquals("ALIPAY", extended.route("ALIPAY").channelCode());
    }
}
