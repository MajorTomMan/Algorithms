package com.majortom.algorithms.visualization.runtime.maze;

import com.majortom.algorithms.structure.maze.GridPoint;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;

/** Immutable grid-indexed visited cells, using compact snapshots for replay. */
final class MazeVisitedBits extends AbstractSet<GridPoint> {
  private final int rows;
  private final int columns;
  private final long[] words;
  private final int count;

  private MazeVisitedBits(int rows, int columns, long[] words, int count) {
    this.rows = rows;
    this.columns = columns;
    this.words = words;
    this.count = count;
  }

  static Set<GridPoint> copyOf(int rows, int columns, Set<GridPoint> source) {
    Objects.requireNonNull(source, "source");
    if (source instanceof MazeVisitedBits bits && bits.rows == rows && bits.columns == columns)
      return bits;
    int length = Math.multiplyExact(rows, columns);
    long[] words = new long[(int) (((long) length + 63L) >>> 6)];
    int count = 0;
    for (GridPoint point : source) {
      Objects.requireNonNull(point, "visited point");
      if (!inside(rows, columns, point))
        return Set.copyOf(source);
      int index = point.row() * columns + point.column();
      words[index >>> 6] |= 1L << (index & 63);
      count++;
    }
    return new MazeVisitedBits(rows, columns, words, count);
  }

  static Set<GridPoint> withAdded(int rows, int columns, Set<GridPoint> source, GridPoint point) {
    Objects.requireNonNull(point, "visited point");
    if (!inside(rows, columns, point)) {
      Set<GridPoint> copy = new HashSet<>(source);
      copy.add(point);
      return Set.copyOf(copy);
    }
    Set<GridPoint> immutable = copyOf(rows, columns, source);
    if (!(immutable instanceof MazeVisitedBits bits)) {
      Set<GridPoint> copy = new HashSet<>(immutable);
      copy.add(point);
      return Set.copyOf(copy);
    }
    int index = point.row() * columns + point.column();
    int word = index >>> 6;
    long mask = 1L << (index & 63);
    if ((bits.words[word] & mask) != 0L) return bits;
    long[] updated = bits.words.clone();
    updated[word] |= mask;
    return new MazeVisitedBits(rows, columns, updated, bits.count + 1);
  }

  static void forEachDifference(int rows, int columns, Set<GridPoint> before,
      Set<GridPoint> after, IntConsumer changedIndex) {
    Set<GridPoint> oldValues = copyOf(rows, columns, before);
    Set<GridPoint> newValues = copyOf(rows, columns, after);
    if (oldValues instanceof MazeVisitedBits oldBits
        && newValues instanceof MazeVisitedBits newBits) {
      for (int word = 0; word < oldBits.words.length; word++) {
        long changed = oldBits.words[word] ^ newBits.words[word];
        while (changed != 0L) {
          changedIndex.accept((word << 6) + Long.numberOfTrailingZeros(changed));
          changed &= changed - 1L;
        }
      }
      return;
    }
    // Legacy or malformed observations can contain out-of-grid coordinates.
    // Skip those when deciding which visible grid cells need repainting.
    for (GridPoint point : oldValues) {
      if (inside(rows, columns, point) && !newValues.contains(point))
        changedIndex.accept(point.row() * columns + point.column());
    }
    for (GridPoint point : newValues) {
      if (inside(rows, columns, point) && !oldValues.contains(point))
        changedIndex.accept(point.row() * columns + point.column());
    }
  }

  private static boolean inside(int rows, int columns, GridPoint point) {
    return point.row() >= 0 && point.row() < rows
        && point.column() >= 0 && point.column() < columns;
  }

  @Override
  public boolean contains(Object candidate) {
    if (!(candidate instanceof GridPoint point) || !inside(rows, columns, point))
      return false;
    int index = point.row() * columns + point.column();
    return (words[index >>> 6] & (1L << (index & 63))) != 0L;
  }

  @Override
  public int size() {
    return count;
  }

  @Override
  public Iterator<GridPoint> iterator() {
    return new Iterator<>() {
      private int next = nextIndex(0);

      @Override
      public boolean hasNext() {
        return next >= 0;
      }

      @Override
      public GridPoint next() {
        if (next < 0) throw new NoSuchElementException();
        int index = next;
        next = nextIndex(index + 1);
        return new GridPoint(index / columns, index % columns);
      }
    };
  }

  private int nextIndex(int start) {
    int max = Math.multiplyExact(rows, columns);
    for (int index = start; index < max; index++) {
      if ((words[index >>> 6] & (1L << (index & 63))) != 0L)
        return index;
    }
    return -1;
  }
}
