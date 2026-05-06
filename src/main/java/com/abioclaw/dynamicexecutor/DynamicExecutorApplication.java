package com.abioclaw.dynamicexecutor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 */
@SpringBootApplication
public class DynamicExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicExecutorApplication.class, args);
    }
}
