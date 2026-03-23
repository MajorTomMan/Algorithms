package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutionArchive {

    private final List<ExecutionRecord<? extends BaseStructure<?>>> records = new ArrayList<>();

    public synchronized void add(ExecutionRecord<? extends BaseStructure<?>> record) {
        records.add(record);
    }

    public synchronized List<ExecutionRecord<? extends BaseStructure<?>>> all() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    public synchronized List<ExecutionRecord<? extends BaseStructure<?>>> comparableRecords(
            ExecutionRecord<? extends BaseStructure<?>> current) {
        return records.stream()
                .filter(record -> record != current)
                .filter(record -> record.moduleId().equals(current.moduleId()))
                .filter(record -> record.inputSignature().equals(current.inputSignature()))
                .collect(Collectors.toList());
    }
}
