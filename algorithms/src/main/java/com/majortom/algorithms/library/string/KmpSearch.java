package com.majortom.algorithms.library.string;

import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.structure.StringStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class KmpSearch {
    public List<Integer> search(StringStructure target, String pattern) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty()) throw new IllegalArgumentException("pattern must not be empty");
        String text = target.raw();
        int[] prefix = prefix(pattern);
        List<Integer> matches = new ArrayList<>();
        ExecutionEvents.emit(new KmpEvent.Initialized(text, pattern));
        int patternIndex = 0;
        for (int targetIndex = 0; targetIndex < text.length(); targetIndex++) {
            ExecutionEvents.checkpoint();
            while (patternIndex > 0 && text.charAt(targetIndex) != pattern.charAt(patternIndex)) {
                ExecutionEvents.emit(new KmpEvent.Compared(targetIndex, patternIndex,
                        text.charAt(targetIndex), pattern.charAt(patternIndex), false));
                int previous = patternIndex;
                patternIndex = prefix[patternIndex - 1];
                ExecutionEvents.emit(new KmpEvent.Fallback(targetIndex, previous, patternIndex));
            }
            boolean equal = text.charAt(targetIndex) == pattern.charAt(patternIndex);
            ExecutionEvents.emit(new KmpEvent.Compared(targetIndex, patternIndex,
                    text.charAt(targetIndex), pattern.charAt(patternIndex), equal));
            if (!equal) continue;
            patternIndex++;
            if (patternIndex == pattern.length()) {
                int start = targetIndex - pattern.length() + 1;
                matches.add(start);
                ExecutionEvents.emit(new KmpEvent.Matched(start, pattern.length()));
                patternIndex = prefix[patternIndex - 1];
            }
        }
        List<Integer> result = List.copyOf(matches);
        ExecutionEvents.emit(new KmpEvent.Completed(result));
        return result;
    }

    private int[] prefix(String pattern) {
        int[] prefix = new int[pattern.length()];
        int matched = 0;
        for (int index = 1; index < pattern.length(); index++) {
            while (matched > 0 && pattern.charAt(index) != pattern.charAt(matched)) matched = prefix[matched - 1];
            if (pattern.charAt(index) == pattern.charAt(matched)) matched++;
            prefix[index] = matched;
        }
        return prefix;
    }
}
