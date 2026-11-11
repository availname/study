package com.bigcenter.post;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 帖子 Mapper（审核状态机 CAS：期望状态不匹配返回 0——并发推进只有一方赢） */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Update("UPDATE t_post SET status = #{next}, reject_reason = #{reason} WHERE id = #{id} AND status = #{expect}")
    int casStatus(@Param("id") long id, @Param("expect") int expect,
                  @Param("next") int next, @Param("reason") String reason);
}
