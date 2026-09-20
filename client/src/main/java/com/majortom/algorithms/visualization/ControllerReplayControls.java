package com.majortom.algorithms.visualization;

import com.majortom.algorithms.visualization.render.fx.FxDispatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Slider;

/** Owns playback slider listeners, delayed-event pacing and scrub generations. */
final class ControllerReplayControls<S> {
    private final BaseVisualizer<S> visualizer;
    private final BooleanSupplier running;
    private final BooleanSupplier hasPlaybackData;
    private final DoubleConsumer onDelayChanged;
    private final DoubleConsumer onDragSeek;
    private final AtomicLong livePlaybackDelayMillis = new AtomicLong(50L);
    private Slider delaySlider;
    private Slider timelineSlider;
    private boolean updatingTimelineSlider;
    private boolean dragging;
    private long scrubGeneration;

    private final ChangeListener<Number> delayListener = (observable, before, value) -> {
        livePlaybackDelayMillis.set(Math.max(0L, value.longValue()));
        onDelayChanged.accept(value.doubleValue());
    };
    private final ChangeListener<Number> timelineListener = (observable, before, value) -> {
        if (!updatingTimelineSlider && !running.getAsBoolean()
                && timelineSlider != null && timelineSlider.isValueChanging()) {
            onDragSeek.accept(value.doubleValue());
        }
    };
    private final ChangeListener<Boolean> changingListener = (observable, before, changing) -> {
        if (updatingTimelineSlider || running.getAsBoolean() || timelineSlider == null) return;
        if (Boolean.TRUE.equals(changing)) {
            dragging = true;
            beginScrubbing();
            return;
        }
        if (dragging) {
            dragging = false;
            onDragSeek.accept(timelineSlider.getValue());
            releaseScrubbingAfterQueuedRender(scrubGeneration);
        }
    };

    ControllerReplayControls(BaseVisualizer<S> visualizer, BooleanSupplier running,
            BooleanSupplier hasPlaybackData, DoubleConsumer onDelayChanged, DoubleConsumer onDragSeek) {
        this.visualizer = visualizer;
        this.running = running;
        this.hasPlaybackData = hasPlaybackData;
        this.onDelayChanged = onDelayChanged;
        this.onDragSeek = onDragSeek;
    }

    void bind(Slider delay, Slider timeline) {
        dispose();
        delaySlider = delay;
        timelineSlider = timeline;
        if (delaySlider != null) {
            livePlaybackDelayMillis.set(Math.max(0L, Math.round(delaySlider.getValue())));
            delaySlider.valueProperty().addListener(delayListener);
        }
        if (timelineSlider != null) {
            timelineSlider.setDisable(true);
            timelineSlider.valueProperty().addListener(timelineListener);
            timelineSlider.valueChangingProperty().addListener(changingListener);
        }
    }

    long liveDelayMillis() { return livePlaybackDelayMillis.get(); }
    long scrubGeneration() { return scrubGeneration; }
    boolean isSliderChanging() { return timelineSlider != null && timelineSlider.isValueChanging(); }

    void beginScrubbing() {
        scrubGeneration++;
        if (visualizer != null) visualizer.setScrubbing(true);
    }

    /** A stale deferred release must not end a newer drag or seek. */
    void releaseScrubbingAfterQueuedRender(long generation) {
        BaseVisualizer<S> target = visualizer;
        if (target == null) return;
        FxDispatch.defer(() -> {
            if (generation != scrubGeneration || dragging) return;
            target.setScrubbing(false);
        });
    }

    void prepareTimelineControls() {
        if (timelineSlider == null) return;
        timelineSlider.setDisable(!hasPlaybackData.getAsBoolean());
        updatingTimelineSlider = true;
        try { timelineSlider.setValue(hasPlaybackData.getAsBoolean() ? 1.0d : 0.0d); }
        finally { updatingTimelineSlider = false; }
    }

    void syncTimelineSlider(int index, int size) {
        if (timelineSlider == null) return;
        updatingTimelineSlider = true;
        try {
            timelineSlider.setValue(size > 1 ? (double) index / (double) (size - 1) : 0.0d);
            timelineSlider.setDisable(running.getAsBoolean() || !hasPlaybackData.getAsBoolean());
        } finally { updatingTimelineSlider = false; }
    }

    void clearTimeline() {
        if (timelineSlider == null) return;
        timelineSlider.setDisable(true);
        updatingTimelineSlider = true;
        try { timelineSlider.setValue(0.0d); }
        finally { updatingTimelineSlider = false; }
    }

    void dispose() {
        if (delaySlider != null) delaySlider.valueProperty().removeListener(delayListener);
        if (timelineSlider != null) {
            timelineSlider.valueProperty().removeListener(timelineListener);
            timelineSlider.valueChangingProperty().removeListener(changingListener);
        }
        delaySlider = null;
        timelineSlider = null;
    }
}
