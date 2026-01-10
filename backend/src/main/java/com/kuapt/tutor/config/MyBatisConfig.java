package com.kuapt.tutor.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.kuapt.tutor.mapper")
public class MyBatisConfig {}
