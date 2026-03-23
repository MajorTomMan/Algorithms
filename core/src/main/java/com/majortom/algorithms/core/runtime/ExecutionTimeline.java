package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExecutionTimeline<S extends BaseStructure<?>> {

    private final List<ExecutionFrame<S>> frames = new ArrayList<>();

    public void add(ExecutionFrame<S> frame) {
        frames.add(frame);
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public int size() {
        return frames.size();
    }

    public ExecutionFrame<S> get(int index) {
        return frames.get(index);
    }

    public List<ExecutionFrame<S>> frames() {
        return Collections.unmodifiableList(frames);
    }
}
