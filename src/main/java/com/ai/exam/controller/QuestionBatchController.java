package com.ai.exam.controller;

import com.ai.exam.common.Result;
import com.ai.exam.dto.QuestionImportDTO;
import com.ai.exam.service.QuestionService;
import com.ai.exam.utils.ExcelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "题目批量操作", description = "题目批量管理相关操作，包括Excel导入、AI生成题目、批量验证等功能")
@RestController
@RequestMapping("/api/questions/batch")
public class QuestionBatchController {

    @Autowired
    private QuestionService questionService;

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
}
