package com.majortom.algorithms.library.string;

import com.majortom.algorithms.library.structure.StringStructure;

/** Algorithms that select one longest substring from a StringStructure. */
public interface LongestSubstringAlgorithm extends StringAlgorithm {
    SubstringRange find(StringStructure source);

    record SubstringRange(int start, int length) {
        public SubstringRange {
            if (start < 0) {
                throw new IllegalArgumentException("substring start must not be negative");
            }
            if (length < 0) {
                throw new IllegalArgumentException("substring length must not be negative");
            }
        }

        public String value(StringStructure source) {
            return source.value().substring(start, start + length);
        }
    }
}
