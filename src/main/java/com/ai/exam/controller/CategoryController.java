package com.ai.exam.controller;

import com.ai.exam.common.Result;
import com.ai.exam.entity.Category;
import com.ai.exam.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "分类管理")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @GetMapping
    @Operation(summary = "获取分类列表")
    public Result<List<Category>> getCategories(){
        return Result.success(categoryService.getAllCategories());
    }

    @GetMapping("/tree")
    @Operation(summary = "获取分类树形结构", description = "获取题目分类的树形层级结构，用于前端树形组件展示")
    public Result<List<Category>> getCategoryTree() {
        return Result.success(categoryService.getCategoryTree());
    }

    @PostMapping
    @Operation(summary = "添加新分类", description = "创建新的题目分类，支持设置父分类实现层级结构")
    public Result<Void> addCategory(@RequestBody Category category) {
        categoryService.addCategory(category);
        return Result.success(null);
    }

    @PutMapping
    @Operation(summary = "更新分类信息", description = "修改分类的名称、描述、排序等信息")
    public Result<Void> updateCategory(@RequestBody Category category) {
        categoryService.updateCategory(category);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类", description = "删除指定的题目分类，注意：删除前需确保分类下没有题目")
    public Result<Void> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success(null);
    }
}
