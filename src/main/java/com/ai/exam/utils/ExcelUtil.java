package com.ai.exam.utils;

import com.ai.exam.dto.QuestionImportDTO;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel文件处理工具类
 * 用于解析题目导入的Excel文件
 */
public class ExcelUtil {

    public static List<QuestionImportDTO> parseExcel(MultipartFile file) throws IOException {
        List<QuestionImportDTO> questions = new ArrayList<>();

        InputStream inputStream = file.getInputStream();
        Workbook workbook = null;
        try {
            String fileName = file.getOriginalFilename();
            if (fileName!= null && fileName.endsWith(".xlsx")) {
               workbook = new XSSFWorkbook(inputStream); // Excel 2007+
            }else {
                workbook = new HSSFWorkbook(inputStream);// Excel 97-2003
            }
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                QuestionImportDTO question = new QuestionImportDTO();
                question.setTitle(row.getCell(0).getStringCellValue());
                question.setType(row.getCell(1).getStringCellValue());
                question.setMulti("是".equals(row.getCell(2).getStringCellValue()) || "true".equals(row.getCell(2).getStringCellValue()));

                String categoryIdStr = row.getCell(3).getStringCellValue();
                if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
                    try {
                        question.setCategoryId(Long.parseLong(categoryIdStr));
                    } catch (NumberFormatException e) {
                        question.setCategoryId(1L);
                    }

                }
                question.setDifficulty(row.getCell(4).getStringCellValue());

                String scoreStr = row.getCell(5).getStringCellValue();
                if (scoreStr != null && !scoreStr.isEmpty()) {
                    try {
                        question.setScore(Integer.parseInt(scoreStr));
                    } catch (NumberFormatException e) {
                        question.setScore(5);
                    }
                }
                if ("CHOICE".equals(question.getType())) {
                    List<QuestionImportDTO.ChoiceImportDTO> choices = new ArrayList<>();
                    String correctAnswer = row.getCell(10).getStringCellValue();

                    for (int j = 0; j<4;j++){
                        String optionContent = row.getCell(6 + j).getStringCellValue();
                        if (optionContent != null && !optionContent.trim().isEmpty()) {
                            QuestionImportDTO.ChoiceImportDTO choice = new QuestionImportDTO.ChoiceImportDTO();
                            choice.setContent(optionContent);
                            choice.setSort(j+1);

                            char optionLabel = (char) ('A' + j);
                            boolean isCorrect = correctAnswer != null && correctAnswer.contains(String.valueOf(optionLabel));
                            choice.setIsCorrect(isCorrect);

                            choices.add(choice);
                        }
                    }
                    question.setChoices(choices);
                }else {
                    question.setAnswer(row.getCell(10).getStringCellValue());
                }
                question.setAnalysis(row.getCell(11).getStringCellValue());
                if (question.getTitle() != null && !question.getTitle().trim().isEmpty() &&
                        question.getType() != null && !question.getType().trim().isEmpty()) {
                    questions.add(question);
                }
            }
        } finally {
            if (workbook != null) {
                workbook.close();
            }
            inputStream.close();
        }
        return questions;
    }



    private static String getCellValue(Cell cell){
        if (cell == null){
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return String.valueOf(cell.getCellFormula());
            default:
                return null;
        }

    }


    public static byte[] generateTemplate()throws IOException{
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("题目导入模板");

        Row headerRow = sheet.createRow(0);
        String [] headers = {
                "题目内容", "题目类型", "是否多选", "分类ID", "难度", "分值",
                "选项A", "选项B", "选项C", "选项D", "正确答案", "解析"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // 创建示例数据行
        Row exampleRow = sheet.createRow(1);
        exampleRow.createCell(0).setCellValue("以下哪个是Spring框架的核心特性？");
        exampleRow.createCell(1).setCellValue("CHOICE");
        exampleRow.createCell(2).setCellValue("否");
        exampleRow.createCell(3).setCellValue("1");
        exampleRow.createCell(4).setCellValue("MEDIUM");
        exampleRow.createCell(5).setCellValue("5");
        exampleRow.createCell(6).setCellValue("依赖注入");
        exampleRow.createCell(7).setCellValue("面向切面编程");
        exampleRow.createCell(8).setCellValue("事务管理");
        exampleRow.createCell(9).setCellValue("以上都是");
        exampleRow.createCell(10).setCellValue("D");
        exampleRow.createCell(11).setCellValue("Spring框架的核心特性包括依赖注入、面向切面编程和事务管理等。");

        for (int i =0; i<headers.length;i++){
            sheet.autoSizeColumn(i);
        }
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            workbook.write(out);
            workbook.close();
            return out.toByteArray();
        }
    }
}
