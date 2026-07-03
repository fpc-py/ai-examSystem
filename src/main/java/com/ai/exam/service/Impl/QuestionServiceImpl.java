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

import java.util.*;
import java.util.stream.Collectors;
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

    @Override
    public List<Question> getPopularQuestions(Integer size) {
        if (size <= 0) {
            size = CacheConstants.POPULAR_QUESTIONS_COUNT;
        }
        try {
            if (!redisUtils.hasKey(CacheConstants.QUESTION_VIEW_COUNT_KEY)) {
                refreshPopularQuestionsCache();
            }
            Set<Object> popularQuestionIds = redisUtils.zReverseRange(CacheConstants.QUESTION_VIEW_COUNT_KEY, 0, size - 1);

            if (popularQuestionIds == null || popularQuestionIds.isEmpty()) {
                return getLatestQuestions(size);
            }
            // 将Object类型转换为Long类型
            List<Long> questionIds = popularQuestionIds.stream()
                    .map(id -> {
                        try {
                            return Long.valueOf(id.toString());
                        } catch (NumberFormatException e) {
                            log.warn("题目ID转换失败: {}", id);
                            return null;
                        }
                    })
                    .filter(id -> id != null)
                    .collect(Collectors.toList());

            if (questionIds.isEmpty()) {
                log.warn("转换后的题目ID列表为空，返回最新题目作为备选方案");
                return getLatestQuestions(size);
            }
            List<Question> questions = this.listByIds(questionIds);
            if (questions.isEmpty()) {
                log.warn("根据热门题目ID未找到任何题目，返回最新题目作为备选方案");
                return getLatestQuestions(size);
            }
            fillChoicesAndAnswer(questions);

            HashMap<Long, Double> scoreMap = new HashMap<>();
            for (Long id : questionIds) {
                Double score = redisUtils.zScore(CacheConstants.QUESTION_VIEW_COUNT_KEY, id);
                if (score != null) {
                    scoreMap.put(id, score);
                }
            }

            questions.sort((q1, q2) -> {
                Double score1 = scoreMap.getOrDefault(q1.getId(), 0.0);
                Double score2 = scoreMap.getOrDefault(q2.getId(), 0.0);
                return Double.compare(score2, score1); // 降序排列
            });
            return questions;
        } catch (Exception e) {
            log.error("获取热门题目失败: {}", e.getMessage(), e);
            // 如果Redis出现问题，返回最新的题目作为备选方案
            return getLatestQuestions(size);
        }


    }

    private List<Question> getLatestQuestions(int limit) {
        try {
            log.info("获取最新题目作为备选方案，数量: {}", limit);
            List<Question> latestQuestions = this.list(
                    new QueryWrapper<Question>()
                            .orderByDesc("create_time")
                            .last("LIMIT " + limit)
            );
            fillChoicesAndAnswer(latestQuestions);
            return latestQuestions;
        } catch (Exception e) {
            log.error("获取最新题目失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }


    private void fillChoicesAndAnswer(List<Question> questions) {
        if (questions.isEmpty()) {
            return;
        }


        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toList());

        List<QuestionChoice> allChoices = questionChoiceMapper.selectList(
                new QueryWrapper<QuestionChoice>().in("question_id", questionIds)
        );

        List<QuestionAnswer> allAnswers = questionAnswerMapper.selectList(
                new QueryWrapper<QuestionAnswer>().in("question_id", questionIds)
        );

        Map<Long, List<QuestionChoice>> choicesMap = allChoices.stream()
                .collect(Collectors.groupingBy(QuestionChoice::getQuestionId));

        Map<Long, QuestionAnswer> answersMap = allAnswers.stream()
                .collect(Collectors.toMap(QuestionAnswer::getQuestionId, answer -> answer));

        for (Question question : questions) {
            if ("CHOICE".equals(question.getType())) {
                List<QuestionChoice> choices = choicesMap.getOrDefault(question.getId(), new ArrayList<>());

                choices.sort((c1, c2) -> Integer.compare(
                        c1.getSort() != null ? c1.getSort() : 0,
                        c2.getSort() != null ? c2.getSort() : 0
                ));
                question.setChoices(choices);
            }


            QuestionAnswer answer = answersMap.get(question.getId());
            question.setAnswer(answer);
        }

    }

    private int refreshPopularQuestionsCache() {

        try {
            String viewCountKey = CacheConstants.QUESTION_VIEW_COUNT_KEY;
            redisUtils.delete(viewCountKey);
            List<Question> allQuestions = this.list();
            if (allQuestions.isEmpty()) {
                log.info("没有题目数据，热门题目缓存刷新完成");
                return 0;
            }
            int count = 0;
            for (Question question : allQuestions) {
                Long id = question.getId();
                // 初始计数值，这里使用ID模100作为示例，实际应用可以使用更复杂的算法
                double initialScore = id % 100;
                redisUtils.zAdd(viewCountKey, id, initialScore);
                count++;
            }
            return count;
        } catch (Exception e) {
            return 0;
        }

    }
}
