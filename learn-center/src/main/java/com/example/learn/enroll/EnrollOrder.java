package com.example.learn.enroll;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 抢课订单（一人一课唯一索引 = 幂等兜底，教程 stage3/02） */
@TableName("t_enroll_order")
public class EnrollOrder {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String orderNo;
    public Long userId;
    public Long courseId;
    public Integer status;      // 0待支付 1已支付 2已取消
    public Integer price;
}
