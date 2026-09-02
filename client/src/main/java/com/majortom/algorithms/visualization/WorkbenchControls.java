package com.majortom.algorithms.visualization;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import com.majortom.algorithms.visualization.logging.LogView;
import javafx.scene.layout.HBox;

/**
 * Shared execution controls supplied by the single Workbench shell to one
 * module controller.
 *
 * <p>Structure and Algorithm modes reuse one controller instance. Execution
 * controls are exposed only in Algorithm mode, while the controller keeps the
 * run, pause, replay, statistics and export state.</p>
 */
public record WorkbenchControls(
        Label statsLabel,
        LogView logView,
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
