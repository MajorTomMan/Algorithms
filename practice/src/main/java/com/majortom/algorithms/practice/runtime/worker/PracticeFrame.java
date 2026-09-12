package com.majortom.algorithms.practice.runtime.worker;

import java.util.Map;
import java.util.Objects;

public record PracticeFrame(String sourceName, String className, String methodName, int lineNumber,
                            Map<String, String> locals) {
    public PracticeFrame {
        sourceName = Objects.requireNonNullElse(sourceName, "<unknown>");
        className = Objects.requireNonNull(className, "className");
        methodName = Objects.requireNonNull(methodName, "methodName");
        locals = Map.copyOf(Objects.requireNonNull(locals, "locals"));
    }
}
