package com.bigcenter.shortlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bigcenter.shortlink.entity.ShortLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLink> {

    /** 统计刷库（Write Behind：Redis INCR 累积，定时/阈值回写 MySQL） */
    @Update("UPDATE t_short_link SET click_count = click_count + #{delta} WHERE short_code = #{shortCode}")
    int incrClick(String shortCode, long delta);
}
