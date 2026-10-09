package com.majortom.algorithms.visualization.impl.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Selection identity is independent of localization and the order of visible choices. */
class AlgorithmSelectorBinderTest {
    @Test
    void preservesStableIdWhenChoicesAreReordered() {
        assertEquals("dfs", AlgorithmSelectorBinder.preferredId(
                List.of("bfs", "dfs", "dijkstra"), "dfs"));
    }

    @Test
    void choosesFirstValidIdAfterTypeOrVariantChange() {
        assertEquals("sort", AlgorithmSelectorBinder.preferredId(
                List.of("sort", "search"), "old-algorithm"));
    }

    @Test
    void clearsSelectionWhenNoCompatibleAlgorithmExists() {
        assertNull(AlgorithmSelectorBinder.preferredId(List.of(), "dfs"));
    }

    @Test
    void usesFirstIdForInitialSelection() {
        assertEquals("hash-search", AlgorithmSelectorBinder.preferredId(
                List.of("hash-search"), null));
        assertNull(AlgorithmSelectorBinder.preferredId(List.of(), null));
    }

    @Test
    void nullPreviousSelectionIsSafeWithCopiedImmutableChoices() {
        List<String> ids = List.copyOf(List.of("hash-search", "hash-insert"));
        assertEquals("hash-search", AlgorithmSelectorBinder.preferredId(ids, null));
    }
}
