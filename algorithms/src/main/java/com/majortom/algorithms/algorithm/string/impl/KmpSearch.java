package com.majortom.algorithms.algorithm.string.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.logging.Log;
import com.majortom.algorithms.core.runtime.Observations;
import com.majortom.algorithms.structure.string.StringStructure;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Algorithm(id = "kmp", name = "KMP Search", type = String.class, structure = StringStructure.class)
public final class KmpSearch {
  private static final String DEFAULT_PATTERN = "ABABCABAB";

  @AlgorithmEntry
  public List<Integer> search(StringStructure target) {
    Objects.requireNonNull(target, "target");
    String pattern = pattern(target);
    String text = target.value();
    Log.d("KMP", "Search start, text=" + text.length() + ", pattern=" + pattern.length());
    int[] prefix = prefix(pattern);
    List<Integer> matches = new ArrayList<>();
    int patternIndex = 0;
    for (int targetIndex = 0; targetIndex < text.length(); targetIndex++) {
      boolean matchedCharacter = false;
      while (true) {
        Observations.compared("target", targetIndex, "pattern", patternIndex);
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
        Observations.fallback(previousPatternIndex, patternIndex);
      }
      if (!matchedCharacter) {
        continue;
      }
      if (patternIndex == pattern.length()) {
        int matchIndex = targetIndex - pattern.length() + 1;
        matches.add(matchIndex);
        Observations.matched(matchIndex, pattern.length());
        int previousPatternIndex = patternIndex;
        patternIndex = prefix[patternIndex - 1];
        if (patternIndex != previousPatternIndex) {
          Observations.fallback(previousPatternIndex, patternIndex);
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

  private String pattern(StringStructure target) {
    // Pattern selection is KMP policy. Structure and framework deliberately know nothing about it.
    return DEFAULT_PATTERN;
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
