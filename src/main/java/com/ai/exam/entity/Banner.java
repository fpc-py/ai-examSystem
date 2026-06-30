package com.ai.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

import java.time.LocalDateTime;

@Data
@TableName("banners")
@Schema(description = "轮播图信息")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Banner {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
