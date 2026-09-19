package com.majortom.algorithms.core.event.algorithm;

import com.majortom.algorithms.core.domain.observation.ArrayObservationDomains;

/** Sorting/searching decisions in an array, never ArrayStructure mutations. */
public interface ArrayAlgorithmEvent extends AlgorithmEvent {
  record PivotSelected(int pivotIndex, int fromInclusive, int toExclusive)
      implements ArrayAlgorithmEvent, AlgorithmEvent.Targeted {
    public PivotSelected {
      if (fromInclusive < 0 || toExclusive <= fromInclusive || pivotIndex < fromInclusive
          || pivotIndex >= toExclusive) throw new IllegalArgumentException("invalid pivot range");
    }
    @Override public AlgorithmEvent.IndexRef target() {
      return new AlgorithmEvent.IndexRef(ArrayObservationDomains.INDEX_SOURCE, pivotIndex);
    }
  }
  record RangeExamined(int fromInclusive, int toExclusive) implements ArrayAlgorithmEvent {
    public RangeExamined {
      if (fromInclusive < 0 || toExclusive <= fromInclusive)
        throw new IllegalArgumentException("invalid array range");
    }
  }
}
