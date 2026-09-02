package com.majortom.algorithms.visualization.execution;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.ExecutionOperation;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Client-facing boundary for starting runtime-managed domain operations. */
public interface ClientExecutionService extends AutoCloseable {

    <S> ExecutionHandle start(String operationId, ExecutionOperation<?> operation, EventReducer<S> reducer,
            Consumer<EventEnvelope> liveEventConsumer, Consumer<S> liveStateConsumer,
            Consumer<ExecutionStatistics> liveStatisticsConsumer, LongSupplier delayMillisSupplier);

    default <S> ExecutionHandle start(String operationId, ExecutionOperation<?> operation, EventReducer<S> reducer,
            Consumer<S> liveStateConsumer, Consumer<ExecutionStatistics> liveStatisticsConsumer,
            LongSupplier delayMillisSupplier) {
        return start(operationId, operation, reducer, ignored -> { }, liveStateConsumer, liveStatisticsConsumer,
                delayMillisSupplier);
    }

    default <S> ExecutionHandle start(String operationId, ExecutionOperation<?> operation, EventReducer<S> reducer,
            Consumer<S> liveStateConsumer, LongSupplier delayMillisSupplier) {
        return start(operationId, operation, reducer, ignored -> { }, liveStateConsumer, ignored -> { },
                delayMillisSupplier);
    }

    @Override
    void close();

    static <S> void requireStartArguments(String operationId, ExecutionOperation<?> operation, EventReducer<S> reducer,
            Consumer<EventEnvelope> liveEventConsumer, Consumer<S> liveStateConsumer,
            Consumer<ExecutionStatistics> liveStatisticsConsumer, LongSupplier delayMillisSupplier) {
        Objects.requireNonNull(operationId, "operationId");
        if (operationId.isBlank()) throw new IllegalArgumentException("operationId must not be blank");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(reducer, "reducer");
        Objects.requireNonNull(liveEventConsumer, "liveEventConsumer");
        Objects.requireNonNull(liveStateConsumer, "liveStateConsumer");
        Objects.requireNonNull(liveStatisticsConsumer, "liveStatisticsConsumer");
        Objects.requireNonNull(delayMillisSupplier, "delayMillisSupplier");
    }
}
