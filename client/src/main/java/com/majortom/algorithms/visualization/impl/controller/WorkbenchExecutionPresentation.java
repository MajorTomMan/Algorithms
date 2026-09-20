package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.visualization.BaseController;
import java.util.Locale;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Owns the current event overlay, output labels and viewport obstruction insets. */
final class WorkbenchExecutionPresentation {
    private final VBox currentStepOverlay, algorithmSelectionOverlay;
    private final Label currentStepSequenceLabel, currentStepKindLabel, currentStepDetailLabel;
    private final Label timelineCursorLabel, resultLabel, resultPreviewLabel;
    WorkbenchExecutionPresentation(VBox currentStepOverlay, Label currentStepSequenceLabel,
            Label currentStepKindLabel, Label currentStepDetailLabel, Label timelineCursorLabel,
            Label resultLabel, Label resultPreviewLabel, VBox algorithmSelectionOverlay) {
        this.currentStepOverlay = currentStepOverlay;
        this.currentStepSequenceLabel = currentStepSequenceLabel;
        this.currentStepKindLabel = currentStepKindLabel;
        this.currentStepDetailLabel = currentStepDetailLabel;
        this.timelineCursorLabel = timelineCursorLabel;
        this.resultLabel = resultLabel;
        this.resultPreviewLabel = resultPreviewLabel;
        this.algorithmSelectionOverlay = algorithmSelectionOverlay;
    }

    void render(BaseController<?> controller, EventEnvelope current) {
        if (current == null) {
            if (currentStepOverlay != null) {
                currentStepOverlay.setManaged(false);
                currentStepOverlay.setVisible(false);
            }
            updateObstruction(controller,false);
            if (timelineCursorLabel != null) {
                timelineCursorLabel.setText("");
                timelineCursorLabel.setVisible(false);
            }
        } else {
            boolean showStep = !(current.event() instanceof AlgorithmEvent);
            if (currentStepOverlay != null) {
                currentStepOverlay.setManaged(showStep);
                currentStepOverlay.setVisible(showStep);
            }
            updateObstruction(controller,showStep);
            if (showStep) {
                String kind = WorkbenchEventText.eventDisplayName(current);
                if (currentStepSequenceLabel != null) currentStepSequenceLabel.setText(String.format(Locale.ROOT, "#%04d", current.sequence()));
                if (currentStepKindLabel != null) currentStepKindLabel.setText(kind);
                if (currentStepDetailLabel != null) currentStepDetailLabel.setText(WorkbenchEventText.describeCurrentStep(current));
            }
            if (timelineCursorLabel != null) timelineCursorLabel.setVisible(false);
        }
        String result = controller.latestResultText();
        if (resultLabel != null) resultLabel.setText(result);
        if (resultPreviewLabel != null) resultPreviewLabel.setText(result);
    }

    void updateObstruction(BaseController<?> currentSubController, boolean currentStepVisible) {
        if (currentSubController == null || currentSubController.getVisualizer() == null) {
            return;
        }
        double left;
        if (currentStepVisible) {
            left = currentStepOverlay.getWidth() + 24.0d;
        } else {
            left = 0.0d;
        }
        double right;
        if (algorithmSelectionOverlay != null && algorithmSelectionOverlay.isVisible()) {
            right = algorithmSelectionOverlay.getWidth() + 24.0d;
        } else {
            right = 0.0d;
        }
        currentSubController.getVisualizer().setViewportObstructionInsets(
                new javafx.geometry.Insets(0.0d, right, 0.0d, left));
    }

}
