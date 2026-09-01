package com.majortom.algorithms.visualization.execution;

import com.majortom.algorithms.core.api.AlgorithmInput;
import com.majortom.algorithms.core.api.AlgorithmInvoker;
import com.majortom.algorithms.core.runtime.ExecutionEvent;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.runtime.EventReducer;
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

/** Adapter that exposes the existing per-run local runtime through a client service boundary. */
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
            AlgorithmInvoker invoker,
            AlgorithmInput input,
            EventReducer<S> reducer,
            Consumer<S> liveStateConsumer,
            LongSupplier delayMillisSupplier) {
        ClientExecutionService.requireStartArguments(
                invoker, input, reducer, liveStateConsumer, delayMillisSupplier);
        ExecutionSession session = delegate.start(
                invoker, input, reducer, liveStateConsumer, delayMillisSupplier);
        return new LocalExecutionHandle(session);
    }

    @Override
    public <S> ExecutionHandle start(
            AlgorithmInvoker invoker,
            AlgorithmInput input,
            EventReducer<S> reducer,
            Consumer<S> liveStateConsumer,
            Consumer<ExecutionStatistics> liveStatisticsConsumer,
            LongSupplier delayMillisSupplier) {
        ClientExecutionService.requireStartArguments(
                invoker, input, reducer, liveStateConsumer, liveStatisticsConsumer, delayMillisSupplier);
        ExecutionSession session = delegate.start(
                invoker,
                input,
                reducer,
                liveStateConsumer,
                liveStatisticsConsumer,
                delayMillisSupplier);
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
        public List<ExecutionEvent> events() {
            return delegate.events();
        }

        @Override
        public CompletableFuture<ExecutionResult> completion() {
            return delegate.completion();
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
        public void closeObserver() {
            delegate.closeObserver();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
