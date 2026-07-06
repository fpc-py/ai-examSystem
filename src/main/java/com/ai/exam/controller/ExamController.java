package com.ai.exam.controller;

import com.ai.exam.common.Result;
import com.ai.exam.dto.StartExamDTO;
import com.ai.exam.dto.SubmitAnswerDTO;
import com.ai.exam.entity.ExamRecord;
import com.ai.exam.service.ExamService;
import com.ai.exam.service.PaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@Tag(name = "考试管理", description = "考试流程相关操作，包括开始考试、答题提交、AI批阅、成绩查询等功能")
public class ExamController {
    @Autowired
    private ExamService examService;
    @Autowired
    private PaperService paperService;

    @PostMapping("/start")
    @Operation(summary = "开始考试")
    public Result<ExamRecord> startExam(@RequestBody StartExamDTO dto) {
        // TODO: 从SecurityContext获取当前登录用户ID  // 暂时使用固定用户ID
        Integer userId = 1; // 假设用户ID为1
       ExamRecord examRecord = examService.startExam(dto.getPaperId(),dto.getStudentName());
        return Result.success(examRecord, "考试开始成功");
    }


    @PostMapping("/{examRecordId}/submit")
    @Operation(summary = "提交考试答案", description = "学生提交考试答案，系统记录答题情况")
    public Result<Void> submitAnswers(@PathVariable Integer examRecordId,@RequestBody List<SubmitAnswerDTO> answers){
        examService.submitAnswers(examRecordId,answers);
        return Result.success("答案提交成功");

    }

    @PostMapping("/{examRecordId}/grade")
    @Operation(summary = "AI自动批阅", description = "使用AI技术自动批阅试卷，特别是简答题的智能评分")
    public Result<ExamRecord> gradeExam(@PathVariable Integer examRecordId) {
        ExamRecord examRecord = examService.gradeExam(examRecordId);
        return Result.success(examRecord, "试卷批阅完成");
    }

}
