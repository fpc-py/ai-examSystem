package com.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

@Data
@TableName("categories")
public class Category {
    private Long id;
    private String name;
    private Long parentId;
    private Integer sort;
    @TableField(exist = false)
    private List<Category> children;
    @TableField(exist = false)
    private Long count;
}
