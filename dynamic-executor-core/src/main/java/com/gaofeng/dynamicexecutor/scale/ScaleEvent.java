package com.gaofeng.dynamicexecutor.scale;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 伸缩事件记录
 */
@Data
@AllArgsConstructor
public class ScaleEvent {

    /** 事件时间 */
    private LocalDateTime time;
    /** 方向：UP 扩容 / DOWN 缩容 */
    private String direction;
    /** 老核心线程数 */
    private int oldCore;
    /** 老最大线程数 */
    private int oldMax;
    /** 新核心线程数 */
    private int newCore;
    /** 新最大线程数 */
    private int newMax;
    /** 伸缩原因 */
    private String reason;
}
