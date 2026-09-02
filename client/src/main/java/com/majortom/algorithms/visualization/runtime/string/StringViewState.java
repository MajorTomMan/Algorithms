package com.majortom.algorithms.visualization.runtime.string;

import java.util.List;

public record StringViewState(
        String target,
        String pattern,
        int targetIndex,
        int patternIndex,
        List<Integer> matches,
        Phase phase,
        boolean completed) {

    public StringViewState {
        target = target == null ? "" : target;
        pattern = pattern == null ? "" : pattern;
        matches = List.copyOf(matches);
    }

    public static StringViewState source(String target) {
        return new StringViewState(target, "", -1, -1, List.of(), Phase.IDLE, false);
    }

    public enum Phase {
        IDLE,
        INITIALIZED,
        COMPARING,
        FALLBACK,
        MATCHED,
        COMPLETED
    }
}
