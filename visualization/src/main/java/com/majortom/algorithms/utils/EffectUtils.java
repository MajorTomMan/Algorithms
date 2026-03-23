package com.majortom.algorithms.utils;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;

/**
 * 视觉特效工具类
 * 职责：动态适配按钮视觉反馈，提供呼吸灯效果。
 */
public class EffectUtils {

    /**
     * 批量应用动态效果
     */
    public static void applyDynamicEffect(Button... buttons) {
        for (Button button : buttons) {
            if (button != null) {
                applyToSingle(button);
            }
        }
    }

    private static void applyToSingle(Button button) {
        // 1. 配置呼吸动画
        FadeTransition breathe = new FadeTransition(Duration.seconds(0.8), button);
        breathe.setFromValue(1.0);
        breathe.setToValue(0.5); // 稍微变淡，不影响文字读取
        breathe.setCycleCount(Animation.INDEFINITE);
        breathe.setAutoReverse(true);

        // 2. 划过状态监听
        button.hoverProperty().addListener((obs, wasHover, isHover) -> {
            if (isHover) {
                activateEffect(button, breathe);
            } else {
                deactivateEffect(button, breathe);
            }
        });

        // 3. 点击状态监听
        button.pressedProperty().addListener((obs, wasPressed, isPressed) -> {
            if (isPressed) {
                breathe.setRate(2.0); // 点击时加快呼吸节奏
            } else {
                breathe.setRate(1.0);
                if (!button.isHover()) {
                    deactivateEffect(button, breathe);
                }
            }
        });

        // 4. 失去焦点时强制重置
        button.focusedProperty().addListener((obs, old, isFocused) -> {
            if (!isFocused && !button.isHover()) {
                deactivateEffect(button, breathe);
            }
        });
    }

    private static void activateEffect(Button btn, FadeTransition anim) {
        // 动态获取当前按钮的视觉颜色作为高亮基准
        String highlightColor = getDynamicColor(btn);

        // 使用内联样式动态设置高亮，同时保持文字清晰
        btn.setStyle(String.format("-fx-background-color: %s; -fx-cursor: hand;", highlightColor));
        anim.play();
    }

    private static void deactivateEffect(Button btn, FadeTransition anim) {
        anim.stop();
        btn.setOpacity(1.0);
        btn.setStyle(""); // 回归 CSS 定义的原始样式
    }

    /**
     * 动态取色逻辑：优先取边框色，其次取背景色，最后保底
     */
    private static String getDynamicColor(Button button) {
        // 尝试获取背景颜色
        Paint fill = button.getBackground() != null && !button.getBackground().getFills().isEmpty()
                ? button.getBackground().getFills().get(0).getFill()
                : null;

        // 尝试获取边框颜色
        Paint stroke = button.getBorder() != null && !button.getBorder().getStrokes().isEmpty()
                ? button.getBorder().getStrokes().get(0).getTopStroke()
                : null;

        Paint target = (stroke instanceof Color) ? stroke : fill;

        if (target instanceof Color) {
            return toRGBCode((Color) target);
        }

        // 如果动态获取失败（如初始化未完成），尝试从 StyleClass 解析常用色值
        return resolveFromClass(button.getStyleClass().toString());
    }

    private static String resolveFromClass(String styleClass) {
        if (styleClass.contains("primary") || styleClass.contains("blue"))
            return "#00A2FF";
        if (styleClass.contains("gold") || styleClass.contains("warning"))
            return "#FFD700";
        if (styleClass.contains("red") || styleClass.contains("danger"))
            return "#FF4B4B";
        return "#666666"; // 中立灰
    }

    private static String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}