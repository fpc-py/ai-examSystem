package com.ai.exam.mapper;

import com.ai.exam.entity.Video;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface VideoMapper extends BaseMapper<Video> {



    @Select("<script>" +
            "SELECT v.*, vc.name as category_name, u.real_name as audit_admin_name " +
            "FROM videos v " +
            "LEFT JOIN video_categories vc ON v.category_id = vc.id " +
            "LEFT JOIN users u ON v.audit_admin_id = u.id " +
            "WHERE 1=1 " +
            "<if test='status != null'> AND v.status = #{status} </if>" +
            "<if test='uploaderType != null'> AND v.uploader_type = #{uploaderType} </if>" +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (v.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR v.uploader_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY v.created_at DESC" +
            "</script>")
    IPage<Video> getVideosForAdmin(Page<?> page,
                                   @Param("status") Integer status,
                                   @Param("uploaderType") Integer uploaderType,
                                   @Param("keyword") String keyword);
    @Select("SELECT " +
            "COUNT(*) as total_count, " +
            "COUNT(CASE WHEN status = 0 THEN 1 END) as pending_count, " +
            "COUNT(CASE WHEN status = 1 THEN 1 END) as published_count, " +
            "COUNT(CASE WHEN status = 2 THEN 1 END) as rejected_count, " +
            "COUNT(CASE WHEN uploader_type = 1 THEN 1 END) as user_upload_count, " +
            "COUNT(CASE WHEN uploader_type = 2 THEN 1 END) as admin_upload_count " +
            "FROM videos")
    Map<String, Object> getVideoStatistics();

    @Select("<script>" +
            "SELECT v.*, vc.name as category_name " +
            "FROM videos v " +
            "LEFT JOIN video_categories vc ON v.category_id = vc.id " +
            "WHERE v.status = 1 " +
            "<if test='categoryId != null'> AND v.category_id = #{categoryId} </if>" +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (v.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR v.tags LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY v.created_at DESC" +
            "</script>")
    IPage<Video> getPublishedVideosPage(Page<Video> pageObj, Long categoryId, String keyword);

    @Select("SELECT v.*, vc.name as category_name " +
            "FROM videos v " +
            "LEFT JOIN video_categories vc ON v.category_id = vc.id " +
            "WHERE v.status = 1 " +
            "ORDER BY v.view_count DESC " +
            "LIMIT #{limit}")
    List<Video> getPopularVideos(Integer limit);

    @Select("SELECT v.*, vc.name as category_name " +
            "FROM videos v " +
            "LEFT JOIN video_categories vc ON v.category_id = vc.id " +
            "WHERE v.status = 1 " +
            "ORDER BY v.created_at DESC " +
            "LIMIT #{limit}")
    List<Video> getLatestVideos(Integer limit);

    @Update("UPDATE videos SET view_count = view_count + 1 WHERE id = #{videoId}")
    void incrementViewCount(Long videoId);


    @Update("UPDATE videos SET like_count = like_count + 1 WHERE id = #{videoId}")
    void incrementLikeCount(Long videoId);
    @Update("UPDATE videos SET like_count = like_count - 1 WHERE id = #{videoId} AND like_count > 0")
    void decrementLikeCount(Long videoId);
}
