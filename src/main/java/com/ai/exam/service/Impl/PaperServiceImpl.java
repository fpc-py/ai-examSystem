package com.ai.exam.service.Impl;

import com.ai.exam.dto.AiPaperDTO;
import com.ai.exam.dto.PaperDTO;
import com.ai.exam.dto.RuleDTO;
import com.ai.exam.entity.*;
import com.ai.exam.mapper.*;
import com.ai.exam.service.AIService;
import com.ai.exam.service.PaperService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    private static final Logger log = LoggerFactory.getLogger(PaperServiceImpl.class);

    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;
    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;
    @Autowired
    private AIService  aiService;

    @Transactional
    @Override
    public Paper createPaper(PaperDTO dto) {
        Paper paper = new Paper();
        paper.setName(dto.getName());
        paper.setDescription(dto.getDescription());
        paper.setDuration(dto.getDuration());
        paper.setStatus("DRAFT");

        BigDecimal totalScore = dto.getQuestions().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        paper.setTotalScore(totalScore);
        paper.setQuestionCount(dto.getQuestions().size());
        baseMapper.insert(paper);

        List<PaperQuestion> paperQuestions = dto.getQuestions().entrySet().stream()
                .map(entry -> new PaperQuestion(paper.getId(), entry.getKey().longValue(), entry.getValue()))
                .collect(Collectors.toList());

        return paper;
    }

    @Override
    @Transactional
    public Paper updatePaper(Integer paperId, PaperDTO dto) {
        // 1. 更新试卷基本信息
        Paper paper = new Paper();
        paper.setId(paperId);
        paper.setName(dto.getName());
        paper.setDescription(dto.getDescription());
        paper.setDuration(dto.getDuration());

        // 2. 重新计算总分和题目数
        BigDecimal totalScore = dto.getQuestions().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        paper.setTotalScore(totalScore);
        paper.setQuestionCount(dto.getQuestions().size());
        baseMapper.updateById(paper);

        // 3. 删除旧的试卷-题目关联
        paperQuestionMapper.delete(new QueryWrapper<PaperQuestion>().eq("paper_id", paperId));

        // 4. 插入新的试卷-题目关联
        List<PaperQuestion> paperQuestions = dto.getQuestions().entrySet().stream()
                .map(entry -> new PaperQuestion(paperId, entry.getKey().longValue(), entry.getValue()))
                .collect(Collectors.toList());
        paperQuestions.forEach(paperQuestionMapper::insert);

        return paper;
    }

    @Transactional
    @Override
    public Paper createPaperWithAI(AiPaperDTO dto) {
        Paper paper = new Paper();
        paper.setName(dto.getName());
        paper.setDescription(dto.getDescription());
        paper.setDuration(dto.getDuration());
        paper.setStatus("待发布");

        int totalQuestionCount = dto.getRules().stream()
                .mapToInt(rule -> rule.getCount() != null ? rule.getCount() : 0)
                .sum();
        BigDecimal totalScore = dto.getRules().stream()
                .map(rule -> BigDecimal.valueOf(rule.getScore()).multiply(BigDecimal.valueOf(rule.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        paper.setQuestionCount(totalQuestionCount);
        paper.setTotalScore(totalScore);

        baseMapper.insert(paper);

        for (RuleDTO rule : dto.getRules()) {
            if (rule.getCount() == null || rule.getCount() <= 0) {
                log.info("规则 {} 的题目数量为0，已跳过", rule);
                continue;
            }
            QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("type", rule.getType().name());

            // 如果指定了分类，则加入分类查询条件
            if (rule.getCategoryIds() != null && !rule.getCategoryIds().isEmpty()) {
                queryWrapper.in("category_id", rule.getCategoryIds());
            } else {
                // 如果前端没传分类ID，我们应该查询该大类下的所有题目
                // 这需要一个逻辑来找到大类下的所有子分类ID
                log.warn("规则 {} 没有提供具体的分类ID，将在此类型下所有题目中随机选择", rule.getType());
            }
            // 为了避免LIMIT数量大于实际数量的问题，我们先查询ID，再随机选取
            queryWrapper.select("id"); // 只查询ID，提高效率
            List<Long> availableQuestionIds = questionMapper.selectList(queryWrapper)
                    .stream()
                    .map(Question::getId)
                    .collect(Collectors.toList());

            if(availableQuestionIds.isEmpty()){
                log.warn("类型 {} 下没有找到任何题目，跳过此规则", rule.getType());
                continue;
        }
            Collections.shuffle(availableQuestionIds);
            int questionToTake = Math.min(rule.getCount(), availableQuestionIds.size());
            List<Long> selectedQuestionIds = availableQuestionIds.subList(0, questionToTake);

            if(selectedQuestionIds.isEmpty()){
                continue;
            }
            List<Question> questions = questionMapper.selectBatchIds(selectedQuestionIds);

            for (Question question : questions) {
                PaperQuestion pq = new PaperQuestion();
                pq.setPaperId(paper.getId());
                pq.setQuestionId(question.getId());
                pq.setScore(BigDecimal.valueOf(rule.getScore())); // 修正类型
                paperQuestionMapper.insert(pq); // 逐条插入
            }
        }

        return paper;
    }

    @Override
    public Paper getPaperWithQuestions(Integer id) {
        // 1. 查询试卷基本信息
        Paper paper = baseMapper.selectById(id); // 查询试卷
        if (paper == null) {
            return null; // 如果试卷不存在，返回null
        }

        // 2. 查询试卷包含的所有题目ID和分值
        QueryWrapper<PaperQuestion> pqWrapper = new QueryWrapper<>();
        pqWrapper.eq("paper_id", id); // 设置查询条件
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(pqWrapper); // 查询关联记录
        if (paperQuestions.isEmpty()) {
            paper.setQuestions(new ArrayList<>());
            return paper;
        }
        List<Long> questionIds = paperQuestions.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());

        // 3. 根据题目ID查询题目详情
        List<Question> questions = questionMapper.selectBatchIds(questionIds);

        // 4. 优化：批量查询避免N+1问题
        // 4.1 填充分值信息
        Map<Long, BigDecimal> scoreMap = paperQuestions.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, PaperQuestion::getScore));

        // 批量查询选项
        List<QuestionChoice> allChoices = questionChoiceMapper.selectList(
                new QueryWrapper<QuestionChoice>().in("question_id", questionIds)
        );
        Map<Long, List<QuestionChoice>> choicesMap = allChoices.stream()
                .collect(Collectors.groupingBy(QuestionChoice::getQuestionId));

        // 批量查询答案
        List<QuestionAnswer> allAnswers = questionAnswerMapper.selectList(
                new QueryWrapper<QuestionAnswer>().in("question_id", questionIds)
        );
        Map<Long, QuestionAnswer> answersMap = allAnswers.stream()
                .collect(Collectors.toMap(QuestionAnswer::getQuestionId, answer -> answer));

        // 4.3 为每个题目设置信息
        questions.forEach(q -> {
            // 设置分值
            q.setPaperScore(scoreMap.get(q.getId()));

            // 设置选项（仅选择题）
            if ("CHOICE".equals(q.getType())) {
                List<QuestionChoice> choices = choicesMap.getOrDefault(q.getId(), new ArrayList<>());
                // 按sort字段排序
                choices.sort((c1, c2) -> Integer.compare(
                        c1.getSort() != null ? c1.getSort() : 0,
                        c2.getSort() != null ? c2.getSort() : 0
                ));
                q.setChoices(choices);
            }

            // 设置答案
            q.setAnswer(answersMap.get(q.getId()));
        });

        // 5. 按题目类型排序：选择题 -> 判断题 -> 简答题
        questions.sort((q1, q2) -> {
            int order1 = getTypeOrder(q1.getType());
            int order2 = getTypeOrder(q2.getType());
            return Integer.compare(order1, order2);
        });

        paper.setQuestions(questions); // 设置题目列表

        return paper;
    }

    @Override
    public void updatePaperStatus(Integer id, String status) {
        Paper paper = new Paper();
        paper.setId(id);
        paper.setStatus(status);
        this.updateById(paper);
    }

    private int getTypeOrder(String type) {
        switch (type) {
            case "CHOICE": return 1; // 选择题
            case "JUDGE": return 2;  // 判断题
            case "TEXT": return 3;   // 简答题
            default: return 4;       // 其他类型
        }
    }


}
