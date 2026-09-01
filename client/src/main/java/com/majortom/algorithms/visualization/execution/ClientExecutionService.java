package com.majortom.algorithms.visualization.execution;

import com.majortom.algorithms.core.api.AlgorithmInput;
import com.majortom.algorithms.core.api.AlgorithmInvoker;
import com.majortom.algorithms.core.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Client-facing boundary for starting local executions without exposing runner wiring to UI controllers. */
public interface ClientExecutionService extends AutoCloseable {

    /**
     * Starts an execution and reports statistics for every event consumed by
     * the same reducer cursor that produces {@code liveStateConsumer} states.
     *
     * <p>Implementations must deliver both callbacks on their UI dispatcher.
     * Keeping statistics in this primary contract prevents a client execution
     * adapter from silently dropping live metrics.</p>
     */
    <S> ExecutionHandle start(
            AlgorithmInvoker invoker,
            AlgorithmInput input,
            EventReducer<S> reducer,
            Consumer<S> liveStateConsumer,
            Consumer<ExecutionStatistics> liveStatisticsConsumer,
            LongSupplier delayMillisSupplier);

    /** Starts an execution when the caller does not need live statistics. */
    default <S> ExecutionHandle start(
            AlgorithmInvoker invoker,
            AlgorithmInput input,
            EventReducer<S> reducer,
            Consumer<S> liveStateConsumer,
            LongSupplier delayMillisSupplier) {
        return start(
                invoker,
                input,
                reducer,
                liveStateConsumer,
                ignored -> { },
                delayMillisSupplier);
    }

    @Override
    void close();

    static <S> void requireStartArguments(
            AlgorithmInvoker invoker,
            AlgorithmInput input,
            EventReducer<S> reducer,
            Consumer<S> liveStateConsumer,
            LongSupplier delayMillisSupplier) {
        requireStartArguments(
                invoker, input, reducer, liveStateConsumer, ignored -> { }, delayMillisSupplier);
    }

    static <S> void requireStartArguments(
            AlgorithmInvoker invoker,
            AlgorithmInput input,
            EventReducer<S> reducer,
            Consumer<S> liveStateConsumer,
            Consumer<ExecutionStatistics> liveStatisticsConsumer,
            LongSupplier delayMillisSupplier) {
        Objects.requireNonNull(invoker, "invoker");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(reducer, "reducer");
        Objects.requireNonNull(liveStateConsumer, "liveStateConsumer");
        Objects.requireNonNull(liveStatisticsConsumer, "liveStatisticsConsumer");
        Objects.requireNonNull(delayMillisSupplier, "delayMillisSupplier");
    }
}
