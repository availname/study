package com.example.learn.course;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** MP：继承 BaseMapper 即拥有单表全套 CRUD（selectById/insert/selectPage/update...） */
@Mapper
public interface CourseMapper extends com.baomidou.mybatisplus.core.mapper.BaseMapper<Course> {
}
