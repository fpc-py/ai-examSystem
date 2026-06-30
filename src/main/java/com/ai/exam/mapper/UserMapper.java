package com.ai.exam.mapper;

import com.ai.exam.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;

@MapperScan
public interface UserMapper extends BaseMapper<User> {
}
