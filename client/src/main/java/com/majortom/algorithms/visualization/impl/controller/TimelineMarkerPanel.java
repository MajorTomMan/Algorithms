package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.international.I18N;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Owns timeline marker grouping, filtering, drawing and navigation, not execution state. */
final class TimelineMarkerPanel {
    private final Pane timelineMarkers;
    private final Label timelineLabel;
    private final Label timelineRuntimeLegendLabel;
    private final Label timelineStructureLegendLabel;
    private final Label timelineObservationLegendLabel;
    private final Supplier<BaseController<?>> controller;
    private final Runnable afterSeek;
    private boolean timelineRuntimeVisible = true;
    private boolean timelineStructureVisible = true;
    private boolean timelineObservationVisible = true;

    // Only UI nodes and a small invalidation token are retained, not a second event history.
    private BaseController<?> renderedController;
    private long renderedRevision = Long.MIN_VALUE;
    private double renderedWidth = Double.NaN;
    private double renderedHeight = Double.NaN;
    private double renderedFontSize = Double.NaN;
    private int highlightedEventIndex = -1;
    private Region cursorMarker;
    private List<TimelineMarkerGroup> renderedGroups = List.of();
    private List<VBox> renderedMarkers = List.of();

    // Only integer positions are indexed; authoritative event objects stay with the controller.
    private List<EventEnvelope> indexedSource;
    private int indexedCount;
    private final List<Integer> visibleEventIndexes = new ArrayList<>();

    TimelineMarkerPanel(Pane markers, Label label, Label runtime, Label structure, Label observation,
            Supplier<BaseController<?>> controller, Runnable afterSeek) {
        this.timelineMarkers = markers;
        this.timelineLabel = label;
        this.timelineRuntimeLegendLabel = runtime;
        this.timelineStructureLegendLabel = structure;
        this.timelineObservationLegendLabel = observation;
        this.controller = controller;
        this.afterSeek = afterSeek;
    }

    void install() {
        if (timelineMarkers != null) {
            timelineMarkers.widthProperty().addListener((observable, before, after) -> rebuild());
        }
        configureTimelineLegendToggle(timelineRuntimeLegendLabel, TimelineMarkerCategory.RUNTIME);
        configureTimelineLegendToggle(timelineStructureLegendLabel, TimelineMarkerCategory.STRUCTURE);
        configureTimelineLegendToggle(timelineObservationLegendLabel, TimelineMarkerCategory.OBSERVATION);
        refreshTimelineLegendState();
    }

    private BaseController<?> currentController() { return controller.get(); }

    private void configureTimelineLegendToggle(Label label, TimelineMarkerCategory category) {
        if (label == null) {
            return;
        }
        label.getStyleClass().add("timeline-legend-toggle");
        label.setFocusTraversable(true);
        label.setOnMouseClicked(event -> {
            toggleTimelineMarkerCategory(category);
            event.consume();
        });
        label.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                toggleTimelineMarkerCategory(category);
                event.consume();
            }
        });
    }

    private void toggleTimelineMarkerCategory(TimelineMarkerCategory category) {
        switch (category) {
            case RUNTIME -> timelineRuntimeVisible = !timelineRuntimeVisible;
            case STRUCTURE -> timelineStructureVisible = !timelineStructureVisible;
            case OBSERVATION -> timelineObservationVisible = !timelineObservationVisible;
        }
        refreshTimelineLegendState();
        rebuild();
    }

    private void refreshTimelineLegendState() {
        updateTimelineLegendState(timelineRuntimeLegendLabel, timelineRuntimeVisible);
        updateTimelineLegendState(timelineStructureLegendLabel, timelineStructureVisible);
        updateTimelineLegendState(timelineObservationLegendLabel, timelineObservationVisible);
    }

    private void updateTimelineLegendState(Label label, boolean visible) {
        if (label == null) {
            return;
        }
        if (visible) {
            label.getStyleClass().remove("timeline-legend-muted");
        } else if (!label.getStyleClass().contains("timeline-legend-muted")) {
            label.getStyleClass().add("timeline-legend-muted");
        }
    }

    /** Normal cursor refresh: no history copy or JavaFX node reconstruction when data is unchanged. */
    void refresh() {
        if (timelineMarkers == null) return;
        BaseController<?> active = currentController();
        long revision = active == null ? Long.MIN_VALUE : active.executionEventRevision();
        if (active != renderedController
                || Double.compare(renderedWidth, timelineMarkers.getWidth()) != 0
                || Double.compare(renderedHeight, timelineMarkers.getHeight()) != 0
                || Double.compare(renderedFontSize, timelineBaseFontSize()) != 0) {
            rebuild();
        } else if (revision != renderedRevision) {
            rebuildMarkers();
        } else {
            updateCursor(active == null ? -1 : active.presentationEventIndex());
        }
    }

    /** Recreate marker groups only after a history, filter, locale or geometry change. */
    void rebuild() {
        indexedSource = null;
        indexedCount = 0;
        visibleEventIndexes.clear();
        rebuildMarkers();
    }

    /** Recompute positions from the indexed candidate offsets; classify only new events. */
    private void rebuildMarkers() {
        if (timelineMarkers == null) return;
        BaseController<?> active = currentController();
        timelineMarkers.getChildren().clear();
        renderedController = active;
        renderedRevision = active == null ? Long.MIN_VALUE : active.executionEventRevision();
        renderedWidth = timelineMarkers.getWidth();
        renderedHeight = timelineMarkers.getHeight();
        renderedFontSize = timelineBaseFontSize();
        renderedGroups = List.of();
        renderedMarkers = List.of();
        highlightedEventIndex = -1;
        cursorMarker = null;
        if (active == null) return;
        List<EventEnvelope> events = active.timelineMarkerEvents();
        if (events != indexedSource || events.size() < indexedCount) {
            visibleEventIndexes.clear();
            indexedCount = 0;
            indexedSource = events;
        }
        for (int i = indexedCount; i < events.size(); i++) {
            if (isTimelineMarkerVisible(events.get(i))) visibleEventIndexes.add(i);
        }
        indexedCount = events.size();
        if (events.isEmpty()) return;

        double paneWidth = timelineMarkers.getWidth();
        if (!(paneWidth > 0.0d)) {
            paneWidth = timelineMarkers.prefWidth(-1.0d);
        }
        if (!(paneWidth > 0.0d) || !Double.isFinite(paneWidth)) {
            paneWidth = 320.0d;
        }
        double paneHeight = timelineMarkers.getHeight();
        if (!(paneHeight > 0.0d)) {
            paneHeight = timelineMarkers.prefHeight(paneWidth);
        }
        if (!(paneHeight > 0.0d) || !Double.isFinite(paneHeight)) {
            paneHeight = 36.0d;
        }

        double fontSize = timelineBaseFontSize();
        double symbolSize = Math.max(7.0d, Math.min(14.0d, fontSize * 0.52d));
        double horizontalInset = Math.max(symbolSize, fontSize * 0.62d);
        double usableWidth = Math.max(1.0d, paneWidth - horizontalInset * 2.0d);
        double trackCenterY = paneHeight / 2.0d;

        cursorMarker = new Region();
        cursorMarker.getStyleClass().add("timeline-marker-cursor");
        cursorMarker.setManaged(false);
        cursorMarker.setMouseTransparent(true);
        cursorMarker.setVisible(false);
        timelineMarkers.getChildren().add(cursorMarker);

        List<TimelineMarkerGroup> groups =
                timelineMarkerGroups(events, visibleEventIndexes, usableWidth);
        List<VBox> nodes = new ArrayList<>(groups.size());
        for (TimelineMarkerGroup group : groups) {
            int representativeIndex = group.representativeIndex();
            EventEnvelope envelope = events.get(representativeIndex);
            VBox marker = new VBox(Math.max(1.0d, fontSize * 0.06d));
            marker.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            marker.setManaged(false);
            marker.setFocusTraversable(true);
            marker.getStyleClass().add("timeline-marker-node");
            if (group.eventIndexes().size() > 1) {
                marker.getStyleClass().add("timeline-marker-aggregate");
            }

            Region symbol = new Region();
            symbol.setMinSize(symbolSize, symbolSize);
            symbol.setPrefSize(symbolSize, symbolSize);
            symbol.setMaxSize(symbolSize, symbolSize);
            symbol.getStyleClass().addAll("timeline-marker-symbol", eventMarkerClass(envelope));
            marker.getChildren().setAll(symbol);
            if (group.eventIndexes().size() > 1) {
                Label aggregate = new Label("×" + group.eventIndexes().size());
                aggregate.getStyleClass().add("timeline-marker-sequence");
                marker.getChildren().add(aggregate);
            }

            timelineMarkers.getChildren().add(marker);
            nodes.add(marker);
            marker.applyCss();
            marker.autosize();
            double markerWidth = Math.max(symbolSize, marker.prefWidth(-1.0d));
            double resolvedMarkerHeight = Math.max(symbolSize, marker.prefHeight(markerWidth));
            marker.resize(markerWidth, resolvedMarkerHeight);

            double ratio = eventRatio(representativeIndex, events.size());
            double centerX = horizontalInset + ratio * usableWidth;
            double markerX = Math.max(0.0d, Math.min(paneWidth - markerWidth, centerX - markerWidth / 2.0d));
            double markerY = Math.max(0.0d, trackCenterY - symbolSize / 2.0d);
            marker.relocate(markerX, markerY);

            String tooltipText = timelineMarkerTooltip(events, group);
            Tooltip.install(marker, new Tooltip(tooltipText));
            marker.setAccessibleText(tooltipText);
            marker.setOnMouseClicked(event -> {
                jumpToTimelineMarkerGroup(group);
                event.consume();
            });
            marker.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                    jumpToTimelineMarkerGroup(group);
                    event.consume();
                }
            });
        }
        renderedGroups = groups;
        renderedMarkers = nodes;
        updateCursor(active.presentationEventIndex());
    }

    /** Move the cursor and swap CSS states on existing markers; at most 48 groups. */
    private void updateCursor(int currentIndex) {
        if (currentIndex == highlightedEventIndex) return;
        highlightedEventIndex = currentIndex;
        boolean valid = currentIndex >= 0 && currentIndex < indexedCount;
        if (cursorMarker != null) {
            cursorMarker.setVisible(valid);
            if (valid) {
                double paneWidth = renderedWidth;
                if (!(paneWidth > 0.0d)) paneWidth = timelineMarkers.prefWidth(-1.0d);
                if (!(paneWidth > 0.0d) || !Double.isFinite(paneWidth)) paneWidth = 320.0d;
                double paneHeight = renderedHeight;
                if (!(paneHeight > 0.0d)) paneHeight = timelineMarkers.prefHeight(paneWidth);
                if (!(paneHeight > 0.0d) || !Double.isFinite(paneHeight)) paneHeight = 36.0d;
                double symbolSize = Math.max(7.0d, Math.min(14.0d, renderedFontSize * 0.52d));
                double inset = Math.max(symbolSize, renderedFontSize * 0.62d);
                double usableWidth = Math.max(1.0d, paneWidth - inset * 2.0d);
                double width = Math.max(2.0d, renderedFontSize * 0.10d);
                double height = Math.max(symbolSize * 2.4d, paneHeight * 0.72d);
                double x = inset + eventRatio(currentIndex, indexedCount) * usableWidth - width / 2.0d;
                cursorMarker.resizeRelocate(x, (paneHeight - height) / 2.0d, width, height);
            }
        }
        for (int i = 0; i < renderedGroups.size(); i++) {
            // Group indices are ordered; avoid scanning large aggregated groups each frame.
            boolean selected = java.util.Collections.binarySearch(
                    renderedGroups.get(i).eventIndexes(), currentIndex) >= 0;
            VBox marker = renderedMarkers.get(i);
            Region symbol = (Region) marker.getChildren().getFirst();
            setStyleClass(symbol, "timeline-marker-current", selected);
            if (marker.getChildren().size() > 1) {
                setStyleClass(marker.getChildren().get(1), "timeline-marker-sequence-current", selected);
            }
        }
    }

    private static void setStyleClass(javafx.scene.Node node, String styleClass, boolean selected) {
        if (selected) {
            if (!node.getStyleClass().contains(styleClass)) node.getStyleClass().add(styleClass);
        } else {
            node.getStyleClass().remove(styleClass);
        }
    }

    private double timelineBaseFontSize() {
        if (timelineLabel != null && timelineLabel.getFont() != null) {
            return Math.max(8.0d, timelineLabel.getFont().getSize());
        }
        return 16.0d;
    }

    private void jumpToTimelineMarkerGroup(TimelineMarkerGroup group) {
        BaseController<?> active = currentController();
        if (active == null || active.isRunning()) return;
        int currentIndex = active.presentationEventIndex();
        int targetIndex = group.eventIndexes().getFirst();
        if (group.eventIndexes().size() > 1) {
            int nextIndex = java.util.Collections.binarySearch(group.eventIndexes(), currentIndex);
            nextIndex = nextIndex >= 0 ? nextIndex + 1 : -nextIndex - 1;
            if (nextIndex < group.eventIndexes().size())
                targetIndex = group.eventIndexes().get(nextIndex);
        }
        active.seekEventIndex(targetIndex);
        afterSeek.run();
    }

    private List<TimelineMarkerGroup> timelineMarkerGroups(
            List<EventEnvelope> events, List<Integer> candidates, double usableWidth) {
        if (candidates.isEmpty()) return List.of();

        double markerFootprint = Math.max(18.0d, timelineBaseFontSize() * 1.75d);
        int markerBudget = (int) Math.floor(usableWidth / markerFootprint);
        markerBudget = Math.max(3, Math.min(48, markerBudget));
        if (candidates.size() <= markerBudget) {
            List<TimelineMarkerGroup> groups = new ArrayList<>(candidates.size());
            for (int index : candidates) {
                groups.add(new TimelineMarkerGroup(index, List.of(index)));
            }
            return List.copyOf(groups);
        }

        Map<Integer, List<Integer>> buckets = new LinkedHashMap<>();
        for (int eventIndex : candidates) {
            double ratio = eventRatio(eventIndex, events.size());
            int bucket = (int) Math.floor(ratio * (markerBudget - 1));
            buckets.computeIfAbsent(bucket, ignored -> new ArrayList<>()).add(eventIndex);
        }
        List<TimelineMarkerGroup> groups = new ArrayList<>(buckets.size());
        for (List<Integer> eventIndexes : buckets.values()) {
            int representative = representativeTimelineEvent(events, eventIndexes);
            groups.add(new TimelineMarkerGroup(representative, List.copyOf(eventIndexes)));
        }
        groups.sort((left, right) -> Integer.compare(left.representativeIndex(), right.representativeIndex()));
        return List.copyOf(groups);
    }

    private int representativeTimelineEvent(
            List<EventEnvelope> events,
            List<Integer> eventIndexes) {
        for (int eventIndex : eventIndexes) {
            if (events.get(eventIndex).event() instanceof ExecutionLifecycleEvent) {
                return eventIndex;
            }
        }
        for (int eventIndex : eventIndexes) {
            if (events.get(eventIndex).event() instanceof com.majortom.algorithms.core.event.structure.StructureEvent) {
                return eventIndex;
            }
        }
        return eventIndexes.get(eventIndexes.size() / 2);
    }

    private boolean isTimelineMarkerVisible(EventEnvelope envelope) {
        if (envelope.event() instanceof ExecutionLifecycleEvent) {
            return timelineRuntimeVisible;
        }
        if (envelope.event() instanceof com.majortom.algorithms.core.event.structure.StructureEvent) {
            return timelineStructureVisible;
        }
        if (envelope.event() instanceof AlgorithmEvent) {
            return timelineObservationVisible;
        }
        return false;
    }

    private double eventRatio(int eventIndex, int eventCount) {
        if (eventCount <= 1) {
            return 0.0d;
        }
        return eventIndex / (double) (eventCount - 1);
    }

    private String timelineMarkerTooltip(List<EventEnvelope> events, TimelineMarkerGroup group) {
        EventEnvelope envelope = events.get(group.representativeIndex());
        String base = I18N.text("label.workspace.event") + " " + envelope.sequence()
                + " · " + WorkbenchEventText.eventDisplayName(envelope);
        if (group.eventIndexes().size() <= 1) {
            return base;
        }
        return base + "  ·  ×" + group.eventIndexes().size();
    }

    private record TimelineMarkerGroup(int representativeIndex, List<Integer> eventIndexes) {
    }

    private enum TimelineMarkerCategory {
        RUNTIME,
        STRUCTURE,
        OBSERVATION
    }

    private String eventMarkerClass(EventEnvelope envelope) {
        if (envelope.event() instanceof ExecutionLifecycleEvent) return "timeline-runtime";
        if (envelope.event() instanceof com.majortom.algorithms.core.event.structure.StructureEvent) return "timeline-structure";
        if (envelope.event() instanceof AlgorithmEvent) return "timeline-observation";
        return "timeline-other";
    }

}
