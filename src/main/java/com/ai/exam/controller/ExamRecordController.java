package com.ai.exam.controller;

import com.ai.exam.common.Result;
import com.ai.exam.entity.ExamRecord;
import com.ai.exam.service.ExamRecordService;
import com.ai.exam.service.PaperService;
import com.ai.exam.vo.ExamRankingVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/exam-records")
@Tag(name = "考试记录管理", description = "考试记录相关操作，包括记录查询、成绩管理、排行榜展示等功能")
public class ExamRecordController {
    @Autowired
    private ExamRecordService examRecordService;
    @Autowired
    private PaperService paperService;

    @GetMapping("/list")
    @Operation(summary = "分页查询考试记录", description = "支持多条件筛选的考试记录分页查询，包括按姓名、状态、时间范围等筛选")
    public Result<Page<ExamRecord>> getExamRecords(@RequestParam(defaultValue = "1") Integer page,@RequestParam(defaultValue = "20") Integer size,
                                                   @RequestParam(required = false) String studentName,@RequestParam(required = false) String studentNumber,
                                                   @RequestParam(required = false) Integer status, @RequestParam(required = false) String startDate,
                                                   @RequestParam(required = false) String endDate){
        QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();

        if (studentName != null && !studentName.trim().isEmpty()) {
            wrapper.like("student_name", studentName.trim());
        }
        // 按学号搜索（如果有学号字段的话）
        if (studentNumber != null && !studentNumber.trim().isEmpty()) {
            // 暂时注释掉，因为实体类中没有学号字段
            // wrapper.like("student_number", studentNumber.trim());
        }
        if (status != null) {
            String statusStr;
            switch (status) {
                case 0:
                    statusStr = "进行中";
                    break;
                case 1:
                    statusStr = "已完成";
                    break;
                case 2:
                    statusStr = "已批阅";
                    break;
                default:
                    statusStr = "进行中";
            }
            wrapper.eq("status", statusStr);
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            LocalDate start = LocalDate.parse(startDate);
            LocalDateTime startDateTime = start.atStartOfDay();
            wrapper.ge("create_time", startDateTime);
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            LocalDate end = LocalDate.parse(endDate);
            LocalDateTime endDateTime = end.atTime(23, 59, 59);
            wrapper.le("create_time", endDateTime);
        }
        wrapper.orderByDesc("create_time");

        Page<ExamRecord> pageParam = new Page<>(page, size);
        Page<ExamRecord> result = examRecordService.page(pageParam, wrapper);

        result.getRecords().forEach(record -> {
            record.setPaper(paperService.getPaperWithQuestions(record.getExamId()));
        });

        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取考试记录详情", description = "根据记录ID获取考试记录的详细信息，包括试卷内容和答题情况")
    public Result<ExamRecord> getExamRecordById(
            @Parameter(description = "考试记录ID") @PathVariable Integer id) {
        ExamRecord record = examRecordService.getById(id);
        if (record != null) {
            // 加载试卷信息
            record.setPaper(paperService.getPaperWithQuestions(record.getExamId()));
        }
        return Result.success(record);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除考试记录", description = "根据ID删除指定的考试记录")
    public Result<Void> deleteExamRecord(
            @Parameter(description = "考试记录ID") @PathVariable Integer id) {
        boolean success = examRecordService.removeById(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    @GetMapping("/ranking")
    @Operation(summary = "获取考试排行榜", description = "获取考试成绩排行榜，支持按试卷筛选和限制显示数量，使用优化的SQL关联查询提升性能")
    public Result<List<ExamRankingVO>> getExamRanking( @RequestParam(required = false) Integer paperId, @RequestParam(required = false) Integer limit){
        List<ExamRankingVO> rankingList = examRecordService.getExamRankingOptimized(paperId, limit);

        return Result.success(rankingList);
    }
}
