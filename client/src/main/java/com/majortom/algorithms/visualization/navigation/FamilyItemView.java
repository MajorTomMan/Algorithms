package com.majortom.algorithms.visualization.navigation;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;

/**
 * One stable row in the family navigator.
 *
 * <p>Selection/hover/disabled are presentation-only states. The row never changes its
 * layout metrics because of those states.</p>
 */
public final class FamilyItemView extends Region {
  private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

  private static final double ROW_EM = 3.0d;
  private static final double PAD_EM = 0.82d;
  private static final double MARK_EM = 0.20d;
  private static final double INDEX_EM = 2.05d;
  private static final double ICON_EM = 2.15d;
  private static final double GAP_EM = 0.44d;

  private final String familyId;
  private final Region selectionMark = new Region();
  private final Label indexLabel = new Label();
  private final Label iconLabel = new Label();
  private final Label nameLabel = new Label();
  private Runnable action;
  private double em = 16.0d;

  FamilyItemView(FamilyEntry entry, String index) {
    familyId = entry.id();
    action = entry.action();

    getStyleClass().add("family-item");
    setAccessibleRole(AccessibleRole.BUTTON);
    setFocusTraversable(true);

    selectionMark.getStyleClass().add("family-item-selection-mark");
    selectionMark.setMouseTransparent(true);

    indexLabel.setText(index);
    indexLabel.getStyleClass().add("family-item-index");
    indexLabel.setAlignment(Pos.CENTER_RIGHT);
    indexLabel.setMouseTransparent(true);

    iconLabel.setText(entry.glyph());
    iconLabel.getStyleClass().add("family-item-icon");
    iconLabel.setAlignment(Pos.CENTER);
    iconLabel.setMouseTransparent(true);

    nameLabel.textProperty().bind(entry.name());
    nameLabel.getStyleClass().add("family-item-name");
    nameLabel.setAlignment(Pos.CENTER_LEFT);
    nameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
    nameLabel.setMouseTransparent(true);

    getChildren().addAll(selectionMark, indexLabel, iconLabel, nameLabel);
    setDisable(entry.disabled());

    setOnMouseClicked(event -> {
      if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
        fire();
      }
    });
    setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
        fire();
        event.consume();
      }
    });
  }

  void dispose() {
    nameLabel.textProperty().unbind();
  }

  public String familyId() {
    return familyId;
  }

  public void fire() {
    if (!isDisabled() && action != null) {
      action.run();
    }
  }

  public void setSelected(boolean selected) {
    pseudoClassStateChanged(SELECTED, selected);
  }

  public boolean isSelected() {
    return getPseudoClassStates().contains(SELECTED);
  }

  void setEm(double em) {
    double resolved = Math.max(8.0d, em);
    if (Math.abs(this.em - resolved) < 0.01d) {
      return;
    }
    this.em = resolved;
    setStyle("-fx-font-size: " + resolved + "px;");
    requestLayout();
  }

  double rowHeight() {
    return ROW_EM * em;
  }

  double preferredWidth() {
    double fixed = (PAD_EM * 2.0d + MARK_EM + INDEX_EM + ICON_EM + GAP_EM * 2.0d) * em;
    return fixed + Math.ceil(nameLabel.prefWidth(-1.0d));
  }

  @Override
  protected double computeMinWidth(double height) {
    return 11.0d * em;
  }

  @Override
  protected double computePrefWidth(double height) {
    return preferredWidth();
  }

  @Override
  protected double computeMaxWidth(double height) {
    return 16.0d * em;
  }

  @Override
  protected double computeMinHeight(double width) {
    return rowHeight();
  }

  @Override
  protected double computePrefHeight(double width) {
    return rowHeight();
  }

  @Override
  protected double computeMaxHeight(double width) {
    return rowHeight();
  }

  @Override
  protected void layoutChildren() {
    double width = getWidth();
    double height = getHeight();
    double pad = PAD_EM * em;
    double markWidth = MARK_EM * em;
    double indexWidth = INDEX_EM * em;
    double iconWidth = ICON_EM * em;
    double gap = GAP_EM * em;

    selectionMark.resizeRelocate(0.0d, 0.0d, markWidth, height);

    double x = pad + markWidth;
    indexLabel.resizeRelocate(x, 0.0d, indexWidth, height);
    x += indexWidth + gap;
    iconLabel.resizeRelocate(x, 0.0d, iconWidth, height);
    x += iconWidth + gap;
    nameLabel.resizeRelocate(x, 0.0d, Math.max(0.0d, width - x - pad), height);
  }
}
