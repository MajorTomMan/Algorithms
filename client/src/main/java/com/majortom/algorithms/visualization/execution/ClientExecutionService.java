package com.majortom.algorithms.visualization.execution;

import com.majortom.algorithms.core.api.AlgorithmInput;
import com.majortom.algorithms.core.api.AlgorithmInvoker;
import com.majortom.algorithms.core.runtime.EventReducer;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Client-facing boundary for starting local executions without exposing runner wiring to UI controllers. */
public interface ClientExecutionService extends AutoCloseable {

    <S> ExecutionHandle start(
            AlgorithmInvoker invoker,
            AlgorithmInput input,
            EventReducer<S> reducer,
            Consumer<S> liveStateConsumer,
            LongSupplier delayMillisSupplier);

    @Override
    void close();

    static <S> void requireStartArguments(
            AlgorithmInvoker invoker,
            AlgorithmInput input,
            EventReducer<S> reducer,
            Consumer<S> liveStateConsumer,
            LongSupplier delayMillisSupplier) {
        Objects.requireNonNull(invoker, "invoker");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(reducer, "reducer");
        Objects.requireNonNull(liveStateConsumer, "liveStateConsumer");
        Objects.requireNonNull(delayMillisSupplier, "delayMillisSupplier");
    }
}
