package com.bigcenter.redpack.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bigcenter.redpack.entity.RedPack;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RedPackMapper extends BaseMapper<RedPack> {

    /** 状态机 CAS：抢完/过期退款推进（乐观锁语义，与支付中心同款） */
    @Update("UPDATE t_red_pack SET status = #{to} WHERE id = #{packId} AND status = #{from}")
    int casStatus(@Param("packId") Long packId, @Param("from") int from, @Param("to") int to);
}
