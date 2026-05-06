package com.abioclaw.dynamicexecutor.dto;

import lombok.Data;

/**
 * 线程池配置传输对象。包装类型 null 表示该字段不修改，非 null 表示需更新
 */
@Data
public class ThreadPoolConfigDTO {

    private Integer corePoolSize;
    private Integer maximumPoolSize;
    private Long keepAliveTime;
    private String timeUnit;
    private String queueType;
    private Integer queueCapacity;
    private String rejectedPolicy;
}
