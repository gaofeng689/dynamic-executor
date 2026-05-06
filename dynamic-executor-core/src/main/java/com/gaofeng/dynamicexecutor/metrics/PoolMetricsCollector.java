package com.gaofeng.dynamicexecutor.metrics;

import com.gaofeng.dynamicexecutor.util.ThreadPoolManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 指标采集器。定时采集线程池快照写入 MySQL，并清理超期数据
 */
@Service
@RequiredArgsConstructor
public class PoolMetricsCollector {

    private final ThreadPoolManager threadPoolManager;
    private final PoolSnapshotMapper mapper;

    /** 数据保留时长（小时） */
    @Value("${metrics.retention-hours:24}")
    private int retentionHours;

    /** 定时采集线程池指标，写入 de_pool_snapshot 表 */
    @Scheduled(fixedDelayString = "${metrics.collection-interval-seconds:10}000")
    public void collect() {
        ThreadPoolExecutor executor = threadPoolManager.getExecutor();
        PoolSnapshot snapshot = new PoolSnapshot();
        snapshot.setCorePoolSize(executor.getCorePoolSize());
        snapshot.setMaximumPoolSize(executor.getMaximumPoolSize());
        snapshot.setPoolSize(executor.getPoolSize());
        snapshot.setActiveCount(executor.getActiveCount());
        snapshot.setCompletedTaskCount(executor.getCompletedTaskCount());
        snapshot.setTaskCount(executor.getTaskCount());
        snapshot.setQueueSize(executor.getQueue().size());
        int remaining = executor.getQueue().remainingCapacity();
        snapshot.setQueueRemainingCapacity(remaining == Integer.MAX_VALUE ? -1 : remaining);
        snapshot.setQueueType(executor.getQueue().getClass().getSimpleName());
        snapshot.setRejectedHandler(executor.getRejectedExecutionHandler().getClass().getSimpleName());
        snapshot.setKeepAliveTime(executor.getKeepAliveTime(java.util.concurrent.TimeUnit.SECONDS));
        snapshot.setCreateTime(LocalDateTime.now());
        mapper.insert(snapshot);
    }

    /** 每小时清理超过 retention-hours 的旧数据 */
    @Scheduled(fixedRate = 3600000)
    public void cleanup() {
        mapper.deleteOlderThan(retentionHours);
    }
}
