package com.ai.exam.controller;

import com.ai.exam.common.Result;
import com.ai.exam.entity.Banner;
import com.ai.exam.service.BannerService;
import com.ai.exam.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/banners")
@CrossOrigin
@Tag(name = "轮播图管理", description = "轮播图相关操作，包括图片上传、轮播图增删改查、状态管理等功能")
public class BannerController {
    @Autowired
    private BannerService bannerService;

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("/list")
    @Operation(summary = "获取所有轮播图")
    public Result<List<Banner>> getAllBanners() {
        return bannerService.getAllBanners();
    }

    @GetMapping("/active")
    @Operation(summary = "获取启用的轮播图", description = "获取状态为启用的轮播图列表，供前台首页展示使用")
    public Result<List<Banner>> getActiveBanners() {
        return bannerService.getActiveBanners();
    }

    @PutMapping("/toggle/{id}")
    @Operation(summary = "切换轮播图状态", description = "启用或禁用指定的轮播图，禁用后不会在前台显示")
    public Result<String> toggleBannerStatus(@Parameter(description = "轮播图ID") @PathVariable Long id,
                                             @Parameter(description = "是否启用，true为启用，false为禁用") @RequestParam Boolean isActive){
        return bannerService.toggleBannerStatus(id, isActive);
    }
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除轮播图", description = "根据ID删除指定的轮播图")
    public Result<String> deleteBanner(@Parameter(description = "轮播图ID") @PathVariable Long id) {
        return bannerService.deleteBanner(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取轮播图", description = "根据轮播图ID获取单个轮播图的详细信息")
    public Result<Banner> getBannerById(@Parameter(description = "轮播图ID") @PathVariable Long id) {
        Banner banner = bannerService.getById(id);
        if (banner != null) {
            return Result.success(banner);
        } else {
            return Result.error("轮播图不存在");
        }
    }

    @PostMapping("/upload-image")  // 处理POST请求
    @Operation(summary = "上传轮播图图片", description = "将图片文件上传到MinIO服务器，返回可访问的图片URL")  // API描述
    public Result<String> uploadBannerImage(
            @Parameter(description = "要上传的图片文件，支持jpg、png、gif等格式，大小限制5MB")
            @RequestParam("file") MultipartFile file){

        try {
            // 检查文件格式
            if (file.isEmpty()){
                return Result.error("请选择要上传的文件");
            }
            // 验证图片格式
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")){
                return Result.error("只能上传图片文件");
            }
            // 验证文件大小
            if (file.getSize() > 5 * 1024 * 1024){
                return Result.error("图片文件大小不能超过5MB");
            }

            // 执行文件上传
            Map<String, Object> uploadResult = fileUploadService.uploadFile(file, "banners");
            String imageUrl = (String) uploadResult.get("url");
            return Result.success(imageUrl, "图片上传成功");
        } catch (Exception e) {
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }
}
