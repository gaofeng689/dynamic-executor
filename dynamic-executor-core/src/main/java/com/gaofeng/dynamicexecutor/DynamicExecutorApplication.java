package com.gaofeng.dynamicexecutor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DynamicExecutor 启动类
 *
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration
 *                        + @ComponentScan
 * @EnableScheduling 启用定时任务（指标采集、自动伸缩）
 * @MapperScan 扫描 MyBatis-Plus Mapper 接口
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.gaofeng.dynamicexecutor.metrics")
public class DynamicExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicExecutorApplication.class, args);
    }
}
