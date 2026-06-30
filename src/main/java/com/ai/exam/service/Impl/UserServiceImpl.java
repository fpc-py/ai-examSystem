package com.ai.exam.service.Impl;

import com.ai.exam.entity.User;
import com.ai.exam.mapper.UserMapper;
import com.ai.exam.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements UserService {
    @Override
    public User login(String username, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username",username)
                .eq("password",password)
                .eq("status","active");
        User user = this.getOne(wrapper);
        return user;
    }

    @Override
    public boolean isAdmin(Long userId) {
        User user = this.getById(userId);
        return user != null && "admin".equals(user.getRole());

    }
}
