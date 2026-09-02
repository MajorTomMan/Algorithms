package com.majortom.algorithms.core.logging;

import com.majortom.algorithms.core.event.ExecutionEvent;

import java.util.Objects;

/** Domain log payload; runtime metadata is supplied by EventEnvelope. */
public record LogEvent(LogLevel level, String tag, String message) implements ExecutionEvent {
    public LogEvent {
        Objects.requireNonNull(level, "level");
        tag = tag == null ? "" : tag;
        Objects.requireNonNull(message, "message");
    }
}
