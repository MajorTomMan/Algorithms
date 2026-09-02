package com.majortom.algorithms.visualization.structure;

import com.majortom.algorithms.core.snapshot.StructureSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded session-local snapshot storage for the JavaFX client.
 *
 * <p>Snapshots are grouped by module and newest entries are returned first.
 * This class intentionally has no file, JSON, or server concern.</p>
 */
public final class InMemoryStructureSnapshotStore {

    public static final int DEFAULT_MAX_SNAPSHOTS_PER_MODULE = 8;

    private final int maxSnapshotsPerModule;
    private final Map<String, List<StructureSnapshot<?>>> snapshotsByModule = new LinkedHashMap<>();

    public InMemoryStructureSnapshotStore() {
        this(DEFAULT_MAX_SNAPSHOTS_PER_MODULE);
    }

    public InMemoryStructureSnapshotStore(int maxSnapshotsPerModule) {
        if (maxSnapshotsPerModule < 1) {
            throw new IllegalArgumentException("maxSnapshotsPerModule must be positive");
        }
        this.maxSnapshotsPerModule = maxSnapshotsPerModule;
    }

    public int maxSnapshotsPerModule() {
        return maxSnapshotsPerModule;
    }

    /** Saves a snapshot, keeping the newest entries first. */
    public void save(StructureSnapshot<?> snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<StructureSnapshot<?>> snapshots = snapshotsByModule.computeIfAbsent(
                snapshot.moduleId(), ignored -> new ArrayList<>());
        snapshots.removeIf(existing -> existing.id().equals(snapshot.id()));
        snapshots.add(0, snapshot);
        if (snapshots.size() > maxSnapshotsPerModule) {
            snapshots.subList(maxSnapshotsPerModule, snapshots.size()).clear();
        }
    }

    /** Returns a defensive, newest-first view for one module. */
    public List<StructureSnapshot<?>> snapshots(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return List.of();
        }
        return List.copyOf(snapshotsByModule.getOrDefault(moduleId, List.of()));
    }

    public void clear(String moduleId) {
        if (moduleId != null) {
            snapshotsByModule.remove(moduleId);
        }
    }
}
