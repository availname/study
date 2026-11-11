package com.bigcenter.redpack.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 红包主体（金额与份数发时可先落 MySQL 状态=进行中——明细以 MQ 消息为准，14 篇追问①） */
@TableName("t_red_pack")
public class RedPack {
    public static final int ONGOING = 10;
    public static final int FINISHED = 20;
    public static final int REFUNDING = 30;
    public static final int REFUNDED = 40;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerId;
    private Long totalFen;
    private Integer count;
    private Integer grabbedCount;
    private Integer status;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Long getTotalFen() { return totalFen; }
    public void setTotalFen(Long totalFen) { this.totalFen = totalFen; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public Integer getGrabbedCount() { return grabbedCount; }
    public void setGrabbedCount(Integer grabbedCount) { this.grabbedCount = grabbedCount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
