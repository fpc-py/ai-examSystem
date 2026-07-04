package com.ai.exam.dto.ai;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ChatResponse implements Serializable {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<ChatChoice> choices;
    private Usage usage;
    private static final long serialVersionUID = 1L;
}
