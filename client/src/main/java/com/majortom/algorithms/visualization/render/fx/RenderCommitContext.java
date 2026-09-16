package com.majortom.algorithms.visualization.render.fx;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;

public record RenderCommitContext(
        long transactionId,
        RenderSessionId sessionId,
        long generation,
        long modelRevision,
        long layoutRevision,
        long presentationRevision,
        boolean initialFrame) {}
