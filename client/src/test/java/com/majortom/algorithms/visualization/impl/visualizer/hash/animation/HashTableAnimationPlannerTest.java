package com.majortom.algorithms.visualization.impl.visualizer.hash.animation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.impl.visualizer.hash.HashVisualIds;
import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.hash.HashTableViewState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HashTableAnimationPlannerTest {

  @Test
  void rehashMovesStableEntryAndRebuildsBucketLink() {
    long entryId = 1L;
    String entry = HashVisualIds.entry(entryId);
    String oldBucket = HashVisualIds.bucket(0);
    String newBucket = HashVisualIds.bucket(3);
    String oldLink = HashVisualIds.link(oldBucket, entry);
    String newLink = HashVisualIds.link(newBucket, entry);

    HashTableViewState previous = new HashTableViewState(
        2,
        List.of(
            new HashTableViewState.Bucket(0, List.of(hashEntry(entryId, "alpha", 10))),
            new HashTableViewState.Bucket(1, List.of())),
        HashTableViewState.Mutation.none(),
        false);

    HashTableViewState next = new HashTableViewState(
        4,
        List.of(
            new HashTableViewState.Bucket(0, List.of()),
            new HashTableViewState.Bucket(1, List.of()),
            new HashTableViewState.Bucket(2, List.of()),
            new HashTableViewState.Bucket(3, List.of(hashEntry(entryId, "alpha", 10)))),
        HashTableViewState.Mutation.rehashed(),
        false);

    LayoutPatch before = new LayoutPatch(
        1L,
        Map.of(
            oldBucket, geometry(oldBucket, 34, 34),
            HashVisualIds.bucket(1), geometry(HashVisualIds.bucket(1), 34, 108),
            entry, geometry(entry, 140, 34)),
        List.of(),
        new BoundsSnapshot(34, 34, 240, 126));

    LayoutPatch after = new LayoutPatch(
        2L,
        Map.of(
            HashVisualIds.bucket(0), geometry(HashVisualIds.bucket(0), 34, 34),
            HashVisualIds.bucket(1), geometry(HashVisualIds.bucket(1), 34, 108),
            HashVisualIds.bucket(2), geometry(HashVisualIds.bucket(2), 34, 182),
            newBucket, geometry(newBucket, 34, 256),
            entry, geometry(entry, 140, 256)),
        List.of(),
        new BoundsSnapshot(34, 34, 240, 274));

    var plan = new HashTableAnimationPlanner().plan(previous, before, next, after);

    assertTrue(plan.contains(AnimationStep.NodeMove.class, entry));
    assertTrue(plan.contains(AnimationStep.EdgeRemove.class, oldLink));
    assertTrue(plan.contains(AnimationStep.EdgeCreate.class, newLink));
  }

  private static HashTableViewState.Entry hashEntry(
      long id, Object key, Object value) {
    return new HashTableViewState.Entry(id, VisualValue.of(key), VisualValue.of(value));
  }

  private static ElementGeometry geometry(String id, double x, double y) {
    return new ElementGeometry(id, x, y, 64, 52);
  }
}
