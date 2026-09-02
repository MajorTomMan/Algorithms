package com.majortom.algorithms.server.request;

import java.util.Objects;

public record StringSearchRequest(String target, String pattern) {
    public StringSearchRequest {
        target = Objects.requireNonNull(target, "target");
        pattern = Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty()) throw new IllegalArgumentException("pattern must not be empty");
    }
}
