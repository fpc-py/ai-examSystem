package com.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question_answers")
public class QuestionAnswer {
    private Long id;
    private Long questionId;
    private String answer;
    private String keywords;
}
