package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.logging.LogEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import com.majortom.algorithms.visualization.structure.InMemoryStructureSnapshotStore;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.css.PseudoClass;
import java.util.Locale;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Renders saved/current snapshot cards and structure history; never mutates canonical structures. */
final class SnapshotWorkspace {
    private final InMemoryStructureSnapshotStore structureSnapshotStore;
    private final Map<String, String> selectedSnapshotIds;
    private final VBox snapshotCards;
    private final VBox inspectorSnapshotCards;
    private final Label snapshotCountLabel;
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private Button currentInputBtn, savedInputBtn;
    private final HBox structureHistoryCards;
    private final Label structureHistoryCountLabel;
    private final Runnable selectCurrent;
    private final Consumer<StructureSnapshot<?>> selectSaved;
    private final Consumer<StructureSnapshot<?>> restore;
    private final Runnable useCurrentInput;
    private final Consumer<StructureSnapshot<?>> useSavedInput;
    private final Supplier<SnapshotAlgorithmInputSupport<?>> inputSupport;
    private final BiFunction<StructureSnapshotSupport<?>, StructureSnapshot<?>, String> describe;
    private final Function<StructureSnapshot<?>, String> shortId;
    private final Function<StructureSnapshot<?>, String> formatTime;

    SnapshotWorkspace(InMemoryStructureSnapshotStore store, Map<String, String> selections,
            VBox cards, VBox inspectorCards, Label count, HBox historyCards, Label historyCount,
            Runnable selectCurrent,
            Consumer<StructureSnapshot<?>> selectSaved, Consumer<StructureSnapshot<?>> restore,
            Runnable useCurrentInput, Consumer<StructureSnapshot<?>> useSavedInput,
            Supplier<SnapshotAlgorithmInputSupport<?>> inputSupport,
            BiFunction<StructureSnapshotSupport<?>, StructureSnapshot<?>, String> describe,
            Function<StructureSnapshot<?>, String> shortId,
            Function<StructureSnapshot<?>, String> formatTime) {
        this.structureSnapshotStore = store;
        this.selectedSnapshotIds = selections;
        this.snapshotCards = cards;
        this.inspectorSnapshotCards = inspectorCards;
        this.snapshotCountLabel = count;
        this.structureHistoryCards = historyCards;
        this.structureHistoryCountLabel = historyCount;
        this.selectCurrent = selectCurrent;
        this.selectSaved = selectSaved;
        this.restore = restore;
        this.useCurrentInput = useCurrentInput;
        this.useSavedInput = useSavedInput;
        this.inputSupport = inputSupport;
        this.describe = describe;
        this.shortId = shortId;
        this.formatTime = formatTime;
    }

    void refresh(String moduleId, String moduleName, StructureSnapshotSupport<?> support,
            List<EventEnvelope> history) {
        if (support == null) {
            snapshotCards.getChildren().clear();
            if (inspectorSnapshotCards != null) inspectorSnapshotCards.getChildren().clear();
            selectedSnapshotIds.remove(moduleId);
            snapshotCountLabel.setText(I18N.text("label.workspace.snapshot.count", 0,
                    structureSnapshotStore.maxSnapshotsPerModule()));
            renderHistory(history);
            return;
        }
        snapshotCards.getChildren().clear();
        if (inspectorSnapshotCards != null) inspectorSnapshotCards.getChildren().clear();
        List<StructureSnapshot<?>> saved = structureSnapshotStore.snapshots(moduleId);
        String selectedSnapshotId = validSelectedSnapshotId(moduleId, saved);
        StructureSnapshot<?> current = support.captureStructureSnapshot();
        snapshotCards.getChildren().add(createSnapshotCard(moduleName,
                I18N.text("label.workspace.snapshot.current"), current, support, true,
                selectedSnapshotId == null));
        if (inspectorSnapshotCards != null) {
            inspectorSnapshotCards.getChildren().add(createInspectorCurrentSnapshotCard(
                    current, support, selectedSnapshotId == null));
        }
        for (StructureSnapshot<?> snapshot : saved) {
            boolean selected = snapshot.id().equals(selectedSnapshotId);
            snapshotCards.getChildren().add(createSnapshotCard(moduleName,
                    I18N.text("label.workspace.snapshot.saved"), snapshot, support, false, selected));
            if (inspectorSnapshotCards != null) {
                inspectorSnapshotCards.getChildren().add(createInspectorSnapshotCard(
                        snapshot, support, selected));
            }
        }
        if (saved.isEmpty()) {
            Label empty = new Label(I18N.text("label.workspace.snapshot.none"));
            empty.getStyleClass().add("snapshot-empty");
            empty.setWrapText(true);
            snapshotCards.getChildren().add(empty);
        }
        snapshotCountLabel.setText(I18N.text("label.workspace.snapshot.count", saved.size(),
                structureSnapshotStore.maxSnapshotsPerModule()));
        renderHistory(history);
    }

    private String validSelectedSnapshotId(String moduleId, List<StructureSnapshot<?>> snapshots) {
        String selectedId = selectedSnapshotIds.get(moduleId);
        if (selectedId == null) return null;
        for (StructureSnapshot<?> snapshot : snapshots) {
            if (snapshot.id().equals(selectedId)) return selectedId;
        }
        selectedSnapshotIds.remove(moduleId);
        return null;
    }

    StructureSnapshot<?> selectedSavedSnapshot(String moduleId, boolean fallbackToNewest) {
        List<StructureSnapshot<?>> snapshots = structureSnapshotStore.snapshots(moduleId);
        String selectedId = validSelectedSnapshotId(moduleId, snapshots);
        if (selectedId != null) {
            for (StructureSnapshot<?> snapshot : snapshots) {
                if (snapshot.id().equals(selectedId)) return snapshot;
            }
        }
        if (!fallbackToNewest || snapshots.isEmpty()) return null;
        StructureSnapshot<?> newest = snapshots.getFirst();
        selectedSnapshotIds.put(moduleId, newest.id());
        return newest;
    }

    boolean confirmRestore(StructureSnapshot<?> snapshot) {
        ButtonType cancel = new ButtonType(I18N.text("action.common.cancel"));
        ButtonType restore = new ButtonType(I18N.text("action.workspace.restore_snapshot"));
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", cancel, restore);
        alert.setTitle(I18N.text("confirm.snapshot.restore.title"));
        alert.setHeaderText(I18N.text("confirm.snapshot.restore.header", shortId.apply(snapshot)));
        alert.setContentText(I18N.text("confirm.snapshot.restore.content"));
        OperationDialogTheme.apply(alert);
        return alert.showAndWait().filter(restore::equals).isPresent();
    }

    void bindInputControls(Button current, Button saved) {
        this.currentInputBtn = current;
        this.savedInputBtn = saved;
    }

    void syncSelectedInput(String moduleId, String snapshotId) {
        if (snapshotId == null) selectedSnapshotIds.remove(moduleId);
        else selectedSnapshotIds.put(moduleId, snapshotId);
    }

    void renderInputSource(String moduleId) {
        SnapshotAlgorithmInputSupport<?> support = inputSupport.get();
        boolean hasSaved = moduleId != null && !structureSnapshotStore.snapshots(moduleId).isEmpty();
        if (savedInputBtn != null) savedInputBtn.setDisable(!hasSaved || support == null);
        if (currentInputBtn != null) currentInputBtn.setDisable(support == null);
        String snapshotId = support == null ? null : support.algorithmInputSnapshotId();
        boolean current = support != null && snapshotId == null;
        if (currentInputBtn != null) {
            currentInputBtn.pseudoClassStateChanged(SELECTED, current);
            currentInputBtn.setText(inputSourceButtonText("label.workspace.algorithm.input.current_button", current));
        }
        if (savedInputBtn != null) {
            savedInputBtn.pseudoClassStateChanged(SELECTED, support != null && !current);
            savedInputBtn.setText(inputSourceButtonText("label.workspace.algorithm.input.saved_button", support != null && !current));
        }
    }

    private static String inputSourceButtonText(String key, boolean selected) {
        return (selected ? "● " : "○ ") + I18N.text(key).toUpperCase(Locale.ROOT);
    }

    void renderHistory(List<EventEnvelope> events) {
        if (structureHistoryCards == null || structureHistoryCountLabel == null) {
            return;
        }
        structureHistoryCards.getChildren().clear();
        if (events == null) {
            structureHistoryCountLabel.setText("0");
            return;
        }
        List<EventEnvelope> domainEvents = events.stream()
                .filter(event -> !(event.event() instanceof ExecutionLifecycleEvent))
                .filter(event -> !(event.event() instanceof LogEvent))
                .toList();
        structureHistoryCountLabel.setText(String.valueOf(domainEvents.size()));
        if (domainEvents.isEmpty()) {
            Label empty = new Label(I18N.text("label.workspace.structure.history.none"));
            empty.getStyleClass().add("snapshot-empty");
            empty.setWrapText(true);
            structureHistoryCards.getChildren().add(empty);
            return;
        }
        int start = Math.max(0, domainEvents.size() - 12);
        for (int index = domainEvents.size() - 1; index >= start; index--) {
            structureHistoryCards.getChildren().add(createStructureHistoryCard(domainEvents.get(index)));
        }
    }

    private Node createStructureHistoryCard(EventEnvelope envelope) {
        VBox card = new VBox(3);
        card.getStyleClass().add("snapshot-card");
        card.setMaxWidth(Double.MAX_VALUE);
        Label eventName = new Label(envelope.event().getClass().getSimpleName());
        eventName.getStyleClass().add("snapshot-card-title");
        Label operation = new Label(shortOperationId(envelope.operationId()));
        operation.getStyleClass().add("snapshot-card-state");
        Label sequence = new Label("#" + envelope.sequence());
        sequence.getStyleClass().add("snapshot-card-time");
        card.getChildren().addAll(eventName, operation, sequence);
        return card;
    }

    private String shortOperationId(String operationId) {
        int lastDot = operationId.lastIndexOf('.');
        if (lastDot < 0 || lastDot + 1 >= operationId.length()) {
            return operationId;
        }
        return operationId.substring(lastDot + 1);
    }

    Node createInspectorCurrentSnapshotCard(
            StructureSnapshot<?> snapshot,
            StructureSnapshotSupport<?> support,
            boolean selected) {
        VBox card = new VBox(5);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("snapshot-card");
        card.getStyleClass().add("snapshot-card-current");
        if (selected) {
            card.getStyleClass().add("snapshot-card-selected");
        }
        card.setOnMouseClicked(event -> selectCurrent.run());
        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(I18N.text("label.workspace.snapshot.current"));
        title.getStyleClass().add("snapshot-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label state = new Label(I18N.text("label.workspace.snapshot.current_state"));
        state.getStyleClass().add("snapshot-card-state");
        header.getChildren().addAll(title, spacer, state);
        Label detail = new Label(describe.apply(support, snapshot));
        detail.setWrapText(true);
        detail.getStyleClass().add("snapshot-card-detail");
        card.getChildren().addAll(header, detail);
        return card;
    }

    Node createInspectorSnapshotCard(
            StructureSnapshot<?> snapshot,
            StructureSnapshotSupport<?> support,
            boolean selected) {
        VBox card = new VBox(5);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("snapshot-card");
        card.getStyleClass().add("snapshot-card-saved");
        if (selected) card.getStyleClass().add("snapshot-card-selected");
        card.setOnMouseClicked(event -> selectSaved.accept(snapshot));
        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(I18N.text("label.workspace.snapshot.card", shortId.apply(snapshot)));
        title.getStyleClass().add("snapshot-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label time = new Label(formatTime.apply(snapshot));
        time.getStyleClass().add("snapshot-card-detail");
        header.getChildren().addAll(title, spacer, time);
        Label detail = new Label(describe.apply(support, snapshot));
        detail.setWrapText(true);
        detail.getStyleClass().add("snapshot-card-detail");
        card.getChildren().addAll(header, detail);
        return card;
    }

    Node createSnapshotCard(
            String moduleName,
            String status,
            StructureSnapshot<?> snapshot,
            StructureSnapshotSupport<?> support,
            boolean current,
            boolean selected) {
        VBox card = new VBox(6);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("snapshot-card");
        if (current) {
            card.getStyleClass().add("snapshot-card-current");
            card.setOnMouseClicked(event -> selectCurrent.run());
        } else {
            card.getStyleClass().add("snapshot-card-saved");
            card.setOnMouseClicked(event -> selectSaved.accept(snapshot));
        }
        if (selected) {
            card.getStyleClass().add("snapshot-card-selected");
        }

        Label title;
        if (current) {
            title = new Label(moduleName);
        } else {
            title = new Label(moduleName + " · " + shortId.apply(snapshot));
        }
        title.getStyleClass().add("snapshot-card-title");
        Label state = new Label(status);
        state.getStyleClass().add("snapshot-card-state");
        Label detail = new Label(describe.apply(support, snapshot));
        detail.getStyleClass().add("snapshot-card-detail");
        card.getChildren().addAll(title, state, detail);

        if (!current) {
            Label createdAt = new Label(I18N.text("label.workspace.snapshot.time", formatTime.apply(snapshot)));
            createdAt.getStyleClass().add("snapshot-card-detail");
            card.getChildren().add(createdAt);
        }

        SnapshotAlgorithmInputSupport<?> algorithmInputSupport = inputSupport.get();
        if (algorithmInputSupport != null) {
            String inputSnapshotId = algorithmInputSupport.algorithmInputSnapshotId();
            boolean algorithmInput;
            if (current) {
                algorithmInput = inputSnapshotId == null;
            } else {
                algorithmInput = snapshot.id().equals(inputSnapshotId);
            }
            if (algorithmInput) {
                card.getStyleClass().add("snapshot-card-algorithm-input");
                Label inputState = new Label(I18N.text("label.workspace.snapshot.algorithm_input"));
                inputState.getStyleClass().add("snapshot-card-input-state");
                card.getChildren().add(inputState);
            }
        }

        HBox actions = new HBox(6);
        if (!current) {
            Button restore = new Button(I18N.text("action.workspace.restore_snapshot"));
            restore.getStyleClass().add("snapshot-card-action");
            WorkbenchTheme.applyControl(restore);
            restore.setOnAction(event -> this.restore.accept(snapshot));
            actions.getChildren().add(restore);
        }
        if (algorithmInputSupport != null) {
            Button useInput;
            if (current) {
                useInput = new Button(I18N.text("action.workspace.use_current_input"));
            } else {
                useInput = new Button(I18N.text("action.workspace.use_snapshot_input"));
            }
            useInput.getStyleClass().add("snapshot-card-action");
            WorkbenchTheme.applyControl(useInput);
            useInput.setOnAction(event -> {
                if (current) {
                    useCurrentInput.run();
                } else {
                    useSavedInput.accept(snapshot);
                }
            });
            actions.getChildren().add(useInput);
        }
        if (!actions.getChildren().isEmpty()) {
            actions.setOnMouseClicked(event -> event.consume());
            card.getChildren().add(actions);
        }
        return card;
    }

}
