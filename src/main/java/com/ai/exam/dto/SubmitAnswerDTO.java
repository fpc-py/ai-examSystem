package com.ai.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class SubmitAnswerDTO implements Serializable {

    @NotNull(message = "题目ID不能为空")
    private Integer questionId;
    private String userAnswer;
    private static final long serialVersionUID = 1L;
}
