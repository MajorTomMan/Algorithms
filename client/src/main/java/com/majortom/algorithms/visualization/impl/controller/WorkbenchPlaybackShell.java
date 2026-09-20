package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.international.I18N;
import java.util.List;
import java.util.function.Supplier;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Region;

/** Owns playback shell controls, speed choices and timeline status presentation. */
final class WorkbenchPlaybackShell {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private final Button jumpStartBtn, jumpEndBtn, endExecutionBtn, closeExecutionBtn;
    private final Button speed1Btn, speed2Btn, speed4Btn, speed8Btn, speed16Btn;
    private final Slider delaySlider;
    private final Label timelineStatusLabel;
    private final Region timelineStatusDot;
    private final Supplier<BaseController<?>> controller;
    private final Runnable refreshExecution;
    private final Runnable refreshContext;

    WorkbenchPlaybackShell(Button jumpStartBtn, Button jumpEndBtn, Button endExecutionBtn, Button closeExecutionBtn,
            Button speed1Btn, Button speed2Btn, Button speed4Btn, Button speed8Btn, Button speed16Btn,
            Slider delaySlider, Label timelineStatusLabel, Region timelineStatusDot,
            Supplier<BaseController<?>> controller, Runnable refreshExecution, Runnable refreshContext) {
        this.jumpStartBtn = jumpStartBtn;
        this.jumpEndBtn = jumpEndBtn;
        this.endExecutionBtn = endExecutionBtn;
        this.closeExecutionBtn = closeExecutionBtn;
        this.speed1Btn = speed1Btn;
        this.speed2Btn = speed2Btn;
        this.speed4Btn = speed4Btn;
        this.speed8Btn = speed8Btn;
        this.speed16Btn = speed16Btn;
        this.delaySlider = delaySlider;
        this.timelineStatusLabel = timelineStatusLabel;
        this.timelineStatusDot = timelineStatusDot;
        this.controller = controller;
        this.refreshExecution = refreshExecution;
        this.refreshContext = refreshContext;
    }

    void install() {
        setupPlaybackSpeedButtons();
        setupPlaybackShellActions();
    }

    private void setupPlaybackSpeedButtons() {
        bindSpeedButton(speed1Btn, 1.0d, 50.0d);
        bindSpeedButton(speed2Btn, 2.0d, 25.0d);
        bindSpeedButton(speed4Btn, 4.0d, 12.5d);
        bindSpeedButton(speed8Btn, 8.0d, 6.0d);
        bindSpeedButton(speed16Btn, 16.0d, 0.0d);
        setSelectedSpeed(speed1Btn);
    }

    private void setupPlaybackShellActions() {
        if (jumpStartBtn != null) {
            jumpStartBtn.setOnAction(event -> {
                if (controller.get() != null && controller.get().jumpToStart()) {
                    refreshExecution.run();
                }
            });
        }
        if (jumpEndBtn != null) {
            jumpEndBtn.setOnAction(event -> {
                if (controller.get() != null && controller.get().jumpToEnd()) {
                    refreshExecution.run();
                }
            });
        }
        if (endExecutionBtn != null) {
            endExecutionBtn.setOnAction(event -> {
                if (controller.get() != null) {
                    controller.get().endAlgorithm();
                    refreshExecution.run();
                }
            });
        }
        if (closeExecutionBtn != null) {
            closeExecutionBtn.setOnAction(event -> {
                if (controller.get() != null) {
                    controller.get().closeExecution();
                    refreshExecution.run();
                    refreshContext.run();
                }
            });
        }
        refresh();
    }

    void refresh() {
        boolean available = controller.get() != null;
        boolean running = available && controller.get().isRunning();
        boolean hasTimeline = available && controller.get().hasExecutionData();
        if (jumpStartBtn != null) jumpStartBtn.setDisable(running || !hasTimeline);
        if (jumpEndBtn != null) jumpEndBtn.setDisable(running || !hasTimeline);
        if (endExecutionBtn != null) endExecutionBtn.setDisable(!running);
        if (closeExecutionBtn != null) closeExecutionBtn.setDisable(!running && !hasTimeline);
        refreshTimelineStatus();
    }

    void refreshTimelineStatus() {
        if (timelineStatusLabel == null) {
            return;
        }
        boolean available = controller.get() != null;
        boolean running = available && controller.get().isRunning();
        boolean replaying = available && controller.get().isPlaybackPlaying();
        boolean paused = available && controller.get().isPaused();
        boolean hasTimeline = available && controller.get().hasExecutionData();

        String key;
        String tone;
        if (paused && (running || replaying || hasTimeline)) {
            key = "status.workspace.paused";
            tone = "timeline-status-paused";
        } else if (running) {
            key = "status.workspace.running";
            tone = "timeline-status-running";
        } else if (replaying) {
            key = "status.workspace.playing";
            tone = "timeline-status-playing";
        } else if (hasTimeline) {
            key = "status.workspace.completed";
            tone = "timeline-status-completed";
        } else {
            key = "status.workspace.ready";
            tone = "timeline-status-ready";
        }
        timelineStatusLabel.setText(I18N.text(key));
        if (timelineStatusDot != null) {
            timelineStatusDot.getStyleClass().removeAll(
                    "timeline-status-running",
                    "timeline-status-playing",
                    "timeline-status-paused",
                    "timeline-status-completed",
                    "timeline-status-ready");
            timelineStatusDot.getStyleClass().add(tone);
        }
    }

    private void bindSpeedButton(Button button, double speed, double delayMillis) {
        if (button == null) {
            return;
        }
        button.setOnAction(event -> {
            if (delaySlider != null) {
                delaySlider.setValue(delayMillis);
            }
            if (controller.get() != null && controller.get().getVisualizer() != null) {
                controller.get().getVisualizer().setPlaybackSpeed(speed);
                controller.get().getVisualizer().setScrubbing(speed >= 16.0d);
            }
            setSelectedSpeed(button);
        });
    }

    private void setSelectedSpeed(Button selected) {
        for (Button button : List.of(speed1Btn, speed2Btn, speed4Btn, speed8Btn, speed16Btn)) {
            if (button != null) {
                button.pseudoClassStateChanged(SELECTED, button == selected);
            }
        }
    }

}
