package com.bigcenter.pay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bigcenter.auth.UserContext;
import com.bigcenter.common.BizException;
import com.bigcenter.common.ErrorCode;
import com.bigcenter.pay.entity.PayCallback;
import com.bigcenter.pay.entity.PayOrder;
import com.bigcenter.pay.mapper.PayCallbackMapper;
import com.bigcenter.pay.mapper.PayOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付核心服务（java-core/15 四关卡落地）：
 * ① 状态机 CAS——支付回调与超时关单并发竞态，一条 UPDATE 解决；
 * ② 回调幂等——渠道流水号唯一索引 + CAS 双层防重；
 * ③ 掉单补偿——主动查单 Job 补状态（对账 Job 见 ReconcileService）；
 * ④ 金额安全——单位一律分(long)、回调金额比对、验签。
 */
@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PayOrderMapper orderMapper;
    private final PayCallbackMapper callbackMapper;
    private final PaymentFacade facade;

    public PaymentService(PayOrderMapper orderMapper, PayCallbackMapper callbackMapper, PaymentFacade facade) {
        this.orderMapper = orderMapper;
        this.callbackMapper = callbackMapper;
        this.facade = facade;
    }

    /** 创建支付单 + 渠道下单（门面路由到策略实现） */
    public Map<String, Object> createPay(String channelCode, long userId, long amountFen, String subject) {
        if (amountFen <= 0) {
            throw new BizException(ErrorCode.PAY_ORDER_NOT_FOUND, "金额必须为正数（单位：分）");
        }
        PayOrder order = new PayOrder();
        order.setPayOrderNo("P" + com.bigcenter.common.Ids.nextId());
        order.setUserId(userId);
        order.setChannelCode(channelCode);
        order.setAmountFen(amountFen);
        order.setSubject(subject);
        order.setStatus(PayOrder.WAIT_PAY);
        orderMapper.insert(order);

        Map<String, String> credential = facade.createPay(channelCode, order.getPayOrderNo(), amountFen, subject);
        Map<String, Object> view = new LinkedHashMap<>(credential);
        view.put("payOrderNo", order.getPayOrderNo());
        view.put("status", order.getStatus());
        return view;
    }

    /**
     * 渠道回调处理（四关卡全在这）：
     * 验签 → 金额比对 → 流水唯一索引幂等 → CAS 推状态机。
     * 事务内只做轻活（验签+落流水+CAS），重活异步化——回调慢了渠道会重发雪崩（15 篇追问③）。
     */
    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(String channelTradeNo, String payOrderNo, long amountFen, String sign) {
        // ④ 验签（模拟渠道：签名 = payOrderNo + amount 的简单摘要；生产为渠道公钥验签）
        if (!MockSign.verify(payOrderNo, amountFen, sign)) {
            log.warn("traceId={} 回调验签失败 payOrderNo={}", MDC.get("traceId"), payOrderNo);
            throw new BizException(ErrorCode.PAY_SIGN_INVALID);
        }
        PayOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getPayOrderNo, payOrderNo));
        if (order == null) {
            throw new BizException(ErrorCode.PAY_ORDER_NOT_FOUND);
        }
        // ④ 金额比对：回调金额 ≠ 本地订单金额直接拒（防"1 分钱支付 100 元订单"）
        if (order.getAmountFen() != amountFen) {
            log.error("traceId={} 回调金额不一致 payOrderNo={} expect={} actual={}",
                    MDC.get("traceId"), payOrderNo, order.getAmountFen(), amountFen);
            throw new BizException(ErrorCode.PAY_SIGN_INVALID, "金额不一致");
        }
        // ② 幂等第一层：流水唯一索引——重复回调 INSERT 失败即跳过（渠道会重发 N 次）
        PayCallback cb = new PayCallback();
        cb.setChannelTradeNo(channelTradeNo);
        cb.setPayOrderNo(payOrderNo);
        cb.setAmountFen(amountFen);
        cb.setStatus(PayOrder.PAID);
        try {
            callbackMapper.insert(cb);
        } catch (DuplicateKeyException e) {
            log.info("traceId={} 重复回调幂等跳过 channelTradeNo={}", MDC.get("traceId"), channelTradeNo);
            return "DUPLICATE";
        }
        // ② 幂等第二层 + ①状态机 CAS：WHERE status=10 只生效一次
        int n = orderMapper.casPaid(payOrderNo);
        if (n == 0) {
            PayOrder current = orderMapper.selectOne(
                    new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getPayOrderNo, payOrderNo));
            if (current.getStatus() == PayOrder.PAID) {
                return "DUPLICATE";              // 已支付：幂等忽略
            }
            if (current.getStatus() == PayOrder.CLOSED) {
                log.warn("traceId={} 关单赢了支付：支付单已关闭需退款 payOrderNo={}", MDC.get("traceId"), payOrderNo);
                return "CLOSED_NEED_REFUND";     // 输方分支：关单竞态赢了 → 走退款
            }
            throw new BizException(ErrorCode.PAY_STATUS_CONFLICT);
        }
        log.info("traceId={} 支付成功 payOrderNo={} amountFen={}", MDC.get("traceId"), payOrderNo, amountFen);
        return "SUCCESS";
    }

    /** 掉单补偿：主动查单（15 篇关卡③——不信任回调一定来） */
    public String compensateCheck(String payOrderNo) {
        PayOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getPayOrderNo, payOrderNo));
        if (order == null) {
            throw new BizException(ErrorCode.PAY_ORDER_NOT_FOUND);
        }
        if (order.getStatus() != PayOrder.WAIT_PAY) {
            return "NO_NEED";                    // 已有终态
        }
        String channelStatus = facade.queryOrder(order.getChannelCode(), payOrderNo);
        if ("SUCCESS".equals(channelStatus)) {
            // 查到渠道已收钱：补状态（与回调同一 CAS 路径，天然幂等）
            orderMapper.casPaid(payOrderNo);
            log.info("traceId={} 掉单补偿成功 payOrderNo={}", MDC.get("traceId"), payOrderNo);
            return "COMPENSATED";
        }
        return "STILL_WAITING";
    }

    /** 退款：CAS 已支付→已退款（红包过期退款在第 5 步调用） */
    public void refund(String payOrderNo) {
        int n = orderMapper.casRefund(payOrderNo);
        if (n == 0) {
            throw new BizException(ErrorCode.PAY_STATUS_CONFLICT, "仅已支付订单可退款");
        }
    }

    /** 当前用户支付单查询（越权防护示范：SQL 直接 where user_id——15/19 篇铁律） */
    public PayOrder findByNo(String payOrderNo, long loginUserId) {
        PayOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PayOrder>()
                        .eq(PayOrder::getPayOrderNo, payOrderNo)
                        .eq(PayOrder::getUserId, loginUserId));   // 数据权限在查询条件里，天然不越权
        if (order == null) {
            throw new BizException(ErrorCode.PAY_ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 模拟渠道回调签名工具（生产为渠道公钥 RSA 验签） */
    static class MockSign {
        static String of(String payOrderNo, long amountFen) {
            return String.valueOf((payOrderNo + ":" + amountFen).hashCode());
        }

        static boolean verify(String payOrderNo, long amountFen, String sign) {
            return of(payOrderNo, amountFen).equals(sign);
        }
    }

    /** 供 Controller 生成模拟渠道回调签名（演示用） */
    public String mockSignForDemo(String payOrderNo, long amountFen) {
        return MockSign.of(payOrderNo, amountFen);
    }
}
