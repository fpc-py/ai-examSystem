package com.ai.exam.controller;

import com.ai.exam.dto.LoginRequest;
import com.ai.exam.dto.LoginResponse;
import com.ai.exam.entity.User;
import com.ai.exam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ai.exam.common.Result;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@Tag(name = "用户管理",description = "用户相关操作，包括登录认证、权限验证等功能" )
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponse> login(@RequestBody LoginRequest loginRequest){
        if (loginRequest.getUsername() == null || loginRequest.getUsername().isEmpty()){
            return Result.error("用户名不能为空");
        }
        if (loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()){
            return Result.error("密码不能为空");
        }

        User user = userService.login(loginRequest.getUsername(),loginRequest.getPassword());
        if (user == null){
            return Result.error("用户名或密码错误");
        }

        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        response.setToken(UUID.randomUUID().toString());

        return Result.success(response);
    }

    @GetMapping("/check-admin/{userId}")
    @Operation(summary = "检查管理员权限", description = "验证指定用户是否具有管理员权限")
    public Result<Boolean> checkAdmin(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        boolean isAdmin = userService.isAdmin(userId);
        return Result.success(isAdmin);
    }
}
