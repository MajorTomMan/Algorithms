package com.majortom.algorithms.visualization.execution;

import com.majortom.algorithms.core.runtime.ExecutionEvent;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ResourceUsage;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Handle-scoped controls and observations for one local client execution. */
public interface ExecutionHandle extends AutoCloseable {

    List<ExecutionEvent> events();

    CompletableFuture<ExecutionResult> completion();

    Optional<Duration> totalDuration();

    ResourceUsage resourceUsage();

    void pauseExecution();

    void resumeExecution();

    void closeObserver();

    @Override
    void close();
}
