package com.ai.exam.service;

import com.ai.exam.dto.SubmitAnswerDTO;
import com.ai.exam.entity.ExamRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public interface ExamService extends IService<ExamRecord> {


    ExamRecord startExam(@NotNull(message = "试卷ID不能为空") Integer paperId, @NotBlank(message = "考生姓名不能为空") String studentName);

    void submitAnswers(Integer examRecordId, List<SubmitAnswerDTO> answers);

    ExamRecord gradeExam(Integer examRecordId);

    ExamRecord getExamRecordDetail(Integer id);
}
