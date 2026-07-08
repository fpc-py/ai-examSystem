package com.ai.exam.mapper;

import com.ai.exam.entity.Notice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {

    @Select("SELECT * FROM notices WHERE is_active = true ORDER BY priority DESC, create_time DESC")
    List<Notice> selectActiveNotices();

    @Select("SELECT * FROM notices WHERE is_active = true ORDER BY priority DESC, create_time DESC LIMIT #{limit}")
    List<Notice> selectLatestNotices(int limit);
}
