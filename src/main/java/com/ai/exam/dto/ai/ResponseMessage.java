package com.ai.exam.dto.ai;

import lombok.Data;

import java.io.Serializable;
@Data
public class ResponseMessage implements Serializable {
 private String role;
 private String content;
 private static final long serialVersionUID = 1L;
}
