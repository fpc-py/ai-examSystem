package com.ai.exam.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class PaperDTO implements Serializable {
    private String name;
    private String description;
    private Integer duration;
    private Map<Integer, BigDecimal> questions;
    private static  final long serialVersionUID = 1L;
}
