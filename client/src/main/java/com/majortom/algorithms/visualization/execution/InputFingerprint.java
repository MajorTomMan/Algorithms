package com.majortom.algorithms.visualization.execution;


/** Creates a stable identity for normalized input data. */
public interface InputFingerprint {

    String fingerprint(Object input);
}
