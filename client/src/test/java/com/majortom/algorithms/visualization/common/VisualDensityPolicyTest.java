package com.majortom.algorithms.visualization.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class VisualDensityPolicyTest {
    @Test
    void arraysUseOneSharedDensityBoundary() {
        assertEquals(VisualDensity.DETAIL, VisualDensityPolicy.array(16));
        assertEquals(VisualDensity.COMPACT, VisualDensityPolicy.array(17));
        assertEquals(VisualDensity.COMPACT, VisualDensityPolicy.array(40));
        assertEquals(VisualDensity.DENSE, VisualDensityPolicy.array(41));
    }

    @Test
    void stringsUseOneSharedDensityBoundary() {
        assertEquals(VisualDensity.DETAIL, VisualDensityPolicy.string(24));
        assertEquals(VisualDensity.COMPACT, VisualDensityPolicy.string(25));
        assertEquals(VisualDensity.COMPACT, VisualDensityPolicy.string(48));
        assertEquals(VisualDensity.DENSE, VisualDensityPolicy.string(49));
    }
}
