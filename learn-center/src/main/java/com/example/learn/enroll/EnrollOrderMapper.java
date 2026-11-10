package com.example.learn.enroll;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EnrollOrderMapper extends com.baomidou.mybatisplus.core.mapper.BaseMapper<EnrollOrder> {

    /** CAS 支付：只有待支付才能支付（防与超时取消竞态——教程 stage4/02） */
    @Update("UPDATE t_enroll_order SET status = 1, paid_at = NOW(3) WHERE order_no = #{orderNo} AND status = 0")
    int casPay(@Param("orderNo") String orderNo);

    /** CAS 取消：只有待支付才能取消（超时关单/主动取消共用） */
    @Update("UPDATE t_enroll_order SET status = 2, cancel_at = NOW(3) WHERE order_no = #{orderNo} AND status = 0")
    int casCancel(@Param("orderNo") String orderNo);
}
