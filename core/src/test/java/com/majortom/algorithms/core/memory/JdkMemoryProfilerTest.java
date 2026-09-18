package com.majortom.algorithms.core.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JdkMemoryProfilerTest {
  @Test
  void profilesOnlyTheExplicitExecutionScopeAndProducesSerializableSnapshots() throws Exception {
    JdkMemoryProfiler profiler = new JdkMemoryProfiler(5L);
    MemoryProfileSession session = profiler.begin(MemoryDomain.ALGORITHM, "array/bubble-sort");
    List<byte[]> retained = new ArrayList<>();
    for (int index = 0; index < 32; index++) {
      retained.add(new byte[8 * 1024]);
    }
    Thread.sleep(15L);
    MemoryProfile live = session.snapshot();
    assertFalse(live.complete());
    session.close();
    MemoryProfile completed = session.snapshot();

    assertTrue(completed.complete());
    assertEquals(MemoryDomain.ALGORITHM, completed.sessionId().domain());
    assertEquals("array/bubble-sort", completed.sessionId().scopeId());
    assertNotNull(completed.capabilities());
    assertTrue(completed.durationNanos() > 0L);
    if (completed.capabilities().threadAllocatedBytes()) {
      assertTrue(completed.allocatedBytesValue().isPresent());
      assertTrue(completed.allocatedBytesValue().getAsLong() >= 256 * 1024L);
      assertFalse(completed.samples().isEmpty());
    }

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
      output.writeObject(completed);
    }
    assertTrue(bytes.size() > 0);
    assertEquals(32, retained.size());
  }

  @Test
  void storeKeepsBoundedHistoryPerDomainAndScope() {
    MemoryProfileStore store = new MemoryProfileStore(2);
    MemoryCapabilities capabilities = new MemoryCapabilities(true, true, true, true);
    for (long sequence = 1; sequence <= 3; sequence++) {
      store.record(new MemoryProfile(
          new MemorySessionId(sequence, MemoryDomain.STRUCTURE, "graph/add-edge"),
          capabilities,
          true,
          true,
          sequence,
          sequence * 10L,
          1L,
          1L,
          0L,
          0L,
          0L,
          List.of()));
    }

    List<MemoryProfile> history = store.history(MemoryDomain.STRUCTURE, "graph/add-edge");
    assertEquals(2, history.size());
    assertEquals(2L, history.getFirst().sessionId().sequence());
    assertEquals(3L, store.latest(MemoryDomain.STRUCTURE, "graph/add-edge")
                        .orElseThrow().sessionId().sequence());
  }

  @Test
  void storeCanResolveNewestProfileWithinCurrentStructureScope() {
    MemoryProfileStore store = new MemoryProfileStore();
    MemoryCapabilities capabilities = new MemoryCapabilities(true, true, true, true);
    store.record(profile(1L, MemoryDomain.STRUCTURE, "tree/add-child", capabilities));
    store.record(profile(2L, MemoryDomain.STRUCTURE, "avl-tree/insert", capabilities));
    store.record(profile(3L, MemoryDomain.STRUCTURE, "tree/delete", capabilities));

    assertEquals("tree/delete", store.latestByScopePrefix(MemoryDomain.STRUCTURE, "tree/")
        .orElseThrow().sessionId().scopeId());
    assertEquals("avl-tree/insert", store.latestByScopePrefix(MemoryDomain.STRUCTURE, "avl-tree/")
        .orElseThrow().sessionId().scopeId());
  }

  @Test
  void deepStoreKeepsAlgorithmAndStructureVariantsIsolated() {
    MemoryDeepProfileStore store = new MemoryDeepProfileStore();
    store.record(new MemoryDeepProfile(
        MemoryDomain.ALGORITHM, "tree/inorder", 100L, 1L, List.of(), List.of()));
    store.record(new MemoryDeepProfile(
        MemoryDomain.ALGORITHM, "tree/preorder", 200L, 1L, List.of(), List.of()));
    store.record(new MemoryDeepProfile(
        MemoryDomain.STRUCTURE, "avl-tree/insert", 300L, 1L, List.of(), List.of()));
    store.record(new MemoryDeepProfile(
        MemoryDomain.STRUCTURE, "tree/delete", 400L, 1L, List.of(), List.of()));

    assertEquals(100L, store.latest(MemoryDomain.ALGORITHM, "tree/inorder")
        .orElseThrow().estimatedBytes());
    assertEquals(200L, store.latest(MemoryDomain.ALGORITHM, "tree/preorder")
        .orElseThrow().estimatedBytes());
    assertEquals(300L, store.latestByScopePrefix(MemoryDomain.STRUCTURE, "avl-tree/")
        .orElseThrow().estimatedBytes());
    assertEquals(400L, store.latestByScopePrefix(MemoryDomain.STRUCTURE, "tree/")
        .orElseThrow().estimatedBytes());
  }

  private static MemoryProfile profile(
      long sequence, MemoryDomain domain, String scopeId, MemoryCapabilities capabilities) {
    return new MemoryProfile(
        new MemorySessionId(sequence, domain, scopeId), capabilities, true, true,
        sequence, sequence, 1L, 1L, 0L, 0L, 0L, List.of());
  }
}
