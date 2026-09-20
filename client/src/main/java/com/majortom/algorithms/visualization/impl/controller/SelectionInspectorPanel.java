package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import java.util.function.BooleanSupplier;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Owns selection formatting, localization refresh and the two inspector overlays. */
final class SelectionInspectorPanel {
    private final VBox structureSelectionOverlay;
    private final VBox algorithmSelectionOverlay;
    private final Label selectedEntityTitleLabel;
    private final Label selectedEntityHintLabel;
    private final Label selectedNodeIdLabel;
    private final Label selectedNodeValueLabel;
    private final Label algorithmSelectedEntityTitleLabel;
    private final Label algorithmSelectedEntityHintLabel;
    private final Label algorithmSelectedNodeIdLabel;
    private final Label algorithmSelectedNodeValueLabel;
    private final Label structureInspectorBody;
    private final BooleanSupplier structurePageVisible;
    private final Runnable updateObstruction;
    private Runnable selectionPresentation;

    SelectionInspectorPanel(
            VBox structureSelectionOverlay,
            VBox algorithmSelectionOverlay,
            Label selectedEntityTitleLabel,
            Label selectedEntityHintLabel,
            Label selectedNodeIdLabel,
            Label selectedNodeValueLabel,
            Label algorithmSelectedEntityTitleLabel,
            Label algorithmSelectedEntityHintLabel,
            Label algorithmSelectedNodeIdLabel,
            Label algorithmSelectedNodeValueLabel,
            Label structureInspectorBody,
            BooleanSupplier structurePageVisible, Runnable updateObstruction) {
        this.structureSelectionOverlay = structureSelectionOverlay;
        this.algorithmSelectionOverlay = algorithmSelectionOverlay;
        this.selectedEntityTitleLabel = selectedEntityTitleLabel;
        this.selectedEntityHintLabel = selectedEntityHintLabel;
        this.selectedNodeIdLabel = selectedNodeIdLabel;
        this.selectedNodeValueLabel = selectedNodeValueLabel;
        this.algorithmSelectedEntityTitleLabel = algorithmSelectedEntityTitleLabel;
        this.algorithmSelectedEntityHintLabel = algorithmSelectedEntityHintLabel;
        this.algorithmSelectedNodeIdLabel = algorithmSelectedNodeIdLabel;
        this.algorithmSelectedNodeValueLabel = algorithmSelectedNodeValueLabel;
        this.structureInspectorBody = structureInspectorBody;
        this.structurePageVisible = structurePageVisible;
        this.updateObstruction = updateObstruction;
    }

    boolean hasPresentation() { return selectionPresentation != null; }
    void refresh() { if (selectionPresentation != null) selectionPresentation.run(); }

    void showTreeSelection(TreeController.NodeSelection selection) {
        if (selection == null) { clear(); return; }
        selectionPresentation = () -> presentValueSelection(
                I18N.text("label.workspace.selection.node"), "#" + selection.id(),
                selection.value().text(), I18N.text("label.workspace.selection.node.hint"),
                I18N.text("label.workspace.selection.tree.detail", selection.id(), selection.value().text(), nodeIdText(selection.parentId()), selection.childCount(), selection.depth()), selection.value());
        selectionPresentation.run();
    }

    void showArraySelection(ArrayController.IndexSelection selection) {
        if (selection == null) { clear(); return; }
        selectionPresentation = () -> presentValueSelection(
                I18N.text("label.workspace.selection.cell"), "[" + selection.index() + "]",
                selection.value().text(), I18N.text("label.workspace.selection.array.hint"),
                I18N.text("label.workspace.selection.array.detail", selection.index(), selection.value().text(), selection.size()), selection.value());
        selectionPresentation.run();
    }

    void showStringSelection(StringController.IndexSelection selection) {
        if (selection == null) { clear(); return; }
        selectionPresentation = () -> presentSelection(
                I18N.text("label.workspace.selection.character"), "[" + selection.index() + "]",
                Character.toString(selection.value()), I18N.text("label.workspace.selection.string.hint"),
                I18N.text("label.workspace.selection.string.detail", selection.index(), Character.toString(selection.value()), selection.length()));
        selectionPresentation.run();
    }

    void showLinkedSelection(LinkedListController.NodeSelection selection) {
        if (selection == null) { clear(); return; }
        selectionPresentation = () -> presentValueSelection(
                I18N.text("label.workspace.selection.node"), "#" + selection.id(),
                selection.value().text(), I18N.text("label.workspace.selection.linked.hint"),
                I18N.text("label.workspace.selection.linked.detail", selection.id(), selection.value().text(), selection.index(), nodeIdText(selection.previousId()), nodeIdText(selection.nextId()), selection.size()), selection.value());
        selectionPresentation.run();
    }

    void showLinearSelection(LinearStructureController.ItemSelection selection) {
        if (selection == null) { clear(); return; }
        selectionPresentation = () -> presentValueSelection(
                I18N.text("label.workspace.selection.item"), "[" + selection.index() + "]",
                selection.value().text(), linearRoleText(selection.role()),
                I18N.text("label.workspace.selection.linear.detail", selection.index(), selection.value().text(), linearRoleText(selection.role()), selection.size()), selection.value());
        selectionPresentation.run();
    }

    void showMazeSelection(MazeController.CellSelection selection) {
        if (selection == null) { clear(); return; }
        selectionPresentation = () -> presentSelection(
                I18N.text("label.workspace.selection.cell"), "[" + selection.row() + "," + selection.column() + "]",
                mazeCellStateText(selection.state()), I18N.text("label.workspace.selection.maze.hint"),
                I18N.text("label.workspace.selection.maze.detail", selection.row(), selection.column(), mazeCellStateText(selection.state())));
        selectionPresentation.run();
    }

    void showGraphSelection(GraphController.Selection selection) {
        if (selection == null) { clear(); return; }
        selectionPresentation = () -> {
            if (selection instanceof GraphController.NodeSelection node) {
                presentValueSelection(I18N.text("label.workspace.selection.node"), "#" + node.id(),
                        node.value().text(), I18N.text("label.workspace.selection.graph.node.hint"),
                        I18N.text("label.workspace.selection.graph.node.detail", node.id(), node.value().text(), node.degree()), node.value());
            } else if (selection instanceof GraphController.EdgeSelection edge) {
                presentSelection(I18N.text("label.workspace.selection.edge"), "E#" + edge.id(),
                        edge.fromValue().text() + (edge.directed() ? " → " : " — ") + edge.toValue().text(),
                        I18N.text("label.workspace.selection.graph.edge.hint"),
                        I18N.text("label.workspace.selection.graph.edge.detail", edge.id(),
                                edge.fromValue().text(), edge.toValue().text(),
                                I18N.text(edge.directed() ? "label.workspace.selection.yes" : "label.workspace.selection.no"))
                                + "\n\n" + edge.fromValue().projection().details()
                                + "\n\n" + edge.toValue().projection().details());
            }
        };
        selectionPresentation.run();
    }

    private String nodeIdText(Long id) {
        return id == null ? I18N.text("label.workspace.selection.none") : "#" + id;
    }

    /** Structure identity remains in the existing detail; declared element fields follow it. */
    private void presentValueSelection(String title, String id, String summary, String hint,
                                       String structureDetail, com.majortom.algorithms.visualization.runtime.VisualValue value) {
        String fields = value.projection().details();
        presentSelection(title, id, summary, hint, structureDetail + "\n\n" + fields);
    }

    /** All formatting completes before either view is changed, so a failed format cannot leave half a selection. */
    private void presentSelection(String title, String id, String value, String hint, String detail) {
        showStructureSelectionOverlay(title, id, value, hint);
        if (structureInspectorBody != null) structureInspectorBody.setText(detail);
    }

    private String linearRoleText(String role) {
        if ("TOP".equals(role)) {
            return I18N.text("label.workspace.selection.role.top");
        }
        if ("FRONT / REAR".equals(role)) {
            return I18N.text("label.workspace.selection.role.front_rear");
        }
        if ("FRONT".equals(role)) {
            return I18N.text("label.workspace.selection.role.front");
        }
        if ("REAR".equals(role)) {
            return I18N.text("label.workspace.selection.role.rear");
        }
        return I18N.text("label.workspace.selection.role.item");
    }

    private String mazeCellStateText(String state) {
        if ("ENTRANCE".equals(state)) {
            return I18N.text("label.workspace.selection.maze.entrance");
        }
        if ("EXIT".equals(state)) {
            return I18N.text("label.workspace.selection.maze.exit");
        }
        if ("OPEN".equals(state)) {
            return I18N.text("label.workspace.selection.maze.open");
        }
        if ("WALL".equals(state)) {
            return I18N.text("label.workspace.selection.maze.wall");
        }
        return state;
    }

    void showStructureSelectionOverlay(String title, String id, String value, String hint) {
        if (!structurePageVisible.getAsBoolean()) {
            showAlgorithmSelectionOverlay(title, id, value);
            return;
        }
        if (structureSelectionOverlay != null) {
            structureSelectionOverlay.setManaged(true);
            structureSelectionOverlay.setVisible(true);
        }
        if (algorithmSelectionOverlay != null) {
            algorithmSelectionOverlay.setManaged(false);
            algorithmSelectionOverlay.setVisible(false);
        }
        if (selectedEntityTitleLabel != null) selectedEntityTitleLabel.setText(title);
        if (selectedEntityHintLabel != null) selectedEntityHintLabel.setText(hint);
        if (selectedNodeIdLabel != null) selectedNodeIdLabel.setText(id);
        if (selectedNodeValueLabel != null) selectedNodeValueLabel.setText(value);
        updateObstruction.run();
    }

    void showAlgorithmSelectionOverlay(String title, String id, String value) {
        if (algorithmSelectionOverlay != null) {
            algorithmSelectionOverlay.setManaged(true);
            algorithmSelectionOverlay.setVisible(true);
        }
        if (structureSelectionOverlay != null) {
            structureSelectionOverlay.setManaged(false);
            structureSelectionOverlay.setVisible(false);
        }
        if (algorithmSelectedEntityTitleLabel != null) algorithmSelectedEntityTitleLabel.setText(title);
        if (algorithmSelectedEntityHintLabel != null) {
            algorithmSelectedEntityHintLabel.setText(I18N.text("label.workspace.selection.algorithm.hint"));
        }
        if (algorithmSelectedNodeIdLabel != null) algorithmSelectedNodeIdLabel.setText(id);
        if (algorithmSelectedNodeValueLabel != null) algorithmSelectedNodeValueLabel.setText(value);
        updateObstruction.run();
    }

    void clear() {
        selectionPresentation = null;
        if (structureSelectionOverlay != null) {
            structureSelectionOverlay.setManaged(false);
            structureSelectionOverlay.setVisible(false);
        }
        if (algorithmSelectionOverlay != null) {
            algorithmSelectionOverlay.setManaged(false);
            algorithmSelectionOverlay.setVisible(false);
        }
        if (structureInspectorBody != null) {
            structureInspectorBody.setText(I18N.text("label.workspace.selection.prompt"));
        }
        updateObstruction.run();
    }

}
