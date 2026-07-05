package com.ai.exam.service.Impl;

import com.ai.exam.dto.PaperDTO;
import com.ai.exam.entity.Paper;
import com.ai.exam.entity.PaperQuestion;
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
import java.util.List;
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


}
