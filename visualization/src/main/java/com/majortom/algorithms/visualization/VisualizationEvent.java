package com.majortom.algorithms.visualization;

import java.util.Map;

/**
 * 可视化联动事件。
 * 职责：将按钮动作、当前模块与运行状态封装为强类型上下文，便于后续扩展。
 */
public record VisualizationEvent(
        VisualizationActionType actionType,
        String moduleId,
        String source,
        boolean running,
        boolean paused,
        long occurredAtMillis,
        Map<String, Object> metadata) {

    public VisualizationEvent {
        metadata = (metadata == null) ? Map.of() : Map.copyOf(metadata);
    }

    public static VisualizationEvent of(
            VisualizationActionType actionType,
            String moduleId,
            String source,
            boolean running,
            boolean paused) {
        return new VisualizationEvent(
                actionType,
                moduleId,
                source,
                running,
                paused,
                System.currentTimeMillis(),
                Map.of());
    }

    public static VisualizationEvent of(
            VisualizationActionType actionType,
            String moduleId,
            String source,
            boolean running,
            boolean paused,
            Map<String, Object> metadata) {
        return new VisualizationEvent(
                actionType,
                moduleId,
                source,
                running,
                paused,
                System.currentTimeMillis(),
                metadata);
    }
}
