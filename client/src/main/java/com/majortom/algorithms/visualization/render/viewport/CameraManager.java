package com.majortom.algorithms.visualization.render.viewport;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;

/** Sole owner of factual camera policy resolution. */
public final class CameraManager {
    public CameraState resolve(
            CameraPolicy policy,
            BoundsSnapshot content,
            ViewportSnapshot viewport,
            CameraState current,
            CameraState restored,
            boolean userControlled,
            double minScale,
            double maxScale) {
        if ((policy == CameraPolicy.RESTORE || policy == CameraPolicy.RESTORE_OR_FIT_IF_READABLE)
                && restored != null) return restored;
        if (policy == CameraPolicy.KEEP || content == null || content.isEmpty()) return current;

        // Explicit toolbar/shortcut commands always win over the userControlled latch.
        if (policy == CameraPolicy.FIT_CONTENT) return fit(content, viewport, minScale, maxScale);
        if (policy == CameraPolicy.CENTER) return center(content, viewport, current.scale());
        if (policy == CameraPolicy.RESET) {
            return center(content, viewport, clamp(CameraScale.DEFAULT, minScale, maxScale));
        }

        if (policy == CameraPolicy.FIT_IF_READABLE
                || policy == CameraPolicy.RESTORE_OR_FIT_IF_READABLE
                || policy == CameraPolicy.ENSURE_VISIBLE_IF_READABLE) {
            if (policy == CameraPolicy.ENSURE_VISIBLE_IF_READABLE && userControlled) return current;
            // Fit only when the actual content fits at its default readable scale.
            if (!fitsAtDefaultScale(content, viewport)) return current;
            if (policy == CameraPolicy.ENSURE_VISIBLE_IF_READABLE
                    && fullyVisible(content, viewport, current)) return current;
            return fit(content, viewport, minScale, maxScale);
        }
        if (userControlled) return current;
        if (policy == CameraPolicy.ENSURE_VISIBLE && fullyVisible(content, viewport, current)) return current;
        return fit(content, viewport, minScale, maxScale);
    }

    public CameraState fit(
            BoundsSnapshot content, ViewportSnapshot viewport, double minScale, double maxScale) {
        double scaleX = content.width() > 0.0d ? viewport.usableWidth() / content.width() : maxScale;
        double scaleY = content.height() > 0.0d ? viewport.usableHeight() / content.height() : maxScale;
        double scale = clamp(Math.min(scaleX, scaleY), minScale, maxScale);
        return center(content, viewport, scale);
    }

    public CameraState center(BoundsSnapshot content, ViewportSnapshot viewport, double scale) {
        double tx = viewport.safeCenterX() - content.centerX() * scale;
        double ty = viewport.safeCenterY() - content.centerY() * scale;
        return new CameraState(scale, tx, ty);
    }

    private static boolean fitsAtDefaultScale(BoundsSnapshot content, ViewportSnapshot viewport) {
        return viewport.width() > 0.0d && viewport.height() > 0.0d
                && content.width() * CameraScale.DEFAULT <= viewport.usableWidth()
                && content.height() * CameraScale.DEFAULT <= viewport.usableHeight();
    }

    private boolean fullyVisible(BoundsSnapshot content, ViewportSnapshot viewport, CameraState camera) {
        double minX = content.minX() * camera.scale() + camera.translateX();
        double maxX = content.maxX() * camera.scale() + camera.translateX();
        double minY = content.minY() * camera.scale() + camera.translateY();
        double maxY = content.maxY() * camera.scale() + camera.translateY();
        return minX >= viewport.insets().left() && maxX <= viewport.width() - viewport.insets().right()
                && minY >= viewport.insets().top() && maxY <= viewport.height() - viewport.insets().bottom();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
