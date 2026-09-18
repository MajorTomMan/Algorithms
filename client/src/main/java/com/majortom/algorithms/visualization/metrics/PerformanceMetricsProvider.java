package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.runtime.ExecutionSummary;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/** Formats execution timing/resource data into the common overview model. */
public final class PerformanceMetricsProvider {
  public List<MetricItem> metrics(ExecutionSummary summary, Optional<Duration> playbackDuration) {
    return List.of(
        MetricItem.text("eventSpan", "label.workspace.metric.event_span",
            duration(summary.timing().eventSpan())),
        MetricItem.text("totalDuration", "label.workspace.metric.total_duration",
            duration(summary.timing().totalDuration())),
        MetricItem.text("playbackDuration", "label.workspace.metric.playback_duration",
            duration(playbackDuration)),
        MetricItem.text("cpu", "label.workspace.metric.cpu_time",
            nanos(summary.resources().cpuTimeNanos())),
        MetricItem.text("peakMemory", "label.workspace.metric.peak_memory",
            bytes(summary.resources().peakMemoryBytes())));
  }

  private static String duration(Duration value) {
    return value.toMillis() + "ms";
  }

  private static String duration(Optional<Duration> value) {
    return value.map(PerformanceMetricsProvider::duration).orElse("—");
  }

  private static String nanos(OptionalLong value) {
    if (value.isEmpty()) return "—";
    return Duration.ofNanos(value.orElseThrow()).toMillis() + "ms";
  }

  private static String bytes(OptionalLong value) {
    if (value.isEmpty()) return "—";
    long bytes = value.orElseThrow();
    if (bytes < 1024L) return bytes + "B";
    double kilobytes = bytes / 1024.0d;
    if (kilobytes < 1024.0d) return String.format(java.util.Locale.ROOT, "%.1fKB", kilobytes);
    return String.format(java.util.Locale.ROOT, "%.1fMB", kilobytes / 1024.0d);
  }
}
