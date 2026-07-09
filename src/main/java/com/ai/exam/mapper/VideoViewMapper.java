package com.ai.exam.mapper;

import com.ai.exam.entity.VideoView;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface VideoViewMapper extends BaseMapper<VideoView> {
    @Select("SELECT COUNT(*) FROM video_views WHERE video_id = #{videoId}")
    Long getViewCountByVideoId(Long videoId);

    @Select("SELECT AVG(view_duration) FROM video_views WHERE video_id = #{videoId} AND view_duration > 0")
    Double getAverageViewDuration(Long videoId);

    @Select("SELECT DATE (created_at) as view_date,COUNT(*) as view_count"+
    "FROM video_views"+
    "WHERE video_id = #{videoId} AND create_at >= DATE_SUB(NOW(),INTERVAL #{days} DAY)"+
    "GROUP BY DATE(created_at)"+
    "GROUP BY view_date DESC")
    List<Map<String, Object>> getViewStatsByDate(Long videoId, Integer days);
}
