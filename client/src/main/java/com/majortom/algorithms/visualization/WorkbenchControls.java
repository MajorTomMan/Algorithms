package com.majortom.algorithms.visualization;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;

/**
 * Shared execution controls supplied by the dual-workspace JavaFX shell to one
 * module controller.
 *
 * <p>The module panel is loaded once and split by {@code MainController} into
 * structure and algorithm rails. The execution controls remain shared so a
 * single controller continues to own run, pause, replay, statistics and export
 * state.</p>
 */
public record WorkbenchControls(
        Label statsLabel,
        TextArea logArea,
        Slider delaySlider,
        Slider timelineSlider,
        HBox customControlBox,
        Button startButton,
        Button pauseButton,
        Button resetButton,
        Button replayButton,
        Button stepBackwardButton,
        Button stepForwardButton,
        Button exportButton,
        Button compareButton) {
}
