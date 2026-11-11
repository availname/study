package com.bigcenter.push.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bigcenter.push.PushMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PushMessageMapper extends BaseMapper<PushMessage> {
}
