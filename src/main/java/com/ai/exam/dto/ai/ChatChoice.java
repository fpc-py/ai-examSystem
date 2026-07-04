package com.ai.exam.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
@Data
public class ChatChoice implements Serializable {
    private int index;
    private ResponseMessage message;
    @JsonProperty("finish_reason")
    private String finishReason;
    private static final long serialVersionUID = 1L;
}
