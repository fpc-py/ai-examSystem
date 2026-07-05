package com.ai.exam.controller;

import com.ai.exam.common.Result;
import com.ai.exam.dto.AiPaperDTO;
import com.ai.exam.dto.PaperDTO;
import com.ai.exam.entity.Paper;
import com.ai.exam.service.PaperService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
@Tag(name = "试卷管理", description = "试卷相关操作，包括创建、查询、更新、删除，以及AI智能组卷功能")
public class PaperController {

    @Autowired
    private PaperService paperService;

    @GetMapping("/list")
    @Operation(summary = "获取试卷列表", description = "支持按名称模糊搜索和状态筛选的试卷列表查询")
    public Result<List<Paper>> listPapers(@RequestParam(required = false) String name,
                                          @RequestParam(required = false) String status){
        QueryWrapper<Paper> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(name)){
            queryWrapper.like("name", name);
        }
        if (StringUtils.hasText(status)){
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByDesc("create_time");
        return  Result.success(paperService.list(queryWrapper));

    }


    @PostMapping
    @Operation(summary = "手动创建试卷", description = "通过手动选择题目的方式创建试卷")
    public Result<Paper>  createPaper(@RequestBody PaperDTO dto){

        Paper paper = paperService.createPaper(dto);
        return Result.success(paper,"试卷创建成功");

    }


    @PutMapping("/{id}")
    @Operation(summary = "更新试卷信息", description = "更新试卷的基本信息和题目配置")
    public Result<Paper> updatePaper(@PathVariable Integer id, @RequestBody PaperDTO dto){
        Paper updatedPaper = paperService.updatePaper(id, dto);
        return Result.success(updatedPaper, "试卷更新成功");
    }

    @PostMapping("/ai")
    @Operation(summary = "AI智能组卷", description = "基于设定的规则（题型分布、难度配比等）使用AI自动生成试卷")
    public Result<Paper> createPaperWithAI(@RequestBody AiPaperDTO dto){
        Paper paper = paperService.createPaperWithAI(dto);
        return Result.success(paper, "AI智能组卷成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取试卷详情", description = "获取试卷的详细信息，包括试卷基本信息和包含的所有题目")
    public Result<Paper> getPaperById(@PathVariable Integer id){
        Paper paper = paperService.getPaperWithQuestions(id);
        return Result.success(paper);
    }


    @PostMapping("/{id}/status")
    @Operation(summary = "更新试卷状态", description = "修改试卷状态：发布试卷供学生考试或停止试卷禁止考试")
    public Result<Void> updatePaperStatus(@PathVariable Integer id, @RequestParam String status){
        paperService.updatePaperStatus(id, status);
        return Result.success(null, "状态更新成功");
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "删除试卷", description = "删除指定的试卷，注意：已发布的试卷不能删除")
    public Result<Void> deletePaper( @PathVariable Integer id){
        Paper paper = paperService.getById(id);
        if (paper == null) {
            return Result.error("试卷不存在");
        }
        if ("PUBLISHED".equals(paper.getStatus())) {
            return Result.error("已发布的试卷不能删除，请先停止发布");
        }
        boolean deleted = paperService.removeById(id);
        if (deleted) {
            return Result.success(null, "试卷删除成功");
        } else {
            return Result.error("试卷删除失败");
        }
    }
}
