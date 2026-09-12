package com.majortom.algorithms.practice.runtime.worker;

import java.util.Objects;

public record PracticeExceptionFact(String type, String message, int lineNumber, boolean caught) {
    public PracticeExceptionFact {
        type = Objects.requireNonNull(type, "type");
    }
}
