package com.ai.exam.service.Impl;

import com.ai.exam.entity.VideoCategory;
import com.ai.exam.mapper.VideoCategoryMapper;
import com.ai.exam.mapper.VideoMapper;
import com.ai.exam.service.VideoCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VideoCategoryServiceImpl implements VideoCategoryService {
    @Autowired
    private VideoCategoryMapper videoCategoryMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Override
    public List<VideoCategory> getAllCategories() {
        List<VideoCategory> categories = videoCategoryMapper.selectList(
                new LambdaQueryWrapper<VideoCategory>()
                        .orderByAsc(VideoCategory::getSortOrder)
        );

        // 获取并填充每个分类的视频数量
        fillVideoCount(categories);

        return categories;
    }

    @Override
    public List<VideoCategory> getCategoryTree() {
        // 获取所有启用的分类
        List<VideoCategory> allCategories = videoCategoryMapper.selectList(
                new LambdaQueryWrapper<VideoCategory>()
                        .eq(VideoCategory::getStatus, 1)
                        .orderByAsc(VideoCategory::getSortOrder)
        );

        // 获取并填充每个分类的视频数量
        fillVideoCount(allCategories);

        // 构建树形结构
        return buildTree(allCategories);
    }

    private List<VideoCategory> buildTree(List<VideoCategory> allCategories) {
        // 按parentId分组
        Map<Long, List<VideoCategory>> childrenMap = allCategories.stream().collect(Collectors.groupingBy(VideoCategory::getParentId));

        // 设置children属性，并从下至上汇总视频数量
allCategories.forEach(category -> {
    List<VideoCategory> children = childrenMap.getOrDefault(category.getId(), new ArrayList<>());
    category.setChildren(children);
    // 汇总子分类的视频数量到父分类
    long childrenCount = children.stream()
            .mapToLong(c -> c.getVideoCount() != null ? c.getVideoCount() : 0L)
            .sum();
    long selfCount = category.getVideoCount() != null ? category.getVideoCount() : 0L;
    category.setVideoCount(selfCount + childrenCount);
});
        // 返回顶级分类（parentId = 0）
        return allCategories.stream()
                .filter(c -> c.getParentId() == 0)
                .collect(Collectors.toList());
    }

    private void fillVideoCount(List<VideoCategory> categories) {
        // 获取每个分类的视频数量
        List<Map<String, Object>> videoCountList = videoCategoryMapper.getCategoryVideoCount();

        // 将结果转换为Map<Long, Long>格式
        Map<Long, Long> videoCountMap = videoCountList.stream()
                .collect(Collectors.toMap(
                        map -> Long.valueOf(map.get("categoryId").toString()),
                        map -> Long.valueOf(map.get("videoCount").toString())
                ));

        // 设置视频数量
        categories.forEach(category ->
                category.setVideoCount(videoCountMap.getOrDefault(category.getId(), 0L))
        );
    }
}
