package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;

/** Versioned identity of a scheduled render operation. */
record RenderTransaction(long id, RenderSessionId sessionId, long generation,
    long modelRevision, long geometryRevision) {}
