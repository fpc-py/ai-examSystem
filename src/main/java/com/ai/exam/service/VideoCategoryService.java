package com.ai.exam.service;

import com.ai.exam.entity.VideoCategory;

import java.util.List;

public interface VideoCategoryService {
    List<VideoCategory> getAllCategories();

    List<VideoCategory> getCategoryTree();


}
