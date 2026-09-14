package com.majortom.algorithms.core.metadata;

import java.util.Arrays;

/** Stable workbench/domain grouping derived from a Structure contract. */
public enum StructureModule {
    ARRAY("array"),
    LINKED_LIST("linked-list"),
    STACK("stack"),
    QUEUE("queue"),
    TREE("tree"),
    GRAPH("graph"),
    STRING("string"),
    MAZE("maze"),
    HASH("hash");

    private final String id;

    StructureModule(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static StructureModule fromId(String id) {
        return Arrays.stream(values())
                .filter(value -> value.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown structure module: " + id));
    }
}
