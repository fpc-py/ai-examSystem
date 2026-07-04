package com.ai.exam.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionImportDTO {

    private String title;
    private String type;
    private Boolean multi;
    private Long categoryId;
    private String categoryName;
    private String difficulty;
    private Integer score;
    private String analysis;
    private List<ChoiceImportDTO> choices;
    private String answer;
    private String keywords;


    @Data
    public static class ChoiceImportDTO {
        private String content;
        private Boolean isCorrect;
        private Integer sort;
    }

}


