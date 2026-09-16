package com.majortom.algorithms.visualization.render.timing;

import java.time.Duration;

/**
 * Scheduling abstraction for render/presentation timing. Implementations must not touch JavaFX
 * directly.
 */
@FunctionalInterface
public interface RenderTimer {
    void schedule(Duration delay, Runnable task);
}
