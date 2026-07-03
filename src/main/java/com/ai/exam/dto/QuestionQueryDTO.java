package com.ai.exam.dto;


import lombok.Data;



@Data
public class QuestionQueryDTO {

    private Integer page;
    private Integer size;
    private String categoryId;
    private String difficulty;
    private String type;
    private String keyword;
}
