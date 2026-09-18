package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.diagnostics.RenderTrace;
import com.majortom.algorithms.visualization.render.fx.FxDispatch;
import com.majortom.algorithms.visualization.render.fx.FxExecutor;
import com.majortom.algorithms.visualization.render.fx.RenderSurfaceRegistry;
import com.majortom.algorithms.visualization.render.fx.FxPresentationSurfaceRegistry;
import com.majortom.algorithms.visualization.render.api.PresentationSurfacePort;
import com.majortom.algorithms.visualization.render.layout.LayoutEngineRegistry;
import com.majortom.algorithms.visualization.render.layout.FixedLayoutEngine;
import com.majortom.algorithms.visualization.render.layout.LinearLayoutEngine;
import com.majortom.algorithms.visualization.render.timing.RenderClock;
import com.majortom.algorithms.visualization.render.viewport.CameraManager;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.linked.LinkedListLayout;
import com.majortom.algorithms.visualization.impl.visualizer.tree.TreeElkLayout;

public final class RenderRuntime {
    private static final FxExecutor FX = FxDispatch.executor();
    private static final RenderTrace TRACE = new RenderTrace();
    private static final RenderClock CLOCK = new RenderClock();
    private static final DefaultRenderFramework SHARED = new DefaultRenderFramework(
            new RenderScheduler(),
            new LayoutExecutor(Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2))),
            FX,
            new RenderSurfaceRegistry(),
            new FxPresentationSurfaceRegistry(),
            new LayoutEngineRegistry()
                    .register(new LinearLayoutEngine())
                    .register(new FixedLayoutEngine())
                    .register(new GraphElkLayout())
                    .register(new TreeElkLayout())
                    .register(new LinkedListLayout()),
            new CameraManager(),
            TRACE);
    private static final RenderSurfaceHost SURFACE_HOST = new RenderSurfaceHost(SHARED);
    private static final RenderContext CONTEXT = new RenderContext(SHARED, SURFACE_HOST);

    private RenderRuntime() { }
    public static DefaultRenderFramework shared() { return SHARED; }
    public static RenderContext context() { return CONTEXT; }
    public static PresentationSurfacePort presentationSurfaces() { return SHARED; }
    public static RenderClock clock() { return CLOCK; }
}
