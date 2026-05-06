package com.gaofeng.dynamicexecutor.util;

import com.gaofeng.dynamicexecutor.config.ThreadPoolProperties;
import com.gaofeng.dynamicexecutor.dto.ThreadPoolConfigDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * 线程池管理器，支持运行时动态重建线程池
 *
 * 关键技术：
 * - volatile 保证 executor 引用多线程可见性
 * - synchronized 保证 applyConfig 原子性
 * - 每次配置变更都更新 Properties 再重建线程池
 */
@Service
@Getter
public class ThreadPoolManager {

    /** volatile 保证重建后其他线程立即可见新实例 */
    private volatile ThreadPoolExecutor executor;
    private final ThreadPoolProperties properties;

    public ThreadPoolManager(ThreadPoolProperties properties) {
        this.properties = properties;
    }

    /** 启动时根据 application.yml 初始化线程池 */
    @PostConstruct
    public void init() {
        this.executor = createExecutor(properties);
    }

    /**
     * 应用新配置。先校验 corePoolSize <= maximumPoolSize，再更新属性并重建线程池
     */
    public synchronized void applyConfig(ThreadPoolConfigDTO dto) {
        int newCore = dto.getCorePoolSize() != null ? dto.getCorePoolSize() : properties.getCorePoolSize();
        int newMax = dto.getMaximumPoolSize() != null ? dto.getMaximumPoolSize() : properties.getMaximumPoolSize();
        if (newCore > newMax) {
            throw new IllegalArgumentException("核心线程数(" + newCore + ")不能大于最大线程数(" + newMax + ")");
        }
        updateProperties(dto);
        recreateExecutor();
    }

    /** 获取当前线程池配置 */
    public ThreadPoolConfigDTO getCurrentConfig() {
        ThreadPoolConfigDTO dto = new ThreadPoolConfigDTO();
        dto.setCorePoolSize(properties.getCorePoolSize());
        dto.setMaximumPoolSize(properties.getMaximumPoolSize());
        dto.setKeepAliveTime(properties.getKeepAliveTime());
        dto.setTimeUnit(properties.getTimeUnit());
        dto.setQueueType(properties.getQueueType());
        dto.setQueueCapacity(properties.getQueueCapacity());
        dto.setRejectedPolicy(properties.getRejectedPolicy());
        return dto;
    }

    /** 仅更新 DTO 中非 null 的字段 */
    private void updateProperties(ThreadPoolConfigDTO dto) {
        if (dto.getCorePoolSize() != null) {
            properties.setCorePoolSize(dto.getCorePoolSize());
        }
        if (dto.getMaximumPoolSize() != null) {
            properties.setMaximumPoolSize(dto.getMaximumPoolSize());
        }
        if (dto.getKeepAliveTime() != null) {
            properties.setKeepAliveTime(dto.getKeepAliveTime());
        }
        if (dto.getTimeUnit() != null) {
            properties.setTimeUnit(dto.getTimeUnit());
        }
        if (dto.getQueueType() != null) {
            properties.setQueueType(dto.getQueueType());
        }
        if (dto.getQueueCapacity() != null) {
            properties.setQueueCapacity(dto.getQueueCapacity());
        }
        if (dto.getRejectedPolicy() != null) {
            properties.setRejectedPolicy(dto.getRejectedPolicy());
        }
    }

    /** 用 shutdown() 优雅关闭旧池，不会丢失已提交任务 */
    private void recreateExecutor() {
        ThreadPoolExecutor old = this.executor;
        this.executor = createExecutor(properties);
        if (old != null) {
            old.shutdown();
        }
    }

    private ThreadPoolExecutor createExecutor(ThreadPoolProperties props) {
        TimeUnit unit = parseTimeUnit(props.getTimeUnit());
        BlockingQueue<Runnable> queue = createQueue(props.getQueueType(), props.getQueueCapacity());
        RejectedExecutionHandler handler = createRejectedHandler(props.getRejectedPolicy());
        return new ThreadPoolExecutor(
                props.getCorePoolSize(),
                props.getMaximumPoolSize(),
                props.getKeepAliveTime(),
                unit,
                queue,
                handler);
    }

    private TimeUnit parseTimeUnit(String unit) {
        return switch (unit.toUpperCase()) {
            case "MINUTES" -> TimeUnit.MINUTES;
            case "HOURS" -> TimeUnit.HOURS;
            case "DAYS" -> TimeUnit.DAYS;
            default -> TimeUnit.SECONDS;
        };
    }

    /**
     * 四种队列：LinkedBlockingQueue/ArrayBlockingQueue/SynchronousQueue/PriorityBlockingQueue
     */
    private BlockingQueue<Runnable> createQueue(String type, int capacity) {
        return switch (type.toUpperCase()) {
            case "ARRAY_BLOCKING" -> new ArrayBlockingQueue<>(capacity);
            case "SYNCHRONOUS" -> new SynchronousQueue<>();
            case "PRIORITY_BLOCKING" -> new PriorityBlockingQueue<>(capacity);
            default -> new LinkedBlockingQueue<>(capacity);
        };
    }

    /** 四种拒绝策略：CallerRunsPolicy/AbortPolicy/DiscardPolicy/DiscardOldestPolicy */
    private RejectedExecutionHandler createRejectedHandler(String policy) {
        return switch (policy.toUpperCase()) {
            case "ABORT" -> new ThreadPoolExecutor.AbortPolicy();
            case "DISCARD" -> new ThreadPoolExecutor.DiscardPolicy();
            case "DISCARD_OLDEST" -> new ThreadPoolExecutor.DiscardOldestPolicy();
            default -> new ThreadPoolExecutor.CallerRunsPolicy();
        };
    }

    /** 应用关闭时优雅销毁线程池 */
    @PreDestroy
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
