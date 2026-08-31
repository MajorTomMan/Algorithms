package com.majortom.algorithms.visualization;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;

/** Shared controls supplied by the main JavaFX shell to one module controller. */
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
        Button exportButton,
        Button compareButton) {
}
