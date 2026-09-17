package com.majortom.algorithms.utils;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.util.Duration;

/**
 * 视觉特效工具类。
 *
 * <p>按钮颜色、边框和 hover 样式完全交给 CSS pseudo-class 处理；这里仅负责不会参与
 * CSS/layout 计算的透明度呼吸反馈。不要在 hover/pressed 回调中调用 setStyle()，否则会
 * 强制触发 CSS 重新计算，在响应式布局临界值附近可能形成 hover -> relayout -> mouse-exit
 * -> relayout 的反馈循环。
 */
public final class EffectUtils {
  private static final String EFFECT_INSTALLED_KEY = EffectUtils.class.getName() + ".installed";

  private EffectUtils() {
  }

  /** 批量应用动态效果。 */
  public static void applyDynamicEffect(Button... buttons) {
    for (Button button : buttons) {
      if (button != null) {
        applyToSingle(button);
      }
    }
  }

  private static void applyToSingle(Button button) {
    if (Boolean.TRUE.equals(button.getProperties().get(EFFECT_INSTALLED_KEY))) {
      return;
    }
    button.getProperties().put(EFFECT_INSTALLED_KEY, Boolean.TRUE);
    button.setCursor(Cursor.HAND);

    FadeTransition breathe = new FadeTransition(Duration.seconds(0.8), button);
    breathe.setFromValue(1.0);
    breathe.setToValue(0.5);
    breathe.setCycleCount(Animation.INDEFINITE);
    breathe.setAutoReverse(true);

    button.hoverProperty().addListener((obs, wasHover, isHover) -> {
      if (isHover) {
        activateEffect(breathe);
      } else {
        deactivateEffect(button, breathe);
      }
    });

    button.pressedProperty().addListener((obs, wasPressed, isPressed) -> {
      if (isPressed) {
        breathe.setRate(2.0);
      } else {
        breathe.setRate(1.0);
        if (!button.isHover()) {
          deactivateEffect(button, breathe);
        }
      }
    });

    button.focusedProperty().addListener((obs, old, isFocused) -> {
      if (!isFocused && !button.isHover()) {
        deactivateEffect(button, breathe);
      }
    });
  }

  private static void activateEffect(FadeTransition animation) {
    animation.play();
  }

  private static void deactivateEffect(Button button, FadeTransition animation) {
    animation.stop();
    button.setOpacity(1.0);
  }
}
