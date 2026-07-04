package com.ai.exam.dto.ai;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ChatMessage implements Serializable {
    private String role;
    private String content;
    private static final long serialVersionUID = 1L;
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
