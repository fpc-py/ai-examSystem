package com.ai.exam.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class ExamRankingVO implements Serializable {

    private Integer id;
    private String studentName;
    private Integer score;
    private Integer examId;
    private String paperName;
    private BigDecimal paperTotalScore;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;

    public Map<String,Object> getPaper(){
        HashMap<String, Object> paper = new HashMap<>();
        paper.put("examId", examId);
        paper.put("paperName", paperName);
        paper.put("paperTotalScore", paperTotalScore);
        return paper;
    }

    private static final long serialVersionUID = 1L;
}
