package com.example.learn.course;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 课程实体（MyBatis-Plus：@TableName + BaseMapper 即拥有单表 CRUD；Lambda 查询需要 getter） */
@TableName("t_course")
public class Course {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String title;
    public String teacher;
    public Integer price;
    public Integer quota;
    public Integer enrolled;
    public Integer status;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getTeacher() { return teacher; }
    public Integer getPrice() { return price; }
    public Integer getQuota() { return quota; }
    public Integer getEnrolled() { return enrolled; }
    public Integer getStatus() { return status; }
}
