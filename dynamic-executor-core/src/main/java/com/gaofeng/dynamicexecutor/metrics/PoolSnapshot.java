package com.gaofeng.dynamicexecutor.metrics;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线程池快照实体，映射 MySQL 表 de_pool_snapshot
 */
@Data
@TableName("de_pool_snapshot")
public class PoolSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 核心线程数 */
    private Integer corePoolSize;
    /** 最大线程数 */
    private Integer maximumPoolSize;
    /** 当前线程数 */
    private Integer poolSize;
    /** 活跃线程数 */
    private Integer activeCount;
    /** 已完成任务数 */
    private Long completedTaskCount;
    /** 总任务数 */
    private Long taskCount;
    /** 队列大小 */
    private Integer queueSize;
    /** 队列剩余容量（-1 表示无界） */
    private Integer queueRemainingCapacity;
    /** 队列类型 */
    private String queueType;
    /** 拒绝策略 */
    private String rejectedHandler;
    /** 存活时间（秒） */
    private Long keepAliveTime;
    /** 采集时间 */
    private LocalDateTime createTime;
}
