package com.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question_choices")
public class QuestionChoice {
    private Long id;
    private Long questionId;
    private String content;
    private Boolean isCorrect;
    private Integer sort;
}
