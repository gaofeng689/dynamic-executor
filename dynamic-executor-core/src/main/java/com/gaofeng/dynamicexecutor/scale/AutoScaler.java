package com.gaofeng.dynamicexecutor.scale;

import com.gaofeng.dynamicexecutor.dto.ThreadPoolConfigDTO;
import com.gaofeng.dynamicexecutor.util.ThreadPoolManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 自动伸缩执行器
 *
 * 定时检查线程池负载：
 * - 队列使用率超过阈值时，上调最大线程数（扩容）
 * - 活跃线程占比过小时，下调核心线程数（缩容，需连续 N 次达标防止抖动）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoScaler {

    private final ThreadPoolManager threadPoolManager;
    private final ScaleProperties scaleProperties;
    /** 伸缩事件历史（线程安全） */
    private final List<ScaleEvent> scaleHistory = new CopyOnWriteArrayList<>();
    /** 缩容连续达标计数器 */
    private int scaleDownConsecutiveCount = 0;

    /** 定时检查并执行伸缩逻辑 */
    @Scheduled(fixedDelayString = "#{scaleProperties.checkIntervalSeconds * 1000}")
    public void autoScale() {
        if (!scaleProperties.isEnabled()) {
            return;
        }

        ThreadPoolExecutor executor = threadPoolManager.getExecutor();
        int core = executor.getCorePoolSize();
        int max = executor.getMaximumPoolSize();
        int queueSize = executor.getQueue().size();
        int queueCapacity = executor.getQueue().remainingCapacity() + queueSize;
        int activeCount = executor.getActiveCount();

        // 扩容：队列使用率超过阈值
        if (queueCapacity > 0) {
            double queueUsage = (double) queueSize / queueCapacity * 100;
            if (queueUsage > scaleProperties.getScaleUp().getQueueUsagePercent()) {
                int delta = scaleProperties.getScaleUp().getDelta();
                int newMax = Math.min(max + delta, max + 10);
                ThreadPoolConfigDTO dto = new ThreadPoolConfigDTO();
                dto.setMaximumPoolSize(newMax);
                threadPoolManager.applyConfig(dto);

                String reason = String.format("队列使用率 %.1f%% > %d%%",
                        queueUsage, scaleProperties.getScaleUp().getQueueUsagePercent());
                scaleHistory.add(new ScaleEvent(LocalDateTime.now(), "UP", core, max, core, newMax, reason));
                log.warn("自动扩容: {} -> {}, 原因: {}", max, newMax, reason);
                scaleDownConsecutiveCount = 0;
                return;
            }
        }

        // 缩容：活跃线程占比过低，需连续达标防抖动
        double ratio = (double) activeCount / core;
        if (activeCount > 0 && ratio < scaleProperties.getScaleDown().getActiveThreadRatio() && core > 1) {
            scaleDownConsecutiveCount++;
            int need = scaleProperties.getScaleDown().getConsecutiveChecks();
            if (scaleDownConsecutiveCount >= need) {
                int newCore = Math.max(core - 1, 1);
                ThreadPoolConfigDTO dto = new ThreadPoolConfigDTO();
                dto.setCorePoolSize(newCore);
                threadPoolManager.applyConfig(dto);

                String reason = String.format("活跃线程占比 %.1f%% < %.0f%%, 连续%d次达标",
                        ratio * 100, scaleProperties.getScaleDown().getActiveThreadRatio() * 100, need);
                scaleHistory.add(new ScaleEvent(LocalDateTime.now(), "DOWN", core, max, newCore, max, reason));
                log.info("自动缩容: core {} -> {}, 原因: {}", core, newCore, reason);
                scaleDownConsecutiveCount = 0;
            }
        } else {
            scaleDownConsecutiveCount = 0;
        }
    }

    /** 获取伸缩事件历史（返回副本） */
    public List<ScaleEvent> getScaleHistory() {
        return new ArrayList<>(scaleHistory);
    }

    /** 获取当前缩容连续达标次数 */
    public int getScaleDownConsecutiveCount() {
        return scaleDownConsecutiveCount;
    }
}
