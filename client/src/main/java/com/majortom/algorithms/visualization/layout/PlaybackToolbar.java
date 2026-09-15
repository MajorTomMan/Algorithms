package com.majortom.algorithms.visualization.layout;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsive layout for the execution controls.
 *
 * <p>The toolbar treats navigation, execution actions, timeline and speed as
 * semantic groups. On a wide workbench they stay on one line. When font size
 * or window width makes that impossible, the timeline moves to its own row
 * instead of shrinking labels or clipping controls.</p>
 */
public final class PlaybackToolbar extends Pane {

    private static final double DEFAULT_GAP = 10.0d;
    private static final double ROW_GAP = 7.0d;
    private static final String TIMELINE_CLASS = "playback-timeline-area";
    private static final String SPEED_CLASS = "playback-speed-area";

    private double horizontalGap = DEFAULT_GAP;
    private double verticalGap = ROW_GAP;

    public PlaybackToolbar() {
        getStyleClass().add("playback-toolbar-layout");
    }

    public void setGap(double gap) {
        horizontalGap = Math.max(0.0d, gap);
        verticalGap = Math.max(0.0d, gap * 0.7d);
        requestLayout();
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0.0d;
    }

    @Override
    protected double computePrefWidth(double height) {
        List<Node> children = managedChildren();
        double width = children.stream().mapToDouble(this::prefWidth).sum();
        return snappedLeftInset() + snappedRightInset()
                + width + horizontalGap * Math.max(0, children.size() - 1);
    }

    @Override
    protected double computePrefHeight(double width) {
        LayoutPlan plan = plan(Math.max(0.0d, width - snappedLeftInset() - snappedRightInset()));
        return snappedTopInset() + plan.height() + snappedBottomInset();
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double top = snappedTopInset();
        double width = Math.max(0.0d, getWidth() - left - snappedRightInset());
        LayoutPlan plan = plan(width);
        for (Placement placement : plan.placements()) {
            Node child = placement.node();
            child.resizeRelocate(
                    snapPositionX(left + placement.x()),
                    snapPositionY(top + placement.y()),
                    snapSizeX(placement.width()),
                    snapSizeY(placement.height()));
        }
    }

    private LayoutPlan plan(double width) {
        List<Node> children = managedChildren();
        if (children.isEmpty()) {
            return new LayoutPlan(List.of(), 0.0d);
        }
        Node timeline = children.stream().filter(this::isTimeline).findFirst().orElse(null);
        List<Node> fixed = children.stream().filter(node -> node != timeline).toList();

        double fixedWidth = totalWidth(fixed);
        double timelineMinimum = timeline == null ? 0.0d : minWidth(timeline);
        int gaps = Math.max(0, children.size() - 1);
        double oneRowRequired = fixedWidth + timelineMinimum + horizontalGap * gaps;

        if (timeline == null || width >= oneRowRequired) {
            return oneRow(children, timeline, width);
        }
        return wrapped(children, timeline, width);
    }

    private LayoutPlan oneRow(List<Node> children, Node timeline, double width) {
        List<Placement> placements = new ArrayList<>();
        double fixedWidth = children.stream()
                .filter(node -> node != timeline)
                .mapToDouble(this::prefWidth)
                .sum();
        double gaps = horizontalGap * Math.max(0, children.size() - 1);
        double timelineWidth = timeline == null
                ? 0.0d
                : Math.max(minWidth(timeline), width - fixedWidth - gaps);
        double rowHeight = children.stream().mapToDouble(node -> prefHeight(node, node == timeline ? timelineWidth : prefWidth(node))).max().orElse(0.0d);

        double x = 0.0d;
        for (Node child : children) {
            double childWidth = child == timeline ? timelineWidth : prefWidth(child);
            double childHeight = prefHeight(child, childWidth);
            placements.add(new Placement(child, x, Math.max(0.0d, (rowHeight - childHeight) / 2.0d), childWidth, childHeight));
            x += childWidth + horizontalGap;
        }
        return new LayoutPlan(List.copyOf(placements), rowHeight);
    }

    private LayoutPlan wrapped(List<Node> children, Node timeline, double width) {
        List<Node> topRow = children.stream().filter(node -> node != timeline).toList();
        List<Placement> placements = new ArrayList<>();
        double topHeight = topRow.stream().mapToDouble(node -> prefHeight(node, prefWidth(node))).max().orElse(0.0d);

        double topRequired = totalWidth(topRow) + horizontalGap * Math.max(0, topRow.size() - 1);
        double x = 0.0d;
        if (topRequired <= width) {
            for (Node child : topRow) {
                double childWidth = prefWidth(child);
                double childHeight = prefHeight(child, childWidth);
                placements.add(new Placement(child, x, Math.max(0.0d, (topHeight - childHeight) / 2.0d), childWidth, childHeight));
                x += childWidth + horizontalGap;
            }
        } else {
            // Extremely narrow windows keep semantic groups intact and let the
            // final speed group wrap to the second row beside the timeline.
            Node speed = topRow.stream().filter(this::isSpeed).findFirst().orElse(null);
            List<Node> controls = topRow.stream().filter(node -> node != speed).toList();
            double controlsHeight = controls.stream().mapToDouble(node -> prefHeight(node, prefWidth(node))).max().orElse(0.0d);
            for (Node child : controls) {
                double childWidth = Math.min(prefWidth(child), Math.max(0.0d, width - x));
                double childHeight = prefHeight(child, childWidth);
                placements.add(new Placement(child, x, Math.max(0.0d, (controlsHeight - childHeight) / 2.0d), childWidth, childHeight));
                x += childWidth + horizontalGap;
            }
            topHeight = controlsHeight;
            if (speed != null) {
                topRow = List.of(speed);
            } else {
                topRow = List.of();
            }
        }

        double secondY = topHeight + verticalGap;
        double speedWidth = topRow.size() == 1 && isSpeed(topRow.getFirst()) ? prefWidth(topRow.getFirst()) : 0.0d;
        double timelineWidth = speedWidth > 0.0d
                ? Math.max(minWidth(timeline), width - speedWidth - horizontalGap)
                : width;
        double timelineHeight = prefHeight(timeline, timelineWidth);
        placements.add(new Placement(timeline, 0.0d, secondY, timelineWidth, timelineHeight));
        double secondHeight = timelineHeight;
        if (speedWidth > 0.0d) {
            Node speed = topRow.getFirst();
            double speedHeight = prefHeight(speed, speedWidth);
            placements.add(new Placement(speed, timelineWidth + horizontalGap,
                    secondY + Math.max(0.0d, (timelineHeight - speedHeight) / 2.0d), speedWidth, speedHeight));
            secondHeight = Math.max(secondHeight, speedHeight);
        }
        return new LayoutPlan(List.copyOf(placements), secondY + secondHeight);
    }

    private double totalWidth(List<Node> nodes) {
        return nodes.stream().mapToDouble(this::prefWidth).sum();
    }

    private double prefWidth(Node node) {
        double value = node.prefWidth(-1.0d);
        if (!Double.isFinite(value) || value < 0.0d) {
            value = node.minWidth(-1.0d);
        }
        return Math.max(0.0d, value);
    }

    private double minWidth(Node node) {
        double value = node.minWidth(-1.0d);
        if (!Double.isFinite(value) || value < 0.0d) {
            value = 0.0d;
        }
        return Math.max(0.0d, value);
    }

    private double prefHeight(Node node, double width) {
        double value = node.prefHeight(width);
        if (!Double.isFinite(value) || value < 0.0d) {
            value = node.minHeight(width);
        }
        return Math.max(0.0d, value);
    }

    private boolean isTimeline(Node node) {
        return node.getStyleClass().contains(TIMELINE_CLASS);
    }

    private boolean isSpeed(Node node) {
        return node.getStyleClass().contains(SPEED_CLASS);
    }

    private List<Node> managedChildren() {
        return getChildren().stream().filter(Node::isManaged).toList();
    }

    private record Placement(Node node, double x, double y, double width, double height) {
    }

    private record LayoutPlan(List<Placement> placements, double height) {
    }
}
