package com.ai.exam.controller;

import com.ai.exam.common.Result;
import com.ai.exam.entity.VideoCategory;
import com.ai.exam.service.VideoCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/video-categories")
@Tag(name = "视频分类管理", description = "视频分类相关操作，包括分类的增删改查、树形结构管理等功能")
public class VideoCategoryController {
    @Autowired
    private VideoCategoryService videoCategoryService;

    @GetMapping
    @Operation(summary = "获取分类列表", description = "获取所有视频分类列表，包含每个分类下的视频数量统计")
    public Result<List<VideoCategory>> getCategories() {
        return Result.success(videoCategoryService.getAllCategories());
    }

    @GetMapping("/tree")
    @Operation(summary = "获取分类树形结构", description = "获取视频分类的树形层级结构，用于前端组件展示")
    public Result<List<VideoCategory>> getCategoryTree() {
        return Result.success(videoCategoryService.getCategoryTree());
    }
}
