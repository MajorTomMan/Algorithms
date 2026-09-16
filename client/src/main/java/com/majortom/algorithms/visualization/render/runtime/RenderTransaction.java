package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderIntentKind;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;

record RenderTransaction(long id, RenderSessionId sessionId, RenderIntentKind kind, long generation,
    long modelRevision, long geometryRevision, long startedAtNanos) {}
