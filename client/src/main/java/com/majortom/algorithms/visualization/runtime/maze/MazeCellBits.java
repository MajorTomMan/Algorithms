package com.majortom.algorithms.visualization.runtime.maze;

import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.IntConsumer;

/** Immutable bit-packed maze cells; updates clone only the compact word array. */
final class MazeCellBits extends AbstractList<Boolean> implements RandomAccess {
  private final int length;
  private final long[] words;

  private MazeCellBits(int length, long[] words) {
    this.length = length;
    this.words = words;
  }

  static MazeCellBits copyOf(List<Boolean> source) {
    Objects.requireNonNull(source, "source");
    if (source instanceof MazeCellBits bits) return bits;
    int length = source.size();
    long[] words = new long[(int) (((long) length + 63L) >>> 6)];
    for (int index = 0; index < length; index++) {
      if (Objects.requireNonNull(source.get(index), "maze cell"))
        words[index >>> 6] |= 1L << (index & 63);
    }
    return new MazeCellBits(length, words);
  }

  MazeCellBits withOpened(int index) {
    Objects.checkIndex(index, length);
    int word = index >>> 6;
    long mask = 1L << (index & 63);
    if ((words[word] & mask) != 0L) return this;
    long[] updated = words.clone();
    updated[word] |= mask;
    return new MazeCellBits(length, updated);
  }

  static void forEachDifference(List<Boolean> before, List<Boolean> after,
      IntConsumer changedIndex) {
    MazeCellBits oldBits = copyOf(before);
    MazeCellBits newBits = copyOf(after);
    if (oldBits.length != newBits.length)
      throw new IllegalArgumentException("maze cell dimensions must match");
    for (int word = 0; word < oldBits.words.length; word++) {
      long changed = oldBits.words[word] ^ newBits.words[word];
      while (changed != 0L) {
        int index = (word << 6) + Long.numberOfTrailingZeros(changed);
        changedIndex.accept(index);
        changed &= changed - 1L;
      }
    }
  }

  @Override
  public Boolean get(int index) {
    Objects.checkIndex(index, length);
    return (words[index >>> 6] & (1L << (index & 63))) != 0L;
  }

  @Override
  public int size() {
    return length;
  }
}
