package com.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@TableName("questions")
public class Question {
    private Long id;
    private String title;
    private String type;
    private Boolean multi;
    private Long categoryId;
    private String difficulty;
    private Integer score;
    @TableField(exist = false)
    private BigDecimal paperScore;
    private String analysis;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
    @TableField(exist = false)
    private List<QuestionChoice> choices;
    @TableField(exist = false)
    private QuestionAnswer answer;
    @TableField(exist = false)
    private Category category;


}
