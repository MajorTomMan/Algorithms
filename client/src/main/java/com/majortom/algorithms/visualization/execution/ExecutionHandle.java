package com.majortom.algorithms.visualization.execution;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ResourceUsage;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Handle-scoped controls and observations for one local client execution. */
public interface ExecutionHandle extends AutoCloseable {

    List<EventEnvelope> events();

    CompletableFuture<ExecutionResult> runtimeCompletion();

    CompletableFuture<ExecutionResult> presentationCompletion();

    Optional<Duration> totalDuration();

    ResourceUsage resourceUsage();

    void pauseExecution();

    void resumeExecution();

    void stepExecution();

    void closeObserver();

    @Override
    void close();
}
