package com.majortom.algorithms.core.memory;

import java.util.concurrent.CompletionStage;

/** Optional expensive allocation-detail session. The ordinary memory profiler remains independent. */
public interface MemoryDeepProfileSession extends AutoCloseable {
  /** Marks the exact execution body start after profiler setup has completed. */
  void markExecutionStart();

  /** Marks the exact execution body end before profiler teardown begins. */
  void markExecutionEnd();

  /** Schedules JFR post-processing; does not block the measured run. */
  @Override
  void close();

  /** Completes after JFR data has been stopped, dumped and aggregated off the execution thread. */
  CompletionStage<MemoryDeepProfile> completion();
}
