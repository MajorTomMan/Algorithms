package com.majortom.algorithms.visualization.execution;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Thread-safe bounded history for the desktop application lifetime. */
public final class InMemoryRunHistoryService implements RunHistoryService {

    private final RunHistoryPolicy policy;
    private final Deque<ClientExecutionRecord> records = new ArrayDeque<>();
    private long retainedEventCount;

    public InMemoryRunHistoryService(RunHistoryPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public synchronized void add(ClientExecutionRecord record) {
        Objects.requireNonNull(record, "record");
        records.addLast(record);
        retainedEventCount += record.recording().events().size();
        trimToPolicy();
    }

    @Override
    public synchronized Optional<ClientExecutionRecord> latest() {
        return Optional.ofNullable(records.peekLast());
    }

    @Override
    public synchronized List<ClientExecutionRecord> comparableWith(ClientExecutionRecord record) {
        Objects.requireNonNull(record, "record");
        List<ClientExecutionRecord> result = new ArrayList<>();
        for (ClientExecutionRecord candidate : records) {
            if (candidate.recording().runId().equals(record.recording().runId())) {
                continue;
            }
            if (!candidate.moduleId().equals(record.moduleId())) {
                continue;
            }
            if (!candidate.inputFingerprint().equals(record.inputFingerprint())) {
                continue;
            }
            result.add(candidate);
        }
        return List.copyOf(result);
    }

    @Override
    public synchronized List<ClientExecutionRecord> all() {
        return List.copyOf(records);
    }

    @Override
    public synchronized void clear() {
        records.clear();
        retainedEventCount = 0L;
    }

    private void trimToPolicy() {
        while (records.size() > policy.maximumRuns()
                || retainedEventCount > policy.maximumEvents()) {
            ClientExecutionRecord removed = records.removeFirst();
            retainedEventCount -= removed.recording().events().size();
        }
    }
}
