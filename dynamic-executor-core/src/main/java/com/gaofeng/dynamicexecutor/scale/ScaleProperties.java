package com.gaofeng.dynamicexecutor.scale;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自动伸缩规则配置，绑定 application.yml 中 scale.* 配置
 *
 * @ConfigurationProperties 类型安全的配置绑定
 */
@Component
@ConfigurationProperties(prefix = "scale")
@Data
public class ScaleProperties {

    /** 是否启用自动伸缩 */
    private boolean enabled = true;
    /** 伸缩检查间隔（秒） */
    private int checkIntervalSeconds = 15;
    /** 扩容规则 */
    private ScaleUp scaleUp = new ScaleUp();
    /** 缩容规则 */
    private ScaleDown scaleDown = new ScaleDown();

    @Data
    public static class ScaleUp {
        /** 队列使用率超过此百分比时触发扩容 */
        private int queueUsagePercent = 80;
        /** 每次扩容增加的线程数 */
        private int delta = 2;
    }

    @Data
    public static class ScaleDown {
        /** 活跃线程/核心线程比例低于此值时触发缩容检查 */
        private double activeThreadRatio = 0.3;
        /** 缩容需要连续达标次数（防止抖动） */
        private int consecutiveChecks = 4;
    }
}
