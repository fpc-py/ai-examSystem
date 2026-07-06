package com.ai.exam.service.Impl;

import com.ai.exam.dto.SubmitAnswerDTO;
import com.ai.exam.entity.AnswerRecord;
import com.ai.exam.entity.ExamRecord;
import com.ai.exam.entity.Paper;
import com.ai.exam.entity.Question;
import com.ai.exam.mapper.AnswerRecordMapper;
import com.ai.exam.mapper.ExamRecordMapper;
import com.ai.exam.service.ExamService;
import com.ai.exam.service.KimiGradingService;
import com.ai.exam.service.PaperService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
@Slf4j
@Service
public class ExamServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamService {

    @Autowired
    private AnswerRecordMapper answerRecordMapper;
    @Autowired
    private PaperService paperService;
    @Autowired
    private KimiGradingService kimiGradingService;
    @Transactional
    @Override
    public ExamRecord startExam(Integer paperId, String studentName) {
        QueryWrapper<ExamRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("exam_id", paperId);
        wrapper.eq("student_name", studentName);
        wrapper.eq("status","进行中");
        ExamRecord existingRecord = this.getOne(wrapper);
        if (existingRecord != null) {
            return existingRecord;
        }

        ExamRecord examRecord = new ExamRecord();
        examRecord.setExamId(paperId);
        examRecord.setStudentName(studentName);
        examRecord.setStartTime(LocalDateTime.now());
        examRecord.setStatus("进行中");
        examRecord.setScore(0);
        examRecord.setWindowSwitches(0);
        this.save(examRecord);
        return examRecord;
    }

    @Override
    @Transactional
    public void submitAnswers(Integer examRecordId, List<SubmitAnswerDTO> answers) {
        ExamRecord examRecord = this.getById(examRecordId);
        if (examRecord == null || !"进行中".equals(examRecord.getStatus())) {
            throw new RuntimeException("考试记录不存在或已完成");
        }

        answers.forEach(answerDTO->{
            AnswerRecord answerRecord = new AnswerRecord(examRecordId,answerDTO.getQuestionId(), answerDTO.getUserAnswer());
            answerRecordMapper.insert(answerRecord);
        });

        examRecord.setEndTime(LocalDateTime.now());
        examRecord.setStatus("已完成");
        this.updateById(examRecord);

        log.info("试卷提交完成，开始自动AI智能判卷，考试记录ID: {}", examRecordId);
        try {
            this.gradeExam(examRecordId);
            log.info("自动AI判卷完成，考试记录ID: {}", examRecordId);
        } catch (Exception e) {
            log.error("自动AI判卷失败，考试记录ID: {}, 错误: {}", examRecordId, e.getMessage());
            // 判卷失败不影响提交，但记录错误
        }
    }

    @Override
    @Transactional
    public ExamRecord gradeExam(Integer examRecordId) {
        // 1. 获取考试记录和所有答案记录
        ExamRecord examRecord = this.getById(examRecordId);
        if (examRecord == null) {
            throw new RuntimeException("考试记录不存在");
        }

        if (!"已完成".equals(examRecord.getStatus())) {
            throw new RuntimeException("考试尚未完成，无法批阅");
        }

        List<AnswerRecord> answerRecords = answerRecordMapper.selectList(
                new QueryWrapper<AnswerRecord>().eq("exam_record_id", examRecordId));

        if (answerRecords.isEmpty()) {
            log.warn("考试记录ID: {} 没有找到答案记录", examRecordId);
            examRecord.setScore(0);
            examRecord.setStatus("已批阅");
            this.updateById(examRecord);
            return examRecord;
        }
        // 2. 获取试卷中的所有题目及其分值
        Paper paper = paperService.getPaperWithQuestions(examRecord.getExamId());
        if (paper == null || paper.getQuestions() == null) {
            throw new RuntimeException("试卷信息不完整");
        }
        HashMap<Long, Question> questionMap = new HashMap<>();
       paper.getQuestions().forEach(q->questionMap.put(q.getId(), q));

        Integer totalScore = 0;
        Integer correctCount = 0;
        // 3. 使用AI判卷（简答题）或直接判卷（客观题）
        for (AnswerRecord record : answerRecords) {
            Question question = questionMap.get(record.getQuestionId().longValue());
            if (question == null) {
                log.warn("题目ID: {} 在试卷中不存在，跳过", record.getQuestionId());
                continue;
            }
            Integer maxScore = question.getPaperScore() != null ?
                    question.getPaperScore().intValue() : 10;
            try {
                if ("CHOICE".equals(question.getType()) || "JUDGE".equals(question.getType())) {
                    // 客观题：直接对比标准答案
                    String userAnswer = record.getUserAnswer() != null ? record.getUserAnswer().trim() : "";
                    String standardAnswer = question.getAnswer() != null ? question.getAnswer().getAnswer().trim() : "";

                    // 对于判断题，需要处理T/F与TRUE/FALSE的映射
                    if ("JUDGE".equals(question.getType())) {
                        // 统一转换为TRUE/FALSE格式进行比较
                        userAnswer = normalizeJudgeAnswer(userAnswer);
                        standardAnswer = normalizeJudgeAnswer(standardAnswer);
                    }

                    if (userAnswer.equals(standardAnswer)) {
                        // 答案完全正确
                        record.setScore(maxScore);
                        record.setIsCorrect(1); // 完全正确
                        // 选择题和判断题不设置AI评语
                        record.setAiCorrection(null);
                        correctCount++;
                    } else {
                        // 答案错误
                        record.setScore(0);
                        record.setIsCorrect(0); // 错误
                        // 选择题和判断题不设置AI评语
                        record.setAiCorrection(null);
                    }

                    log.info("客观题判卷完成，题目ID: {}, 用户答案: {}, 标准答案: {}, 得分: {}/{}",
                            question.getId(), record.getUserAnswer(), question.getAnswer().getAnswer(), record.getScore(), maxScore);

                } else if ("TEXT".equals(question.getType())) {
                    // 主观题：使用Kimi AI进行智能判卷
                    KimiGradingService.GradingResult gradingResult =
                            kimiGradingService.gradeQuestion(question, record.getUserAnswer(), maxScore);

                    // 更新答案记录
                    record.setScore(gradingResult.getScore());
                    record.setAiCorrection(gradingResult.getFeedback());

                    // 设置正确性标记
                    if (gradingResult.getScore().equals(maxScore)) {
                        record.setIsCorrect(1); // 完全正确
                        correctCount++;
                    } else if (gradingResult.getScore() > 0) {
                        record.setIsCorrect(2); // 部分正确
                    } else {
                        record.setIsCorrect(0); // 错误
                    }

                    log.info("主观题AI判卷完成，题目ID: {}, 得分: {}/{}",
                            question.getId(), gradingResult.getScore(), maxScore);
                }

                totalScore += record.getScore();

            } catch (Exception e) {
                log.error("判卷失败，题目ID: {}, 错误: {}", question.getId(), e.getMessage());
                // 判卷失败时给0分
                record.setScore(0);
                record.setIsCorrect(0);
                record.setAiCorrection("系统判卷失败，请联系管理员");
            }

            // 更新答案记录
            answerRecordMapper.updateById(record);
        }
        // 4. 生成考试总评，使用实际答题数量
        String examSummary = kimiGradingService.generateExamSummary(
                totalScore, paper.getTotalScore().intValue(),
                answerRecords.size(), correctCount);
        // 5. 更新考试记录
        examRecord.setScore(totalScore);
        examRecord.setStatus("已批阅");
        examRecord.setAnswers(examSummary); // 将AI总评存储在answers字段中
        this.updateById(examRecord);

        log.info("AI智能判卷完成，考试记录ID: {}, 总分: {}/{}",
                examRecordId, totalScore, paper.getTotalScore());

        return examRecord;
    }

    @Override
    public ExamRecord getExamRecordDetail(Integer id) {

        ExamRecord examRecord = this.getById(id);
        if (examRecord == null) {
            throw new RuntimeException("考试记录不存在");
        }
        Paper paper = paperService.getPaperWithQuestions(examRecord.getExamId());
        examRecord.setPaper(paper);
        List<AnswerRecord> answerRecords = answerRecordMapper.selectList(
                new QueryWrapper<AnswerRecord>().eq("exam_record_id", id));

        // 新增：按试卷题目顺序排序答题记录，保证前端展示顺序和考试时一致
        if (paper != null && paper.getQuestions() != null && !answerRecords.isEmpty()) {
            List<Long> questionOrder = paper.getQuestions().stream().map(Question::getId).toList();
            answerRecords.sort((a, b) -> {
                int idxA = questionOrder.indexOf(a.getQuestionId().longValue());
                int idxB = questionOrder.indexOf(b.getQuestionId().longValue());
                return Integer.compare(idxA, idxB);
            });
        }
        examRecord.setAnswerRecords(answerRecords);

        return examRecord;
    }

    private String normalizeJudgeAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return "";
        }
        String normalized = answer.trim().toUpperCase();
        switch (normalized) {
            case "T":
            case "TRUE":
            case "正确":
                return "TRUE";
            case "F":
            case "FALSE":
            case "错误":
                return "FALSE";
            default:
                return normalized;
        }
    }
}
