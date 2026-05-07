package com.gaofeng.dynamicexecutor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类，注册 SQL 日志拦截器
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public SqlLogInterceptor sqlLogInterceptor() {
        return new SqlLogInterceptor();
    }
}
