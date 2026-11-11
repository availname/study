package com.bigcenter.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bigcenter.pay.entity.PayOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PayOrderMapper extends BaseMapper<PayOrder> {

    /** 状态机 CAS：UPDATE ... WHERE status=待支付——行锁保证支付回调与关单只有一个赢（15 篇关卡①） */
    @Update("UPDATE t_pay_order SET status = 20, paid_at = NOW(3) " +
            "WHERE pay_order_no = #{payOrderNo} AND status = 10")
    int casPaid(@Param("payOrderNo") String payOrderNo);

    /** 状态机 CAS：关单（超时/用户取消） */
    @Update("UPDATE t_pay_order SET status = 30 WHERE pay_order_no = #{payOrderNo} AND status = 10")
    int casClose(@Param("payOrderNo") String payOrderNo);

    /** 状态机 CAS：退款（已支付 → 已退款） */
    @Update("UPDATE t_pay_order SET status = 40 WHERE pay_order_no = #{payOrderNo} AND status = 20")
    int casRefund(@Param("payOrderNo") String payOrderNo);
}
