package com.majortom.algorithms.visualization.layout;

import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;

/**
 * Stable Workbench header. It owns shell geometry only: brand, workspace mode and global actions.
 * Family identity and run state deliberately stay in the workspace below.
 */
public final class WorkbenchHeader extends Pane {
  private static final double MIN_HEIGHT = 72.0d;
  private static final double MAX_HEIGHT = 108.0d;
  private static final double HEIGHT_EM = 4.8d;

  private double typographySize = 16.0d;

  public WorkbenchHeader() {
    getStyleClass().add("workbench-header");
  }

  public void applyTypography(double fontSize) {
    double next = Math.max(10.0d, fontSize);
    if (Math.abs(next - typographySize) < 0.01d) {
      return;
    }
    typographySize = next;
    requestLayout();
  }

  /** Synchronizes header geometry with the actual styled mode-button font. */
  public Font contentFont() {
    applyCss();
    Node modeButton = lookup(".workspace-mode-button");
    if (modeButton instanceof Labeled labeled && labeled.getFont() != null) {
      return labeled.getFont();
    }
    return Font.getDefault();
  }

  /** Returns the effective shell font after synchronizing this header's em-based geometry. */
  public Font syncTypographyFromContent() {
    Font font = contentFont();
    applyTypography(font.getSize());
    return font;
  }

  @Override
  protected double computePrefHeight(double width) {
    return headerHeight();
  }

  @Override
  protected double computeMinHeight(double width) {
    return headerHeight();
  }

  @Override
  protected double computeMaxHeight(double width) {
    return headerHeight();
  }

  @Override
  protected void layoutChildren() {
    Region brand = region("brand-zone");
    Region modes = region("lab-mode-switch");
    Region actions = region("header-actions");
    if (brand == null || modes == null || actions == null) {
      return;
    }

    double em = typographySize;
    double height = getHeight() > 0.0d ? getHeight() : headerHeight();
    double pad = 1.35d * em;
    double gap = 1.0d * em;

    Node subtitle = descendant("brand-subtitle");
    Node title = descendant("brand-title-main");
    setVisibleManaged(subtitle, true);
    setVisibleManaged(title, true);
    applyCss();

    double modeWidth = modeWidth(modes, em);
    double actionsWidth = Math.max(actions.prefWidth(-1.0d), 2.6d * em);
    double brandWidth = Math.max(brand.prefWidth(-1.0d), 8.0d * em);
    double available = Math.max(0.0d, getWidth() - 2.0d * pad);

    if (brandWidth + modeWidth + actionsWidth + 2.0d * gap > available) {
      setVisibleManaged(subtitle, false);
      applyCss();
      brandWidth = Math.max(brand.prefWidth(-1.0d), 5.2d * em);
    }
    if (brandWidth + modeWidth + actionsWidth + 2.0d * gap > available) {
      setVisibleManaged(title, false);
      applyCss();
      brandWidth = Math.max(brand.prefWidth(-1.0d), 3.2d * em);
    }

    double contentHeight = Math.max(0.0d, height);
    brand.resizeRelocate(pad, 0.0d, brandWidth, contentHeight);
    actions.resizeRelocate(
        Math.max(pad, getWidth() - pad - actionsWidth), 0.0d, actionsWidth, contentHeight);

    double minModeX = pad + brandWidth + gap;
    double maxModeX = getWidth() - pad - actionsWidth - gap - modeWidth;
    double idealModeX = (getWidth() - modeWidth) / 2.0d;
    double modeX = Math.max(minModeX, Math.min(idealModeX, maxModeX));
    modes.resizeRelocate(modeX, 0.0d, modeWidth, contentHeight);
  }

  private double modeWidth(Region modes, double em) {
    double max = 0.0d;
    int count = 0;
    if (modes instanceof Pane pane) {
      for (Node child : pane.getChildren()) {
        if (child instanceof Labeled labeled && child.isManaged()) {
          max = Math.max(max, labeled.prefWidth(-1.0d));
          count++;
        }
      }
    }
    if (count == 0) {
      return 19.0d * em;
    }
    return Math.max(17.5d * em, max * count + 2.4d * em);
  }

  private double headerHeight() {
    return clamp(HEIGHT_EM * typographySize, MIN_HEIGHT, MAX_HEIGHT);
  }

  private Region region(String styleClass) {
    for (Node child : getChildren()) {
      if (child instanceof Region region && child.getStyleClass().contains(styleClass)) {
        return region;
      }
    }
    return null;
  }

  private Node descendant(String styleClass) {
    Node found = lookup("." + styleClass);
    return found;
  }

  private static void setVisibleManaged(Node node, boolean visible) {
    if (node == null) {
      return;
    }
    node.setVisible(visible);
    node.setManaged(visible);
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
