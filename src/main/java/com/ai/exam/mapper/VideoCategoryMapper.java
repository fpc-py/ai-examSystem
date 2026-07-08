package com.ai.exam.mapper;

import com.ai.exam.entity.VideoCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface VideoCategoryMapper extends BaseMapper<VideoCategory> {
    @Select("SELECT category_id, COUNT(*) as video_count FROM videos WHERE status = 1 GROUP BY category_id")
    @Results({
            @Result(property = "categoryId", column = "category_id"),
            @Result(property = "videoCount", column = "video_count")
    })
    List<Map<String, Object>> getCategoryVideoCount();

}
