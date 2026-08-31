package com.majortom.algorithms.visualization.execution;

import com.majortom.algorithms.core.api.AlgorithmInput;

/** Creates a stable identity for normalized input data. */
public interface InputFingerprint {

    String fingerprint(AlgorithmInput input);
}
