package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.HashTableSnapshot;

public record HashTableViewState(HashTableSnapshot<String, Integer> table, String focusKey, boolean found) {
    public static HashTableViewState source(HashTableSnapshot<String, Integer> table) {
        return new HashTableViewState(table, null, false);
    }
}
