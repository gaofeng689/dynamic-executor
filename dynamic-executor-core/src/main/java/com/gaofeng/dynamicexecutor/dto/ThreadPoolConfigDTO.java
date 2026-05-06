package com.gaofeng.dynamicexecutor.dto;

import lombok.Data;

/**
 * 线程池配置传输对象
 *
 * 包装类型 null 表示该字段不修改，非 null 表示需更新
 */
@Data
public class ThreadPoolConfigDTO {

    /** 核心线程数 */
    private Integer corePoolSize;
    /** 最大线程数 */
    private Integer maximumPoolSize;
    /** 非核心线程存活时间 */
    private Long keepAliveTime;
    /** 存活时间单位 */
    private String timeUnit;
    /** 等待队列类型 */
    private String queueType;
    /** 等待队列容量 */
    private Integer queueCapacity;
    /** 拒绝策略 */
    private String rejectedPolicy;
}
