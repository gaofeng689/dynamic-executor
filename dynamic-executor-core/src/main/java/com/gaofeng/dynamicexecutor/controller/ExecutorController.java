package com.gaofeng.dynamicexecutor.controller;

import com.gaofeng.dynamicexecutor.dto.ThreadPoolConfigDTO;
import com.gaofeng.dynamicexecutor.metrics.PoolSnapshot;
import com.gaofeng.dynamicexecutor.metrics.PoolSnapshotMapper;
import com.gaofeng.dynamicexecutor.scale.AutoScaler;
import com.gaofeng.dynamicexecutor.scale.ScaleEvent;
import com.gaofeng.dynamicexecutor.scale.ScaleProperties;
import com.gaofeng.dynamicexecutor.util.ThreadPoolManager;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池管理 REST 接口
 */
@RestController
public class ExecutorController {

    @Resource
    private ThreadPoolManager threadPoolManager;
    @Resource
    private PoolSnapshotMapper poolSnapshotMapper;
    @Resource
    private AutoScaler autoScaler;
    @Resource
    private ScaleProperties scaleProperties;

    /**
     * 查询线程池实时状态
     * GET /executor
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
     * GET /executor/config
     */
    @GetMapping("/executor/config")
    public ThreadPoolConfigDTO getConfig() {
        return threadPoolManager.getCurrentConfig();
    }

    /**
     * 修改线程池配置，即时生效。所有字段可选，null 字段不修改
     * PUT /executor/config
     */
    @PutMapping("/executor/config")
    public ThreadPoolConfigDTO updateConfig(@RequestBody ThreadPoolConfigDTO dto) {
        threadPoolManager.applyConfig(dto);
        return threadPoolManager.getCurrentConfig();
    }

    /**
     * 查询历史指标快照（给前端 Chart.js 用的数据源）
     * GET /executor/metrics/history?minutes=5
     */
    @GetMapping("/executor/metrics/history")
    public List<PoolSnapshot> getMetricsHistory(@RequestParam(defaultValue = "5") int minutes) {
        return poolSnapshotMapper.selectRecent(minutes);
    }

    /**
     * 获取伸缩事件历史
     * GET /executor/scale/history
     */
    @GetMapping("/executor/scale/history")
    public List<ScaleEvent> getScaleHistory() {
        return autoScaler.getScaleHistory();
    }

    /**
     * 获取自动伸缩状态 + 规则
     * GET /executor/scale/status
     */
    @GetMapping("/executor/scale/status")
    public Map<String, Object> getScaleStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", scaleProperties.isEnabled());
        status.put("consecutiveDownChecks", autoScaler.getScaleDownConsecutiveCount());
        status.put("scaleUp", scaleProperties.getScaleUp());
        status.put("scaleDown", scaleProperties.getScaleDown());
        return status;
    }

    /**
     * 启用/停用自动伸缩
     * PUT /executor/scale/status
     */
    @PutMapping("/executor/scale/status")
    public Map<String, Object> toggleScale(@RequestBody Map<String, Boolean> body) {
        if (body.containsKey("enabled")) {
            scaleProperties.setEnabled(body.get("enabled"));
        }
        return getScaleStatus();
    }

    /**
     * 提交批量任务制造负载波动（用于测试图表和自动伸缩）
     * POST /executor/task/load?count=10&sleepMs=3000
     */
    @PostMapping("/executor/task/load")
    public Map<String, Object> submitLoad(@RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "3000") long sleepMs) {
        ThreadPoolExecutor executor = threadPoolManager.getExecutor();
        long beforeActive = executor.getActiveCount();
        long beforeQueue = executor.getQueue().size();
        long beforeCompleted = executor.getCompletedTaskCount();

        for (int i = 0; i < count; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submitted", count);
        result.put("sleepMs", sleepMs);
        result.put("activeBefore", beforeActive);
        result.put("queueBefore", beforeQueue);
        result.put("completedBefore", beforeCompleted);
        result.put("activeNow", executor.getActiveCount());
        result.put("queueNow", executor.getQueue().size());
        result.put("poolSize", executor.getPoolSize());
        return result;
    }
}
