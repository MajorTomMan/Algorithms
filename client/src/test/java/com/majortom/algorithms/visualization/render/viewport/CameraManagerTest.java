package com.majortom.algorithms.visualization.render.viewport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import org.junit.jupiter.api.Test;

class CameraManagerTest {
    private final CameraManager manager = new CameraManager();
    private final ViewportSnapshot viewport = new ViewportSnapshot(
            500.0d, 300.0d, new ViewportInsets(16.0d, 16.0d, 60.0d, 16.0d));
    private final CameraState current = new CameraState(CameraScale.DEFAULT, 0.0d, 0.0d);

    @Test
    void fitDependsOnMeasuredContentAndUsableViewport() {
        BoundsSnapshot fits = new BoundsSnapshot(0.0d, 0.0d, 440.0d, 100.0d);
        assertEquals(468.0d / 440.0d,
                resolve(CameraPolicy.FIT_IF_READABLE, fits, current, null, false).scale(), 1e-9);
        BoundsSnapshot tooWide = new BoundsSnapshot(0.0d, 0.0d, 480.0d, 100.0d);
        assertEquals(current, resolve(CameraPolicy.FIT_IF_READABLE, tooWide, current, null, false));
        assertEquals(current, resolve(CameraPolicy.ENSURE_VISIBLE_IF_READABLE, tooWide, current, null, false));
    }

    @Test
    void restoreAndUserCameraArePreserved() {
        BoundsSnapshot fits = new BoundsSnapshot(0.0d, 0.0d, 200.0d, 100.0d);
        CameraState restored = new CameraState(2.0d, 50.0d, 80.0d);
        assertEquals(restored, resolve(CameraPolicy.RESTORE_OR_FIT_IF_READABLE, fits, current, restored, false));
        assertEquals(current, resolve(CameraPolicy.ENSURE_VISIBLE_IF_READABLE, fits, current, null, true));
    }

    @Test
    void explicitFitCanShrinkLongContentAndRespectsAutoFitMaximum() {
        assertEquals(0.5d, resolve(CameraPolicy.FIT_CONTENT,
                new BoundsSnapshot(0.0d, 0.0d, 936.0d, 100.0d), current, null, true).scale(), 1e-9);
        assertEquals(CameraScale.MAX_AUTO_FIT, resolve(CameraPolicy.FIT_CONTENT,
                new BoundsSnapshot(0.0d, 0.0d, 80.0d, 30.0d), current, null, false).scale(), 1e-9);
    }

    private CameraState resolve(CameraPolicy policy, BoundsSnapshot content, CameraState camera,
            CameraState restored, boolean userControlled) {
        return manager.resolve(policy, content, viewport, camera, restored, userControlled,
                CameraScale.MIN, CameraScale.MAX_AUTO_FIT);
    }
}
