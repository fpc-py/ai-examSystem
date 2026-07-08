package com.ai.exam.service;

import com.ai.exam.entity.Video;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VideoService {


    IPage<Video> getVideoForAdmin(Integer page, Integer size, Integer status, Integer uploaderType, String keyword);

    Map<String, Object> uploadVideoByAdmin(Video video, MultipartFile videoFile, MultipartFile coverFile, Long adminId);
}
