package com.ai.exam.controller;

import com.ai.exam.common.Result;
import com.ai.exam.dto.AiGenerateRequestDTO;
import com.ai.exam.dto.QuestionImportDTO;
import com.ai.exam.service.KimiAIService;
import com.ai.exam.service.QuestionService;
import com.ai.exam.utils.ExcelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@Slf4j
@Tag(name = "题目批量操作", description = "题目批量管理相关操作，包括Excel导入、AI生成题目、批量验证等功能")
@RestController
@RequestMapping("/api/questions/batch")
public class QuestionBatchController {

    @Autowired
    private QuestionService questionService;
    @Autowired
    private KimiAIService kimiAIService;

    @GetMapping("/template")
    @Operation(summary = "下载Excel导入模板", description = "下载题目批量导入的Excel模板文件")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try {
            byte[] template = ExcelUtil.generateTemplate();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=question_import_template.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(template);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    @PostMapping("/preview-excel")
    @Operation(summary = "预览Excel文件内容", description = "解析并预览Excel文件中的题目内容，不会导入到数据库")
    public Result<List<QuestionImportDTO>> previewExcel(@RequestParam("file")MultipartFile file) throws IOException {
        try {
            if (file.isEmpty()){
                return Result.error("文件不能为空");
            }
            String fileName = file.getOriginalFilename();
            if (fileName == null ||(!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                return Result.error("请上传Excel文件（.xlsx或.xls格式）");
            }
            List<QuestionImportDTO> questions = ExcelUtil.parseExcel(file);

            if (questions.isEmpty()){
                return Result.error("Excel文件中没有有效的题目数据");
            }
            return Result.success(questions);
        } catch (IOException e) {
            return Result.error("解析Excel文件失败: " + e.getMessage());
        }
    }


    @PostMapping("/import-excel")
    @Operation(summary = "从Excel文件批量导入题目", description = "解析Excel文件并将题目批量导入到数据库")
    public Result<String> importFromExcel(@RequestParam("file")MultipartFile file) throws IOException {
        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                return Result.error("请上传Excel文件（.xlsx或.xls格式）");
            }

            List<QuestionImportDTO> questions = ExcelUtil.parseExcel(file);

            if (questions.isEmpty()) {
                return Result.error("Excel文件中没有有效的题目数据");
            }

            int successCount = questionService.batchImportQuestions(questions);

            String message = String.format("Excel导入完成！成功导入 %d / %d 道题目", successCount, questions.size());

            return Result.success(message);

        } catch (Exception e) {
            return Result.error("Excel批量导入失败: " + e.getMessage());
        }
    }


    @PostMapping("/ai-generate")
    @Operation(summary = "AI智能生成题目", description = "使用AI技术根据指定主题和要求智能生成题目，支持预览后再决定是否导入")
    public Result<List<QuestionImportDTO>>  generateQuestiionsByAi(@RequestBody @Validated AiGenerateRequestDTO request){
        try {
            // 调用AI服务生成题目
            List<QuestionImportDTO> questions = kimiAIService.generateQuestions(request);

            if (questions.isEmpty()) {
                return Result.error("AI未能生成题目，请检查参数或稍后重试");
            }

            log.info("AI成功生成{}道关于【{}】的题目", questions.size(), request.getTopic());
            return Result.success(questions);

        } catch (Exception e) {
            log.error("AI生成题目失败", e);
            return Result.error("AI生成题目失败: " + e.getMessage());
        }
    }


    @PostMapping("/import-questions") 
    @Operation(summary = "批量导入题目", description = "将题目列表批量导入到数据库，支持Excel解析后的导入或AI生成后的确认导入")
public Result<String> importQuestions(@RequestBody List<QuestionImportDTO> questions){
        try {
            if (questions == null || questions.isEmpty()) {
                return Result.error("题目列表不能为空");
            }
            int successCount = questionService.batchImportQuestions(questions);
            String message = String.format("批量导入完成！成功导入 %d / %d 道题目", successCount, questions.size());
            log.info(message);

            return Result.success(message);
        } catch (Exception e) {
            log.error("批量导入题目失败", e);
            return Result.error("批量导入题目失败: " + e.getMessage());
        }
    }

    @PostMapping("/validate") 
    @Operation(summary = "验证题目数据", description = "验证题目数据的完整性和格式正确性，返回验证结果和错误信息")
    public Result<String> validateQuestions(@RequestBody List<QuestionImportDTO> questions) {
        try {
            if (questions == null || questions.isEmpty()) {
                return Result.error("题目列表不能为空");
            }

            int validCount = 0;
            StringBuilder errors = new StringBuilder();

            for (int i = 0; i < questions.size(); i++) {
                QuestionImportDTO question = questions.get(i);
                String error = validateSingleQuestion(question, i + 1);
                if (error == null) {
                    validCount++;
                } else {
                    errors.append(error).append("\n");
                }
            }

            if (validCount == questions.size()) {
                return Result.success("所有题目数据验证通过");
            } else {
                return Result.error("存在无效题目数据：\n" + errors.toString());
            }

        } catch (Exception e) {
            log.error("验证题目数据失败", e);
            return Result.error("验证题目数据失败: " + e.getMessage());
        }
    }
    /**
     * 验证单个题目数据
     * @param question 题目数据
     * @param index 题目序号
     * @return 错误信息，如果为null表示验证通过
     */
    private String validateSingleQuestion(QuestionImportDTO question, int index) {
        // 验证基本字段
        if (question.getTitle() == null || question.getTitle().trim().isEmpty()) {
            return String.format("第%d题：题目内容不能为空", index);
        }

        if (question.getType() == null || question.getType().trim().isEmpty()) {
            return String.format("第%d题：题目类型不能为空", index);
        }

        if (!"CHOICE".equals(question.getType()) && !"JUDGE".equals(question.getType()) && !"TEXT".equals(question.getType())) {
            return String.format("第%d题：题目类型必须是CHOICE、JUDGE或TEXT", index);
        }

        // 验证选择题特有字段
        if ("CHOICE".equals(question.getType())) {
            if (question.getChoices() == null || question.getChoices().isEmpty()) {
                return String.format("第%d题：选择题必须有选项", index);
            }

            if (question.getChoices().size() < 2) {
                return String.format("第%d题：选择题至少需要2个选项", index);
            }

            boolean hasCorrectAnswer = question.getChoices().stream()
                    .anyMatch(choice -> choice.getIsCorrect() != null && choice.getIsCorrect());

            if (!hasCorrectAnswer) {
                return String.format("第%d题：选择题必须有正确答案", index);
            }
        } else {
            // 判断题和简答题需要答案
            if (question.getAnswer() == null || question.getAnswer().trim().isEmpty()) {
                return String.format("第%d题：%s必须有答案", index,
                        "JUDGE".equals(question.getType()) ? "判断题" : "简答题");
            }
        }

        return null; // 验证通过
    }
} 
