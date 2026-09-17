package com.majortom.algorithms.core.metadata;

import java.util.Arrays;

/** Stable workbench/domain grouping derived from a Structure contract. */
public enum StructureModule {
  ARRAY(StructureIds.ARRAY),
  LINKED_LIST(StructureIds.LINKED_LIST),
  STACK(StructureIds.STACK),
  QUEUE(StructureIds.QUEUE),
  TREE(StructureIds.TREE),
  GRAPH(StructureIds.GRAPH),
  STRING(StructureIds.STRING),
  MAZE(StructureIds.MAZE),
  HASH(StructureIds.HASH);

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
