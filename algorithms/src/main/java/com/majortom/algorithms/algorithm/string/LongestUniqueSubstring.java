package com.majortom.algorithms.algorithm.string;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.core.runtime.Observations;
import com.majortom.algorithms.structure.string.StringStructure;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Sliding-window longest substring without repeated characters. */
@Algorithm(id = "longest-unique-substring", module = "string", type = java.lang.String.class, structure = StringStructure.class)
public final class LongestUniqueSubstring implements LongestSubstringAlgorithm {

    @Override
    public SubstringRange find(StringStructure source) {
        Objects.requireNonNull(source, "source");
        Map<Character, Integer> lastSeen = new HashMap<>();
        int windowStart = 0;
        int bestStart = 0;
        int bestLength = 0;
        for (int index = 0; index < source.length(); index++) {
            ExecutionEvents.checkpoint();
            char value = source.charAt(index);
            Integer previous = lastSeen.put(value, index);
            if (previous != null) {
                Observations.compared("target", previous, "target", index);
                if (previous >= windowStart) {
                    windowStart = previous + 1;
                }
            }
            int length = index - windowStart + 1;
            if (length > bestLength) {
                bestStart = windowStart;
                bestLength = length;
                Observations.matched(bestStart, bestLength);
            }
        }
        return new SubstringRange(bestStart, bestLength);
    }
}
