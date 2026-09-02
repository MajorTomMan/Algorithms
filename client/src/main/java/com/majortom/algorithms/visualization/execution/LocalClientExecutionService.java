package com.majortom.algorithms.visualization.execution;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.ExecutionOperation;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.runtime.ResourceUsage;
import com.majortom.algorithms.visualization.runtime.ExecutionSession;
import com.majortom.algorithms.visualization.runtime.LocalAlgorithmExecution;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Adapter exposing the local runtime through the client execution boundary. */
public final class LocalClientExecutionService implements ClientExecutionService {

    private final LocalAlgorithmExecution delegate;

    public LocalClientExecutionService() {
        this(new LocalAlgorithmExecution());
    }

    LocalClientExecutionService(LocalAlgorithmExecution delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public <S> ExecutionHandle start(
            String operationId,
            ExecutionOperation<?> operation,
            EventReducer<S> reducer,
            Consumer<EventEnvelope> liveEventConsumer,
            Consumer<S> liveStateConsumer,
            Consumer<ExecutionStatistics> liveStatisticsConsumer,
            LongSupplier delayMillisSupplier) {
        ClientExecutionService.requireStartArguments(operationId, operation, reducer, liveEventConsumer,
                liveStateConsumer, liveStatisticsConsumer, delayMillisSupplier);
        ExecutionSession session = delegate.start(operationId, operation, reducer, liveEventConsumer, liveStateConsumer,
                liveStatisticsConsumer, delayMillisSupplier);
        return new LocalExecutionHandle(session);
    }

    @Override
    public void close() {
        delegate.close();
    }

    private static final class LocalExecutionHandle implements ExecutionHandle {

        private final ExecutionSession delegate;

        private LocalExecutionHandle(ExecutionSession delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public List<EventEnvelope> events() {
            return delegate.events();
        }

        @Override
        public CompletableFuture<ExecutionResult> runtimeCompletion() {
            return delegate.runtimeCompletion();
        }

        @Override
        public CompletableFuture<ExecutionResult> presentationCompletion() {
            return delegate.presentationCompletion();
        }

        @Override
        public Optional<Duration> totalDuration() {
            return delegate.totalDuration();
        }

        @Override
        public ResourceUsage resourceUsage() {
            return delegate.resourceUsage();
        }

        @Override
        public void pauseExecution() {
            delegate.pauseExecution();
        }

        @Override
        public void resumeExecution() {
            delegate.resumeExecution();
        }

        @Override
        public void stepExecution() {
            delegate.stepExecution();
        }

        @Override
        public void closeObserver() {
            delegate.closeObserver();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
