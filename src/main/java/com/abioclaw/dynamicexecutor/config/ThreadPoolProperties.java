package com.abioclaw.dynamicexecutor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 线程池配置属性，绑定 application.yml 中 thread.pool.* 配置
 * 
 * @ConfigurationProperties 类型安全配置绑定
 */
@Component
@ConfigurationProperties(prefix = "thread.pool")
@Data
public class ThreadPoolProperties {

    /** 核心线程数 */
    private int corePoolSize = 2;
    /** 最大线程数 */
    private int maximumPoolSize = 4;
    /** 非核心线程存活时间 */
    private long keepAliveTime = 30;
    /** 存活时间单位：SECONDS/MINUTES/HOURS/DAYS */
    private String timeUnit = "SECONDS";
    /** 队列类型：LINKED_BLOCKING/ARRAY_BLOCKING/SYNCHRONOUS/PRIORITY_BLOCKING */
    private String queueType = "LINKED_BLOCKING";
    /** 队列容量 */
    private int queueCapacity = 50;
    /** 拒绝策略：CALLER_RUNS/ABORT/DISCARD/DISCARD_OLDEST */
    private String rejectedPolicy = "CALLER_RUNS";
}
