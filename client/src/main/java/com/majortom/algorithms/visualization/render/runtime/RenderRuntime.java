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
import com.majortom.algorithms.visualization.impl.visualizer.hash.HashTableLayout;
import com.majortom.algorithms.visualization.impl.visualizer.linked.LinkedListLayout;
import com.majortom.algorithms.visualization.impl.visualizer.tree.TreeElkLayout;

/**
 * 桌面进程共享渲染服务的组合根（Composition Root）。
 *
 * <p>在这里组装 RenderScheduler、LayoutCoordinator、各种 LayoutEngine、
 * FX 提交适配器、SurfaceRegistry、CameraManager 与诊断追踪，
 * 并向模块注入 RenderContext。结构控制器不应自行再搭建一套布局线程池。
 *
 * <p>渲染与算法执行各有独立的调度/时间控制：
 * 真实事实按 ExecutionRuntime 序列记录，
 * DefaultRenderFramework 决定这些状态何时被布局和真正绘制。
 */
public final class RenderRuntime {
    private static final FxExecutor FX = FxDispatch.executor();
    private static final RenderTrace TRACE = new RenderTrace();
    private static final RenderClock CLOCK = new RenderClock();
    private static final DefaultRenderFramework SHARED = new DefaultRenderFramework(
            new RenderScheduler(),
            new LayoutCoordinator(
                    new LayoutExecutor(Math.max(
                            2,
                            Math.min(4, Runtime.getRuntime().availableProcessors() / 2))),
                    new LayoutEngineRegistry()
                            .register(new LinearLayoutEngine())
                            .register(new FixedLayoutEngine())
                            .register(new GraphElkLayout())
                            .register(new TreeElkLayout())
                            .register(new LinkedListLayout())
                            .register(new HashTableLayout())),
            FX,
            new RenderSurfaceRegistry(),
            new FxPresentationSurfaceRegistry(),
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
