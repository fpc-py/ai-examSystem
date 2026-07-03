package com.ai.exam.service.Impl;

import com.ai.exam.common.CacheConstants;
import com.ai.exam.entity.Question;
import com.ai.exam.entity.QuestionAnswer;
import com.ai.exam.entity.QuestionChoice;
import com.ai.exam.mapper.QuestionAnswerMapper;
import com.ai.exam.mapper.QuestionChoiceMapper;
import com.ai.exam.mapper.QuestionMapper;
import com.ai.exam.service.QuestionService;
import com.ai.exam.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.IntStream;
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;
    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;
    @Autowired
    private RedisUtils redisUtils;

    @Override
    @Cacheable(value = CacheConstants.QUESTION_CACHE, key = "'detail:' + #id", unless = "#result == null")
    public Question getQuestionWithDetails(Long id) {
        Question question = this.getById(id);
        if (question == null) return null;

        if ("CHOICE".equals(question.getType())) {

            List<QuestionChoice> choices = questionChoiceMapper.selectList(
                    new QueryWrapper<QuestionChoice>().eq("question_id", id)
            );
            question.setChoices(choices);
        }

        QuestionAnswer answer = questionAnswerMapper.selectOne(
                new QueryWrapper<QuestionAnswer>().eq("question_id", id)
        );
        question.setAnswer(answer);

        if ("CHOICE".equals(question.getType()) && question.getAnswer() != null && !CollectionUtils.isEmpty(question.getChoices())) {
            String[] correctAnswers = question.getAnswer().getAnswer().split(",");
            List<String> correctList = List.of(correctAnswers);

            IntStream.range(0, question.getChoices().size()).forEach(i -> {
                String optionLabel = String.valueOf((char) ('A' + i));
                if (correctList.contains(optionLabel)) {
                    question.getChoices().get(i).setIsCorrect(true);
                }
            });
        }
        return question;
    }

    @Async
    @Override
    public void incrementQuestionViewCount(Long questionId) {
        try {
            if (!this.getBaseMapper().exists(new QueryWrapper<Question>().eq("id", questionId))) {
                log.warn("尝试增加不存在题目的访问计数，题目ID: {}", questionId);
                return;
            }
            Double newScore = redisUtils.zIncrementScore(CacheConstants.QUESTION_VIEW_COUNT_KEY, questionId, 1);
            log.info("题目访问计数增加，题目ID: {}, 当前计数: {}", questionId, newScore.intValue());
        } catch (Exception e) {
            log.error("增加题目访问计数失败，题目ID: {}", questionId, e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConstants.QUESTION_CACHE, key = "'category:' + #question.categoryId", condition = "#question.categoryId != null")
    public void saveQuestionWithDetails(Question question) {
        this.save(question);
        Long questionId = question.getId();

        if ("CHOICE".equals(question.getType())) {
            List<QuestionChoice> choices = question.getChoices();
            if (!CollectionUtils.isEmpty(choices)) {
                StringBuilder correctAnswer = new StringBuilder();
                for (int i = 0; i < choices.size(); i++) {
                    QuestionChoice choice = choices.get(i);
                    choice.setQuestionId(questionId);
                    questionChoiceMapper.insert(choice);

                    if (choice.getIsCorrect() != null && choice.getIsCorrect()) {
                        if (correctAnswer.length() > 0) {
                            correctAnswer.append(",");
                        }
                        correctAnswer.append((char) ('A' + i));
                    }
                }
                if (correctAnswer.length() > 0) {
                    QuestionAnswer answer = new QuestionAnswer();
                    answer.setQuestionId(questionId);
                    answer.setAnswer(correctAnswer.toString());
                    questionAnswerMapper.insert(answer);
                }

            }
        } else {
            QuestionAnswer answer = question.getAnswer();
            if (answer != null) {
                answer.setQuestionId(questionId);
                questionAnswerMapper.insert(answer);
            }
        }
    }


    @Override
    public void updateQuestionWithDetails(Question question) {
        this.updateById(question);
        Long questionId = question.getId();
        questionAnswerMapper.delete(new QueryWrapper<QuestionAnswer>().eq("question_id", questionId));
        questionChoiceMapper.delete(new QueryWrapper<QuestionChoice>().eq("question_id", questionId));
        if ("CHOICE".equals(question.getType())) {
            List<QuestionChoice> choices = question.getChoices();
            if (!CollectionUtils.isEmpty(choices)) {
                StringBuilder correctAnswer = new StringBuilder();
                for (int i = 0; i < choices.size(); i++) {
                    QuestionChoice choice = choices.get(i);
                    choice.setQuestionId(questionId);
                    questionChoiceMapper.insert(choice);

                    if (choice.getIsCorrect() != null && choice.getIsCorrect()) {
                        if (correctAnswer.length() > 0) {
                            correctAnswer.append(",");
                        }
                        correctAnswer.append((char) ('A' + i));
                    }
                }
                if (correctAnswer.length() > 0) {
                    QuestionAnswer answer = new QuestionAnswer();
                    answer.setQuestionId(questionId);
                    answer.setAnswer(correctAnswer.toString());
                    questionAnswerMapper.insert(answer);
                }

            }
        } else {
            QuestionAnswer answer = question.getAnswer();
            if (answer != null) {
                answer.setQuestionId(questionId);
                questionAnswerMapper.insert(answer);
            }
        }
    }
}
