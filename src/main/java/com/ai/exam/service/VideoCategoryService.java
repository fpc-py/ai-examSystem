package com.ai.exam.service;

import com.ai.exam.entity.VideoCategory;

import java.util.List;

public interface VideoCategoryService {
    List<VideoCategory> getAllCategories();

    List<VideoCategory> getCategoryTree();


    void deleteCategory(Long id);

    void updateCategory(VideoCategory category);

    void addCategory(VideoCategory category);

    VideoCategory getCategoryById(Long id);

    List<VideoCategory> getChildCategories(Long parentId);

    List<VideoCategory> getTopCategories();


}
