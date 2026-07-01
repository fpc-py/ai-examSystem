package com.ai.exam.service;

import com.ai.exam.entity.Category;

import java.util.List;

public interface CategoryService {
    List<Category> getAllCategories();


    List<Category> getCategoryTree();


}
