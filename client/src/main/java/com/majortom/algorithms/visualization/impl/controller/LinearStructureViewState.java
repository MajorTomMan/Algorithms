package com.majortom.algorithms.visualization.impl.controller;

import java.util.List;

public record LinearStructureViewState(String kind, List<Integer> values) {
    public LinearStructureViewState {
        values = List.copyOf(values);
    }
}
