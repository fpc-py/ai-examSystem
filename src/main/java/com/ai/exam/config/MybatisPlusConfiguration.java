package com.ai.exam.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.ai.exam.mapper")
public class MybatisPlusConfiguration {
}
