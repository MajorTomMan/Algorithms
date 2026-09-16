package com.majortom.algorithms.visualization.layout;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * A font-aware row that keeps controls inline while they fit and wraps them into additional rows
 * when the available width becomes too small.
 *
 * <p>The layout is based on the controls' current preferred sizes, so changing the application font
 * immediately changes the wrapping decision without any module-specific LARGE/XLARGE rules.
 */
public final class AdaptiveFormRow extends Pane {

    private static final double DEFAULT_GAP = 6.0d;
    private static final double MIN_INPUT_WIDTH = 88.0d;
    private static final double MIN_COMBO_WIDTH = 104.0d;
    private static final double MIN_BUTTON_WIDTH = 64.0d;

    private double horizontalGap = DEFAULT_GAP;
    private double verticalGap = DEFAULT_GAP;

    public AdaptiveFormRow() {
        getStyleClass().add("adaptive-form-row");
    }

    public AdaptiveFormRow(double gap) {
        this();
        setGap(gap);
    }

    public void setGap(double gap) {
        double resolved = Math.max(0.0d, gap);
        horizontalGap = resolved;
        verticalGap = resolved;
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
        if (children.isEmpty()) {
            return 0.0d;
        }
        double width = 0.0d;
        for (Node child : children) {
            width += desiredWidth(child);
        }
        width += horizontalGap * Math.max(0, children.size() - 1);
        return snappedLeftInset() + width + snappedRightInset();
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected double computePrefHeight(double width) {
        double contentWidth = Math.max(0.0d, width - snappedLeftInset() - snappedRightInset());
        LayoutPlan plan = plan(managedChildren(), contentWidth);
        return snappedTopInset() + plan.height() + snappedBottomInset();
    }

    @Override
    protected void layoutChildren() {
        double x = snappedLeftInset();
        double y = snappedTopInset();
        double width = Math.max(0.0d, getWidth() - x - snappedRightInset());
        LayoutPlan plan = plan(managedChildren(), width);
        for (Line line : plan.lines()) {
            layoutLine(line, x, y, width);
            y += line.height() + verticalGap;
        }
    }

    private void layoutLine(Line line, double x, double y, double availableWidth) {
        if (line.children().isEmpty()) {
            return;
        }
        double gaps = horizontalGap * Math.max(0, line.children().size() - 1);
        double usable = Math.max(0.0d, availableWidth - gaps);
        double[] widths = allocateWidths(line.children(), usable);
        double cursor = x;
        for (int index = 0; index < line.children().size(); index++) {
            Node child = line.children().get(index);
            double childWidth = widths[index];
            double childHeight = boundedPrefHeight(child, childWidth);
            double childY = y + Math.max(0.0d, (line.height() - childHeight) / 2.0d);
            child.resizeRelocate(
                    snapPositionX(cursor),
                    snapPositionY(childY),
                    snapSizeX(childWidth),
                    snapSizeY(childHeight));
            cursor += childWidth + horizontalGap;
        }
    }

    private LayoutPlan plan(List<Node> children, double availableWidth) {
        if (children.isEmpty()) {
            return new LayoutPlan(List.of(), 0.0d);
        }
        if (availableWidth <= 0.0d) {
            return singleColumn(children, 0.0d);
        }

        List<Line> lines = new ArrayList<>();
        List<Node> current = new ArrayList<>();
        double currentRequired = 0.0d;
        for (Node child : children) {
            double desired = desiredWidth(child);
            double nextRequired =
                    current.isEmpty() ? desired : currentRequired + horizontalGap + desired;
            if (!current.isEmpty() && nextRequired > availableWidth) {
                lines.add(line(current, availableWidth));
                current = new ArrayList<>();
                currentRequired = 0.0d;
            }
            if (!current.isEmpty()) {
                currentRequired += horizontalGap;
            }
            current.add(child);
            currentRequired += desired;
        }
        if (!current.isEmpty()) {
            lines.add(line(current, availableWidth));
        }

        double height = 0.0d;
        for (Line line : lines) {
            height += line.height();
        }
        height += verticalGap * Math.max(0, lines.size() - 1);
        return new LayoutPlan(List.copyOf(lines), height);
    }

    private LayoutPlan singleColumn(List<Node> children, double width) {
        List<Line> lines = new ArrayList<>(children.size());
        double height = 0.0d;
        for (Node child : children) {
            Line line = line(List.of(child), width);
            lines.add(line);
            height += line.height();
        }
        height += verticalGap * Math.max(0, lines.size() - 1);
        return new LayoutPlan(List.copyOf(lines), height);
    }

    private Line line(List<Node> children, double availableWidth) {
        double gaps = horizontalGap * Math.max(0, children.size() - 1);
        double usable = Math.max(0.0d, availableWidth - gaps);
        double[] widths = allocateWidths(children, usable);
        double height = 0.0d;
        for (int index = 0; index < children.size(); index++) {
            height = Math.max(height, boundedPrefHeight(children.get(index), widths[index]));
        }
        return new Line(List.copyOf(children), height);
    }

    private double[] allocateWidths(List<Node> children, double availableWidth) {
        int count = children.size();
        double[] widths = new double[count];
        if (count == 0) {
            return widths;
        }
        if (count == 1) {
            widths[0] = availableWidth;
            return widths;
        }

        boolean allButtons = children.stream().allMatch(ButtonBase.class::isInstance);
        boolean allFlexible = children.stream().allMatch(this::isFlexibleInput);
        if (allButtons || allFlexible) {
            double equal = Math.max(0.0d, availableWidth / count);
            java.util.Arrays.fill(widths, equal);
            return widths;
        }

        double fixed = 0.0d;
        int flexible = 0;
        for (int index = 0; index < count; index++) {
            Node child = children.get(index);
            if (isFlexibleInput(child)) {
                flexible++;
            } else {
                widths[index] = Math.min(desiredWidth(child), availableWidth);
                fixed += widths[index];
            }
        }
        double remainder = Math.max(0.0d, availableWidth - fixed);
        double flexWidth = flexible == 0 ? 0.0d : remainder / flexible;
        for (int index = 0; index < count; index++) {
            if (isFlexibleInput(children.get(index))) {
                widths[index] = flexWidth;
            }
        }
        return widths;
    }

    private boolean isFlexibleInput(Node child) {
        return child instanceof TextInputControl || child instanceof ComboBoxBase<?>;
    }

    private double desiredWidth(Node child) {
        double preferred = Math.max(boundedPrefWidth(child), textAwareWidth(child));
        if (child instanceof TextInputControl) {
            return Math.max(MIN_INPUT_WIDTH, Math.min(preferred, 240.0d));
        }
        if (child instanceof ComboBoxBase<?>) {
            return Math.max(MIN_COMBO_WIDTH, Math.min(preferred, 240.0d));
        }
        if (child instanceof ButtonBase) {
            return Math.max(MIN_BUTTON_WIDTH, preferred);
        }
        return Math.max(64.0d, preferred);
    }

    /**
     * Preferred width derived from the actual localized text. CJK glyphs are full-em and can be
     * substantially wider than the same semantic label in Latin fonts, so relying on a fixed
     * minimum width makes Chinese the first locale to clip or look compressed.
     */
    private double textAwareWidth(Node child) {
        String text = null;
        Font font = null;
        if (child instanceof TextInputControl input) {
            text = input.getText();
            if (text == null || text.isBlank()) {
                text = input.getPromptText();
            }
            font = input.getFont();
        } else if (child instanceof Labeled labeled) {
            text = labeled.getText();
            font = labeled.getFont();
        } else if (child instanceof ComboBox<?> comboBox) {
            Object value = comboBox.getValue();
            text = value == null ? null : value.toString();
        }
        if (text == null || text.isBlank()) {
            return 0.0d;
        }
        if (font == null) {
            font = Font.getDefault();
        }
        Text probe = new Text(text);
        probe.setFont(font);
        double horizontalInsets =
                child instanceof Region region
                        ? region.getInsets().getLeft() + region.getInsets().getRight()
                        : 0.0d;
        return Math.ceil(probe.getLayoutBounds().getWidth() + horizontalInsets + 8.0d);
    }

    private double boundedPrefWidth(Node child) {
        double min = child.minWidth(-1.0d);
        double pref = child.prefWidth(-1.0d);
        double max = child.maxWidth(-1.0d);
        return bounded(min, pref, max);
    }

    private double boundedPrefHeight(Node child, double width) {
        double min = child.minHeight(width);
        double pref = child.prefHeight(width);
        double max = child.maxHeight(width);
        return bounded(min, pref, max);
    }

    private double bounded(double min, double pref, double max) {
        double resolved = Math.max(0.0d, pref);
        if (min != Region.USE_COMPUTED_SIZE && min != Region.USE_PREF_SIZE) {
            resolved = Math.max(resolved, min);
        }
        if (max != Double.MAX_VALUE
                && max != Region.USE_COMPUTED_SIZE
                && max != Region.USE_PREF_SIZE) {
            resolved = Math.min(resolved, max);
        }
        return resolved;
    }

    private List<Node> managedChildren() {
        return getChildren().stream().filter(Node::isManaged).toList();
    }

    private record Line(List<Node> children, double height) {}

    private record LayoutPlan(List<Line> lines, double height) {}
}
