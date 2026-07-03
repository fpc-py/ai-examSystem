package com.ai.exam.service;

import com.ai.exam.entity.Question;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface QuestionService extends IService<Question> {

    Question getQuestionWithDetails(Long id);
    void incrementQuestionViewCount(Long questionId);

    void saveQuestionWithDetails(Question question);

    void updateQuestionWithDetails(Question question);

    List<Question> getPopularQuestions(Integer size);
}
