package com.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@NoArgsConstructor
public class AnswerRecord implements Serializable {
    private Integer id;
    private Integer examRecordId;
    private Integer questionId;
    private String userAnswer;
    private Integer score;
    private Integer isCorrect; // 是否正确 (0: 错误, 1: 正确, 2: 部分正确)
    private String aiCorrection; // AI批改意见
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public AnswerRecord(Integer examRecordId, Integer questionId, String userAnswer) {
        this.examRecordId = examRecordId;
        this.questionId = questionId;
        this.userAnswer = userAnswer;
    }
}
