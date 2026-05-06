package com.abioclaw.dynamicexecutor.controller;

import com.abioclaw.dynamicexecutor.dto.ThreadPoolConfigDTO;
import com.abioclaw.dynamicexecutor.util.ThreadPoolManager;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池管理 REST 接口
 */
@RestController
public class ExecutorController {

    @Resource
    private ThreadPoolManager threadPoolManager;

    /**
     * 查询线程池实时状态，返回 JSON
     */
    @GetMapping("/executor")
    public Map<String, Object> getExecutorInfo() {
        ThreadPoolExecutor executor = threadPoolManager.getExecutor();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("核心线程数", executor.getCorePoolSize());
        info.put("最大线程数", executor.getMaximumPoolSize());
        info.put("当前线程数", executor.getPoolSize());
        info.put("活跃线程数", executor.getActiveCount());
        info.put("已完成任务数", executor.getCompletedTaskCount());
        info.put("总任务数", executor.getTaskCount());
        info.put("队列类型", executor.getQueue().getClass().getSimpleName());
        info.put("队列大小", executor.getQueue().size());
        int remainingCapacity = executor.getQueue().remainingCapacity();
        info.put("队列剩余容量", remainingCapacity == Integer.MAX_VALUE ? "无界" : remainingCapacity);
        info.put("存活时间", executor.getKeepAliveTime(java.util.concurrent.TimeUnit.SECONDS) + " 秒");
        info.put("拒绝策略", executor.getRejectedExecutionHandler().getClass().getSimpleName());
        return info;
    }

    /**
     * 获取当前线程池配置
     */
    @GetMapping("/executor/config")
    public ThreadPoolConfigDTO getConfig() {
        return threadPoolManager.getCurrentConfig();
    }

    /**
     * 修改线程池配置，即时生效。所有字段可选，null 字段不修改
     */
    @PutMapping("/executor/config")
    public ThreadPoolConfigDTO updateConfig(@RequestBody ThreadPoolConfigDTO dto) {
        threadPoolManager.applyConfig(dto);
        return threadPoolManager.getCurrentConfig();
    }
}
