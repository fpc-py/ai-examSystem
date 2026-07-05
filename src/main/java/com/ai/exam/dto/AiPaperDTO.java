package com.ai.exam.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiPaperDTO {
    private String name;
    private String description;
    private Integer duration;
    private List<RuleDTO> rules;
}
