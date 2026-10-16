package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderPort;
import java.util.Objects;

/**
 * 注入单个工作台模块的渲染依赖集合。
 *
 * <p>RenderPort 接受渲染意图，RenderSurfaceHost 负责 FX Surface 的注册/
 * attach/detach。由 RenderRuntime.context() 统一提供，避免控制器自行 new
 * DefaultRenderFramework 并产生多个调度线程池。
 */
public record RenderContext(RenderPort renderPort, RenderSurfaceHost surfaceHost) {
    public RenderContext {
        Objects.requireNonNull(renderPort, "renderPort");
        Objects.requireNonNull(surfaceHost, "surfaceHost");
    }
}
