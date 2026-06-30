package com.ai.exam.service;

import com.ai.exam.common.Result;
import com.ai.exam.entity.Banner;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BannerService extends IService<Banner> {
    Result<List<Banner>> getAllBanners();

    Result<List<Banner>> getActiveBanners();

    Result<String> toggleBannerStatus(Long id, Boolean isActive);

    Result<String> deleteBanner(Long id);
}
