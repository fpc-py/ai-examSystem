package com.ai.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登陆成功响应数据")
public class LoginResponse {
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String token;
}
