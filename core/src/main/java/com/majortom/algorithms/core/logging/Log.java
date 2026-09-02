package com.majortom.algorithms.core.logging;

import com.majortom.algorithms.core.runtime.ExecutionEvents;

import java.util.Objects;

/** Direct logging entrypoint for domain code bound to a Runtime execution. */
public final class Log {
    private Log() {
    }

    public static void d(String message) { emit(LogLevel.DEBUG, "", message); }
    public static void i(String message) { emit(LogLevel.INFO, "", message); }
    public static void w(String message) { emit(LogLevel.WARN, "", message); }
    public static void e(String message) { emit(LogLevel.ERROR, "", message); }
    public static void d(String tag, String message) { emit(LogLevel.DEBUG, tag, message); }
    public static void i(String tag, String message) { emit(LogLevel.INFO, tag, message); }
    public static void w(String tag, String message) { emit(LogLevel.WARN, tag, message); }
    public static void e(String tag, String message) { emit(LogLevel.ERROR, tag, message); }

    private static void emit(LogLevel level, String tag, String message) {
        ExecutionEvents.emit(new LogEvent(level, tag == null ? "" : tag, Objects.requireNonNull(message, "message")));
    }
}
