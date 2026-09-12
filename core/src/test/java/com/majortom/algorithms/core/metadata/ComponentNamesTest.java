package com.majortom.algorithms.core.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentNamesTest {
    @Test
    void formatsClassNamesWithoutOwningComponentMetadata() {
        assertEquals("Quick Sort", ComponentNames.fromClassName("QuickSort"));
        assertEquals("KMP Search", ComponentNames.fromClassName("KMPSearch"));
        assertEquals("搜索插入位置", ComponentNames.fromClassName("搜索插入位置"));
    }

    @Test
    void explicitMetadataWinsOverFallback() {
        assertEquals("Two Sum", ComponentNames.resolve("Two Sum", ComponentNamesTest.class));
    }
}
