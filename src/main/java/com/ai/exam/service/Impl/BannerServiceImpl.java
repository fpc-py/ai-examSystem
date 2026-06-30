package com.ai.exam.service.Impl;

import com.ai.exam.common.Result;
import com.ai.exam.entity.Banner;
import com.ai.exam.mapper.BannerMapper;
import com.ai.exam.service.BannerService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper,Banner> implements BannerService {

    @Override
    public Result<List<Banner>> getAllBanners() {
        try {
            QueryWrapper<Banner> wrapper = new QueryWrapper<>();
            wrapper.orderByAsc("sort_order");
            List<Banner> banners = baseMapper.selectList(wrapper);
            return Result.success(banners);
        } catch (Exception e) {
            return Result.error("获取轮播图列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<Banner>> getActiveBanners() {
        try {
            List<Banner> banners = baseMapper.selectActiveBanner();
            return Result.success(banners);
        } catch (Exception e) {
            return Result.error("获取轮播图失败：" + e.getMessage());
        }
    }

    @Override
    public Result<String> toggleBannerStatus(Long id, Boolean isActive) {
        try {
            Banner banner = Banner.builder()
                    .id(id)
                    .isActive(isActive)
                    .updateTime(LocalDateTime.now())
                    .build();
            boolean success = this.updateById(banner);
            if (success) {
                String status = isActive ? "启用" : "禁用";
                return Result.success("轮播图" + status + "成功");
            } else {
                return Result.error("轮播图状态更新失败");
            }
        } catch (Exception e) {
            return Result.error("轮播图状态更新失败：" + e.getMessage());
        }

    }

    @Override
    public Result<String> deleteBanner(Long id) {
        try {
            boolean success = this.removeById(id);
            if (success) {
                return Result.success("轮播图删除成功");
            } else {
                return Result.error("轮播图删除失败");
            }
        } catch (Exception e) {
            return Result.error("轮播图删除失败：" + e.getMessage());
        }
    }

    @Override
    public Result<String> addBanner(Banner banner) {
        try {
            banner.setCreateTime(LocalDateTime.now());
            banner.setUpdateTime(LocalDateTime.now());
            if (banner.getIsActive() == null) {
                banner.setIsActive(true); // 默认启用
            }
            if (banner.getSortOrder() == null) {
                banner.setSortOrder(0); // 默认排序
            }

            boolean success = this.save(banner);
            if (success) {
                return Result.success("轮播图添加成功");
            } else {
                return Result.error("轮播图添加失败");
            }
        } catch (Exception e) {
            return Result.error("轮播图添加失败：" + e.getMessage());
        }
    }

    @Override
    public Result<String> updateBanner(Banner banner) {
        try {
            banner.setUpdateTime(LocalDateTime.now());
            boolean success = this.updateById(banner);
            if (success) {
                return Result.success("轮播图更新成功");
            } else {
                return Result.error("轮播图更新失败");
            }
        } catch (Exception e) {
            return Result.error("轮播图更新失败：" + e.getMessage());
        }
    }
}
