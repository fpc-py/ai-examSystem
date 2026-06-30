package com.ai.exam.service;

import com.ai.exam.entity.User;
import jakarta.validation.constraints.NotBlank;

public interface UserService {
    User login(@NotBlank(message = "用户名不能为空") String username, @NotBlank(message = "密码不能为空") String password);

    boolean isAdmin(Long userId);
}
