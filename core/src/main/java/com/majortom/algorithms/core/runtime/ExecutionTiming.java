package com.majortom.algorithms.core.runtime;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit timing semantics for one execution and its consumers.
 *
 * <p>{@code eventSpan} is derived from the execution event timestamps and may
 * include event-delivery or pacing waits.
 * {@code totalDuration}, when present, is measured by the hosting runtime from
 * local execution start to completion and may include event delivery or pacing.
 * {@code playbackDuration}, when present, is time spent actively replaying the
 * recorded frames and is never substituted for event span.</p>
 */
public record ExecutionTiming(
        Duration eventSpan,
        Optional<Duration> totalDuration,
        Optional<Duration> playbackDuration) {

    public ExecutionTiming {
        eventSpan = requireDuration(eventSpan, "eventSpan");
        totalDuration = requireOptionalDuration(totalDuration, "totalDuration");
        playbackDuration = requireOptionalDuration(playbackDuration, "playbackDuration");
    }

    /** Creates timing when only the event-derived span is known. */
    public static ExecutionTiming eventOnly(Duration eventSpan) {
        return new ExecutionTiming(eventSpan, Optional.empty(), Optional.empty());
    }

    /** Creates timing with all available measurements. */
    public static ExecutionTiming of(
            Duration eventSpan,
            Optional<Duration> totalDuration,
            Optional<Duration> playbackDuration) {
        return new ExecutionTiming(eventSpan, totalDuration, playbackDuration);
    }

    /** Creates timing with explicit total and playback measurements. */
    public static ExecutionTiming of(
            Duration eventSpan,
            Duration totalDuration,
            Duration playbackDuration) {
        return new ExecutionTiming(
                eventSpan,
                Optional.of(totalDuration),
                Optional.of(playbackDuration));
    }

    /** Alias that emphasizes that total time belongs to the hosting runtime. */
    public Optional<Duration> totalExecutionDuration() {
        return totalDuration;
    }

    /** Alias for consumers that call replay playback rather than playback. */
    public Optional<Duration> replayDuration() {
        return playbackDuration;
    }

    /** Creates timing with a measured total execution duration. */
    public ExecutionTiming withTotalDuration(Duration value) {
        return new ExecutionTiming(eventSpan, Optional.of(requireDuration(value, "totalDuration")),
                playbackDuration);
    }

    /** Creates timing with a measured playback duration. */
    public ExecutionTiming withPlaybackDuration(Duration value) {
        return new ExecutionTiming(eventSpan, totalDuration,
                Optional.of(requireDuration(value, "playbackDuration")));
    }

    private static Duration requireDuration(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }

    private static Optional<Duration> requireOptionalDuration(Optional<Duration> value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isPresent()) {
            requireDuration(value.orElseThrow(), name);
        }
        return value;
    }
}
