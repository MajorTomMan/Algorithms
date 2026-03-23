package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;

public interface ExecutionControl<S extends BaseStructure<?>> {
    void acceptFrame(ExecutionFrame<S> frame, boolean awaitStep);

    boolean isCancelled();

    long getDelayMillis();
}
