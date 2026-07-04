package com.ai.exam.service;

import com.ai.exam.dto.ai.ChatMessage;

import java.util.List;

public interface AIService {
    String getChatCompletion(List<ChatMessage> messages);
}
