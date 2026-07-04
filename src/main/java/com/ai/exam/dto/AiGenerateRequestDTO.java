package com.ai.exam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiGenerateRequestDTO {
    @NotBlank(message = "主题不能为空")
    private String topic;
    @Min(value = 1, message = "题目数量至少为1")
    @Max(value = 20, message = "题目数量最多为20")
    private Integer count;
    private String types;
    private String difficulty;
    private Long categoryId;
    private Boolean includeMultiple;
    private String requirements;



}
