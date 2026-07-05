package com.ai.exam.dto;

import com.ai.exam.entity.QuestionType;
import lombok.Data;

import java.util.List;

@Data
public class RuleDTO {
    private QuestionType type;
    private List<Integer> categoryIds;
    private Integer count;
    private Integer score;
}
