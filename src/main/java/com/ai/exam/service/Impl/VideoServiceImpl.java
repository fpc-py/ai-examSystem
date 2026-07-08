package com.ai.exam.service.Impl;

import com.ai.exam.entity.Video;
import com.ai.exam.mapper.VideoLikeMapper;
import com.ai.exam.mapper.VideoMapper;
import com.ai.exam.service.FileUploadService;
import com.ai.exam.service.VideoService;
import com.ai.exam.utils.IpUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class VideoServiceImpl implements VideoService {
    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoLikeMapper videoLikeMapper;

    @Autowired
    private FileUploadService fileUploadService;

    @Override
    public IPage<Video> getVideoForAdmin(Integer page, Integer size, Integer status, Integer uploaderType, String keyword) {
        Page<Video> pageObj = new Page<>(page, size);
        IPage<Video> result = videoMapper.getVideosForAdmin(pageObj, status, uploaderType, keyword);

        // 格式化视频信息
        result.getRecords().forEach(video -> {
            formatVideoInfo(video);
            formatVideoStatus(video);
        });

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> uploadVideoByAdmin(Video video, MultipartFile videoFile, MultipartFile coverFile, Long adminId) {
        HashMap<String, Object> result = new HashMap<>();

        if (videoFile.isEmpty() || videoFile == null) {
            throw new RuntimeException("视频文件不能为空");
        }
        try {
        Map<String,Object> videoUploadResult = fileUploadService.uploadFile(videoFile,"videos/original/");
        video.setFileUrl(videoUploadResult.get("url").toString());
        video.setFileSize(videoFile.getSize());

        if (coverFile != null && !coverFile.isEmpty()) {
            Map<String, Object> coverUploadResult = fileUploadService.uploadFile(coverFile, "videos/covers/");
            video.setCoverUrl(coverUploadResult.get("url").toString());
        }

        video.setUploaderType(Video.UPLOADER_TYPE_ADMIN);
        video.setAdminId(adminId);
        video.setStatus(Video.STATUS_PUBLISHED); // 管理员上传直接发布
        video.setAuditAdminId(adminId);
        video.setAuditTime(LocalDateTime.now());
        video.setViewCount(0L);
        video.setLikeCount(0L);
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());

        // 保存视频信息
        videoMapper.insert(video);

        result.put("success", true);
        result.put("message", "视频上传成功");
        result.put("videoId", video.getId());
    } catch (Exception e) {
        throw new RuntimeException("视频上传失败：" + e.getMessage());
    }

        return result;
    }

    private void formatVideoStatus(Video video) {
        // 上传者类型文本
        if (video.getUploaderType() == Video.UPLOADER_TYPE_USER) {
            video.setUploaderTypeText("用户投稿");
        } else if (video.getUploaderType() == Video.UPLOADER_TYPE_ADMIN) {
            video.setUploaderTypeText("管理员上传");
        }

        // 状态文本
        switch (video.getStatus()) {
            case 0:
                video.setStatusText("待审核");
                break;
            case 1:
                video.setStatusText("已发布");
                break;
            case 2:
                video.setStatusText("已拒绝");
                break;
            case 3:
                video.setStatusText("已下架");
                break;
            default:
                video.setStatusText("未知状态");
        }
    }

    /**
     * 格式化视频信息
     */
    private void formatVideoInfo(Video video) {
        // 格式化时长
        if (video.getDuration() != null) {
            int minutes = video.getDuration() / 60;
            int seconds = video.getDuration() % 60;
            video.setDurationText(String.format("%02d:%02d", minutes, seconds));
        }

        // 格式化文件大小
        if (video.getFileSize() != null) {
            video.setFileSizeText(formatFileSize(video.getFileSize()));
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1fKB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1fMB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1fGB", size / (1024.0 * 1024 * 1024));
        }
    }
}
