package com.majortom.algorithms.telemetry.memory.analysis;

import com.majortom.algorithms.telemetry.analysis.StateAnalyzer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/** Optional JOL-backed state analyzer kept fully outside JavaFX and Render. */
public final class JolStructureFootprintAnalyzer implements StateAnalyzer<Object, StructureFootprint>, AutoCloseable {
  private static final String GRAPH_LAYOUT = "org.openjdk.jol.info.GraphLayout";

  private final ExecutorService executor = Executors.newSingleThreadExecutor(new AnalyzerThreadFactory());
  private final Class<?> graphLayoutClass;
  private final Method parseInstance;
  private final Method totalSize;
  private final Method totalCount;

  public JolStructureFootprintAnalyzer() {
    Class<?> type = null;
    Method parse = null;
    Method size = null;
    Method count = null;
    try {
      type = Class.forName(GRAPH_LAYOUT);
      parse = type.getMethod("parseInstance", Object[].class);
      size = type.getMethod("totalSize");
      count = type.getMethod("totalCount");
    } catch (ClassNotFoundException | NoSuchMethodException ignored) {
      type = null;
      parse = null;
      size = null;
      count = null;
    }
    graphLayoutClass = type;
    parseInstance = parse;
    totalSize = size;
    totalCount = count;
  }

  public boolean isAvailable() {
    return graphLayoutClass != null;
  }

  @Override
  public CompletionStage<StructureFootprint> analyze(Object root) {
    if (!isAvailable()) {
      return CompletableFuture.completedFuture(
          StructureFootprint.unavailable("jol-core is not present on the runtime classpath"));
    }
    if (root == null) {
      return CompletableFuture.completedFuture(
          StructureFootprint.unavailable("No current structure root is available"));
    }
    return CompletableFuture.supplyAsync(() -> measure(root), executor);
  }

  @Override
  public void close() {
    executor.shutdownNow();
  }

  private StructureFootprint measure(Object root) {
    try {
      Object layout = parseInstance.invoke(null, (Object) new Object[] {root});
      long bytes = ((Number) totalSize.invoke(layout)).longValue();
      long count = ((Number) totalCount.invoke(layout)).longValue();
      return new StructureFootprint(true, root.getClass().getName(), bytes, count, "JOL", "GraphLayout.parseInstance");
    } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
      Throwable cause = exception instanceof InvocationTargetException invocation
          && invocation.getCause() != null ? invocation.getCause() : exception;
      return StructureFootprint.unavailable(
          Objects.toString(cause.getMessage(), cause.getClass().getSimpleName()));
    }
  }

  private static final class AnalyzerThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable task) {
      Thread thread = new Thread(task, "structure-footprint-analyzer");
      thread.setDaemon(true);
      return thread;
    }
  }
}
