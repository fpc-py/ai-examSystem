package com.ai.exam.dto.ai;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class ChatRequest implements Serializable {
    private String model;
    private List<ChatMessage> messages;
    private boolean stream = false;
    private double temperature = 0.3;
    private Integer maxTokens;
    private static final long serialVersionUID = 1L;
}
