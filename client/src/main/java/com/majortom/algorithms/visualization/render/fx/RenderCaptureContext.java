package com.majortom.algorithms.visualization.render.fx;

import com.majortom.algorithms.visualization.render.api.LayoutRequestId;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;

public record RenderCaptureContext(
        long transactionId,
        RenderSessionId sessionId,
        long generation,
        long modelRevision,
        long geometryRevision,
        LayoutRequestId requestId,
        boolean initialFrame) {}
