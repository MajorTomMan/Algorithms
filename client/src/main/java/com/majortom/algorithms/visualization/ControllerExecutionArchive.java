package com.majortom.algorithms.visualization;

import com.majortom.algorithms.core.domain.execution.RunCancelledEvent;
import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.domain.execution.RunFailedEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionAnchorTimeline;
import com.majortom.algorithms.core.runtime.ExecutionRecording;
import com.majortom.algorithms.core.runtime.ExecutionRecordingState;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.runtime.ExecutionSummary;
import com.majortom.algorithms.core.runtime.ExecutionTiming;
import com.majortom.algorithms.core.statistics.MetricKeys;
import com.majortom.algorithms.visualization.execution.ClientExecutionRecord;
import com.majortom.algorithms.visualization.execution.ExecutionExporter;
import com.majortom.algorithms.visualization.execution.InputFingerprint;
import com.majortom.algorithms.visualization.execution.RunHistoryService;
import com.majortom.algorithms.visualization.international.I18N;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

/** Creates validated execution records, manages history/export and formats record comparisons. */
final class ControllerExecutionArchive {
    private final RunHistoryService history;
    private final InputFingerprint fingerprint;
    private final ExecutionExporter exporter;
    private final Consumer<String> log;
    private final Consumer<Throwable> onError;

    ControllerExecutionArchive(RunHistoryService history, InputFingerprint fingerprint,
            ExecutionExporter exporter, Consumer<String> log, Consumer<Throwable> onError) {
        this.history = Objects.requireNonNull(history, "history");
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        this.exporter = Objects.requireNonNull(exporter, "exporter");
        this.log = Objects.requireNonNull(log, "log");
        this.onError = Objects.requireNonNull(onError, "onError");
    }

    void retain(ClientExecutionRecord record) { history.add(record); }

    void export(ClientExecutionRecord record, ExecutionSummary summary) {
        if (record == null) {
            log.accept("Nothing to export.");
            return;
        }
        try {
            java.nio.file.Path file = exporter.export(record, summary);
            log.accept("Exported: " + file);
        } catch (IOException exception) {
            onError.accept(exception);
        }
    }

    void compare(ClientExecutionRecord record) {
        if (record == null) {
            log.accept("No execution data available for comparison.");
            return;
        }
        List<ClientExecutionRecord> candidates = history.comparableWith(record);
        if (candidates.isEmpty()) {
            log.accept("No comparable executions found for the same input.");
            return;
        }
        log.accept("Comparison for input " + record.inputFingerprint() + ":");
        log.accept(describeRecord(record));
        for (ClientExecutionRecord candidate : candidates) {
            log.accept(describeRecord(candidate));
        }
    }


    private String describeRecord(ClientExecutionRecord record) {
        ExecutionSummary summary = record.recording().summary();
        ExecutionTiming timing = summary.timing();
        return String.format(
                "%s | event-span=%dms | total=%s | cpu=%s | memory=%s | events=%d | frames=%d | compares=%d",
                record.operationId(), timing.eventSpan().toMillis(),
                formatDuration(timing.totalDuration()),
                formatNanos(summary.resources().cpuTimeNanos()),
                formatBytes(summary.resources().peakMemoryBytes()),
                record.recording().statistics().totalEventCount(),
                record.visualFrameCount(),
                record.recording().statistics().metric(MetricKeys.COMPARISONS));
    }


    ClientExecutionRecord createRecord(
            String moduleId,
            String operationId,
            Object input,
            ExecutionResult result,
            Throwable error,
            ExecutionSummary summary,
            List<EventEnvelope> events,
            ExecutionAnchorTimeline executionAnchors,
            long visualFrameCount) {
        if (events.isEmpty()) {
            return null;
        }
        if (executionAnchors == null) {
            return null;
        }
        if (!hasTerminalLifecycleEvent(events)) {
            // An external Error may interrupt a run before its terminal lifecycle event.
            // Keep the visual timeline available, but do not publish an invalid history record.
            return null;
        }
        ExecutionRecordingState state = recordingState(result, error);
        EventEnvelope firstEvent = events.getFirst();
        ExecutionStatistics authoritativeStatistics = summary.statistics();
        ExecutionSummary recordingSummary = ExecutionSummary.from(
                authoritativeStatistics, summary.resources()).withTiming(
                ExecutionTiming.of(
                        authoritativeStatistics.eventSpan(),
                        summary.timing().totalDuration()));
        ExecutionRecording recording = new ExecutionRecording(
                firstEvent.runId(), operationId, state, authoritativeStatistics, recordingSummary, events);
        ExecutionResult effectiveResult = result;
        if (effectiveResult == null) {
            String message = "Execution failed";
            String exceptionType = RuntimeException.class.getName();
            if (error != null) {
                if (error.getMessage() != null && !error.getMessage().isBlank()) {
                    message = error.getMessage();
                }
                exceptionType = error.getClass().getName();
            }
            effectiveResult = ExecutionResult.failed(
                    new com.majortom.algorithms.core.runtime.ExecutionFailure(
                            "client.execution.failed",
                            message,
                            exceptionType));
        }
        return new ClientExecutionRecord(
                moduleId, operationId, fingerprint.fingerprint(input), effectiveResult, recording,
                executionAnchors, visualFrameCount);
    }

    private boolean hasTerminalLifecycleEvent(List<EventEnvelope> events) {
        for (EventEnvelope event : events) {
            if (event.event() instanceof RunCompletedEvent
                    || event.event() instanceof RunCancelledEvent
                    || event.event() instanceof RunFailedEvent) {
                return true;
            }
        }
        return false;
    }

    private ExecutionRecordingState recordingState(ExecutionResult result, Throwable error) {
        if (error != null) {
            return ExecutionRecordingState.FAILED;
        }
        if (result == null) {
            return ExecutionRecordingState.FAILED;
        }
        return switch (result.status()) {
            case COMPLETED -> ExecutionRecordingState.COMPLETED;
            case CANCELLED -> ExecutionRecordingState.CANCELLED;
            case FAILED -> ExecutionRecordingState.FAILED;
        };
    }


    static String formatDuration(Optional<Duration> duration) {
        if (duration.isEmpty()) {
            return I18N.text("stats.unavailable");
        }
        return duration.orElseThrow().toMillis() + "ms";
    }

    static String formatNanos(OptionalLong nanos) {
        if (nanos.isEmpty()) {
            return I18N.text("stats.unavailable");
        }
        return Duration.ofNanos(nanos.orElseThrow()).toMillis() + "ms";
    }

    static String formatBytes(OptionalLong bytes) {
        if (bytes.isEmpty()) {
            return I18N.text("stats.unavailable");
        }
        long value = bytes.orElseThrow();
        if (value < 1024L) {
            return value + "B";
        }
        long kilobytes = value / 1024L;
        if (kilobytes < 1024L) {
            return kilobytes + "KB";
        }
        return String.format("%.1fMB", kilobytes / 1024.0d);
    }

}
