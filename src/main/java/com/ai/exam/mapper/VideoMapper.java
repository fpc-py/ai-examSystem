package com.ai.exam.mapper;

import com.ai.exam.entity.Video;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
