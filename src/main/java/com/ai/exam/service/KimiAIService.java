package com.ai.exam.service;

import com.ai.exam.dto.AiGenerateRequestDTO;
import com.ai.exam.dto.QuestionImportDTO;

import java.util.List;

public interface KimiAIService {
    List<QuestionImportDTO> generateQuestions(AiGenerateRequestDTO request);
}
