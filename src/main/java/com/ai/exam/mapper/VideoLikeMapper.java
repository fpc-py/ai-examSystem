package com.ai.exam.mapper;

import com.ai.exam.entity.VideoLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VideoLikeMapper extends BaseMapper<VideoLike> {
    @Select("SELECT COUNT(*) > 0 FROM video_likes WHERE video_id = #{videoId} AND user_ip = #{userIp}")
    boolean isLikedByIp(Long id, String userIp);
}
