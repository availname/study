package com.example.learn.interview;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 面试题库（数据源：java-core/面试速查手册——教学元闭环：用它刷它教的题） */
@TableName("t_question")
public class Question {
    @TableId(type = IdType.AUTO)
    public Long id;
    public String category;
    public String title;
    public String answerMd;
    public Integer difficulty;

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getAnswerMd() { return answerMd; }
    public Integer getDifficulty() { return difficulty; }
}
