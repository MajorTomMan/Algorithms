package com.majortom.algorithms.visualization.execution;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionOperation;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.ExecutionSession;
import com.majortom.algorithms.visualization.runtime.LocalAlgorithmExecution;
import java.util.List;
import java.util.Objects;
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
  public <S> ExecutionHandle start(String operationId, ExecutionOperation<?> operation,
      EventReducer<S> reducer, Consumer<EventEnvelope> liveEventConsumer,
      Consumer<S> liveStateConsumer, Consumer<ExecutionStatistics> liveStatisticsConsumer,
      LongSupplier delayMillisSupplier) {
    ClientExecutionService.requireStartArguments(operationId, operation, reducer, liveEventConsumer,
        liveStateConsumer, liveStatisticsConsumer, delayMillisSupplier);
    ExecutionSession session = delegate.start(operationId, operation, reducer, liveEventConsumer,
        liveStateConsumer, liveStatisticsConsumer, delayMillisSupplier);
    return session;
  }

  @Override
  public <S> ExecutionHandle startBatched(String operationId, ExecutionOperation<?> operation,
      EventReducer<S> reducer, Consumer<List<EventEnvelope>> liveEventsConsumer,
      Consumer<S> liveStateConsumer, Consumer<ExecutionStatistics> liveStatisticsConsumer,
      LongSupplier delayMillisSupplier) {
    ClientExecutionService.requireStartArguments(operationId, operation, reducer, ignored -> {},
        liveStateConsumer, liveStatisticsConsumer, delayMillisSupplier);
    Objects.requireNonNull(liveEventsConsumer, "liveEventsConsumer");
    ExecutionSession session = delegate.startBatched(operationId, operation, reducer,
        liveEventsConsumer, liveStateConsumer, liveStatisticsConsumer, delayMillisSupplier);
    return session;
  }

  @Override
  public void close() {
    delegate.close();
  }

}
