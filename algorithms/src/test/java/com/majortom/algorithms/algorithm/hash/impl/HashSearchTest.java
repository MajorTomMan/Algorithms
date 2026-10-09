package com.majortom.algorithms.algorithm.hash.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.majortom.algorithms.structure.hash.ChainedHashTable;
import org.junit.jupiter.api.Test;

class HashSearchTest {
  @Test
  void returnsValueForAnExistingKey() {
    ChainedHashTable<Integer, Integer> table = new ChainedHashTable<>();
    table.put(7, 70);
    table.put(15, 150);
    assertEquals(70, new HashSearch().search(table));
    assertEquals(2, table.size());
  }

  @Test
  void reportsMissForEmptyTableWithoutModifyingIt() {
    ChainedHashTable<Integer, Integer> table = new ChainedHashTable<>();
    assertNull(new HashSearch().search(table));
    assertEquals(0, table.size());
  }
}
