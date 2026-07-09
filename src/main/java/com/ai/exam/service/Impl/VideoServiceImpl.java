package com.ai.exam.service.Impl;

import com.ai.exam.entity.Video;
import com.ai.exam.entity.VideoLike;
import com.ai.exam.entity.VideoView;
import com.ai.exam.mapper.VideoLikeMapper;
import com.ai.exam.mapper.VideoMapper;
import com.ai.exam.mapper.VideoViewMapper;
import com.ai.exam.service.FileUploadService;
import com.ai.exam.service.VideoService;
import com.ai.exam.utils.IpUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
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

    @Autowired
    private VideoViewMapper videoViewMapper;

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

    @Override
    public void auditVideo(Long videoId, Integer status, String reason, Long adminId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }

        if (video.getStatus() != Video.STATUS_PENDING) {
            throw new RuntimeException("只能审核待审核状态的视频");
        }

        if (status == Video.STATUS_REJECTED && (reason == null || reason.trim().isEmpty())) {
            throw new RuntimeException("拒绝审核时必须填写拒绝原因");
        }

        // 更新审核信息
        video.setStatus(status);
        video.setAuditAdminId(adminId);
        video.setAuditTime(LocalDateTime.now());
        video.setAuditReason(reason);
        video.setUpdatedAt(LocalDateTime.now());

        videoMapper.updateById(video);
    }

    @Override
    public void offlineVideo(Long videoId, Long adminId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }

        if (video.getStatus() != Video.STATUS_PUBLISHED) {
            throw new RuntimeException("只能下架已发布的视频");
        }

        video.setStatus(Video.STATUS_OFFLINE);
        video.setAuditAdminId(adminId);
        video.setAuditTime(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());

        videoMapper.updateById(video);
    }

    @Override
    public void deleteVideo(Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }

        // 删除相关数据
        videoLikeMapper.delete(new LambdaQueryWrapper<VideoLike>().eq(VideoLike::getVideoId, videoId));
        videoViewMapper.delete(new LambdaQueryWrapper<VideoView>().eq(VideoView::getVideoId, videoId));

        // 删除视频记录
        videoMapper.deleteById(videoId);

        // TODO: 删除文件存储中的视频文件和封面文件

    }

    @Override
    public Map<String, Object> getVideoStatistics() {
        return videoMapper.getVideoStatistics();
    }

    @Override
    public Map<String, Object> getVideoDetailStats(Long videoId, Integer days) {
        Map<String, Object> stats = new HashMap<>();

        // 基本统计
        Long viewCount = videoViewMapper.getViewCountByVideoId(videoId);
        Long likeCount = videoLikeMapper.getLikeCountByVideoId(videoId);
        Double avgDuration = videoViewMapper.getAverageViewDuration(videoId);

        stats.put("viewCount", viewCount);
        stats.put("likeCount", likeCount);
        stats.put("averageViewDuration", avgDuration);

        // 按日期统计
        List<Map<String, Object>> dailyStats = videoViewMapper.getViewStatsByDate(videoId, days);
        stats.put("dailyViewStats", dailyStats);

        return stats;
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
