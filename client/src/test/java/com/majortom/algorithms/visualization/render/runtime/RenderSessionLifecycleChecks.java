package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;

/** Lifecycle regression: simplifying storage must not change stale-render generation rules. */
public final class RenderSessionLifecycleChecks {
    public static void main(String[] args) {
        RenderSessionId id = RenderSessionId.of("audit-session");
        RenderSession session = new RenderSession(id);
        check(!session.active() && session.generation == 0L, "new sessions are inactive");

        session.activate();
        check(session.active() && session.generation == 1L, "activation changes generation");
        session.activate();
        check(session.active() && session.generation == 1L, "repeated activation is idempotent");

        session.deactivate();
        check(!session.active() && session.generation == 2L, "deactivation invalidates transactions");
        session.deactivate();
        check(!session.active() && session.generation == 3L,
                "repeated deactivation still invalidates transactions");

        session.activate();
        check(session.active() && session.generation == 4L, "reactivation increments generation");
        System.out.println("RenderSessionLifecycleChecks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
