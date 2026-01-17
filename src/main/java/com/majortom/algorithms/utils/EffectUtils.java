package com.majortom.algorithms.utils;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;

public class EffectUtils {

    public static void applyDynamicEffect(Button button) {
        // 1. 创建呼吸动画
        FadeTransition breathe = new FadeTransition(Duration.seconds(0.8), button);
        breathe.setFromValue(0.4);
        breathe.setToValue(1.0);
        breathe.setCycleCount(Animation.INDEFINITE);
        breathe.setAutoReverse(true);

        // 2. 监听划过状态 (Hover)
        button.hoverProperty().addListener((obs, old, isHover) -> {
            if (isHover) {
                String hexColor = getHexFromButton(button);
                // 设置背景色并开始呼吸
                button.setStyle("-fx-background-color: " + hexColor + "; -fx-text-fill: white;");
                breathe.play();
            } else if (!button.isPressed()) {
                resetButton(button, breathe);
            }
        });

        // 3. 监听点击状态 (Pressed)
        button.pressedProperty().addListener((obs, old, isPressed) -> {
            if (isPressed) {
                String hexColor = getHexFromButton(button);
                button.setStyle("-fx-background-color: " + hexColor + "; -fx-text-fill: white;");
                breathe.play();
            } else {
                // 松开瞬间效果消失
                resetButton(button, breathe);
            }
        });

        // 4. 监听焦点 (Focused) - 确保点击完成后没有任何残留效果
        button.focusedProperty().addListener((obs, old, isFocused) -> {
            resetButton(button, breathe);
        });
    }

    /**
     * 安全地从按钮中提取颜色
     */
    private static String getHexFromButton(Button button) {
        try {
            // 尝试从 Border 读取颜色
            if (button.getBorder() != null && !button.getBorder().getStrokes().isEmpty()) {
                Paint borderPaint = button.getBorder().getStrokes().get(0).getTopStroke();
                if (borderPaint instanceof Color) {
                    return toRGBCode((Color) borderPaint);
                }
            }
        } catch (Exception e) {
            // 如果读取失败（例如 CSS 还没加载完），根据 ClassName 走硬编码保底
            String classes = button.getStyleClass().toString();
            if (classes.contains("btn-ran-blue"))
                return "#00A2FF";
            if (classes.contains("btn-ran-red"))
                return "#FF0000";
            if (classes.contains("btn-ran-gold"))
                return "#FFD700";
        }
        return "#555555"; // 最终保底灰色
    }

    private static void resetButton(Button btn, FadeTransition anim) {
        anim.stop();
        btn.setOpacity(1.0);
        btn.setStyle(""); // 清空内联样式，回归 CSS 定义
    }

    private static String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
