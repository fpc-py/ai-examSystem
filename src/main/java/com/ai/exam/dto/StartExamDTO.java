package com.ai.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartExamDTO {
    @NotNull(message = "试卷ID不能为空")
    private Integer paperId;
    @NotBlank(message = "考生姓名不能为空")
    private String studentName;
}
