package com.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("exams")
@Data
public class Exam {
    private Long id;
    private String name;
    private String description;
    private Integer duration;
    private Integer passScore;
    private Integer questionCount;
    private String status;// 试卷状态：DRAFT(草稿)、PUBLISHED(已发布)、CLOSED(已关闭)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
