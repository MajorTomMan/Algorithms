package com.majortom.algorithms.library.string;

import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.logging.Log;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.structure.StringStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class KmpSearch implements StringSearch {
    @Override
    public List<Integer> search(StringStructure target, String pattern) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty()) {
            Log.e("KMP", "Pattern must not be empty");
            throw new IllegalArgumentException("pattern must not be empty");
        }
        String text = target.value();
        Log.d("KMP", "Search start, text=" + text.length() + ", pattern=" + pattern.length());
        int[] prefix = prefix(pattern);
        List<Integer> matches = new ArrayList<>();
        int patternIndex = 0;
        for (int targetIndex = 0; targetIndex < text.length(); targetIndex++) {
            boolean matchedCharacter = false;
            while (true) {
                ExecutionEvents.observe(new ObservationEvent.Compared(
                        new ObservationEvent.IndexRef("target", targetIndex),
                        new ObservationEvent.IndexRef("pattern", patternIndex)));
                if (text.charAt(targetIndex) == pattern.charAt(patternIndex)) {
                    patternIndex++;
                    matchedCharacter = true;
                    break;
                }
                if (patternIndex == 0) {
                    break;
                }
                int previousPatternIndex = patternIndex;
                patternIndex = prefix[patternIndex - 1];
                ExecutionEvents.observe(new ObservationEvent.Fallback(previousPatternIndex, patternIndex));
            }
            if (!matchedCharacter) {
                continue;
            }
            if (patternIndex == pattern.length()) {
                int matchIndex = targetIndex - pattern.length() + 1;
                matches.add(matchIndex);
                ExecutionEvents.observe(new ObservationEvent.Matched(matchIndex, pattern.length()));
                int previousPatternIndex = patternIndex;
                patternIndex = prefix[patternIndex - 1];
                if (patternIndex != previousPatternIndex) {
                    ExecutionEvents.observe(new ObservationEvent.Fallback(previousPatternIndex, patternIndex));
                }
            }
        }
        List<Integer> result = List.copyOf(matches);
        if (result.isEmpty()) {
            Log.w("KMP", "No matches found");
        } else {
            Log.i("KMP", "Matches found: " + result.size());
        }
        return result;
    }

    private int[] prefix(String pattern) {
        int[] prefix = new int[pattern.length()];
        int matched = 0;
        for (int index = 1; index < pattern.length(); index++) {
            while (matched > 0 && pattern.charAt(index) != pattern.charAt(matched)) {
                matched = prefix[matched - 1];
            }
            if (pattern.charAt(index) == pattern.charAt(matched)) {
                matched++;
            }
            prefix[index] = matched;
        }
        return prefix;
    }
}
