package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.visualization.base.BaseSortVisualizer;
import javafx.animation.AnimationTimer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * 优化比例版 - 动态三角家纹旗
 * 符号学统一版：使用 BaseVisualizer 定义的统一家纹逻辑
 */
public class HistogramSortVisualizer<T extends Comparable<T>> extends BaseSortVisualizer<T> {

    private double waveTimer = 0;
    private AnimationTimer dynamicWind;
    private BaseSort<T> lastSortData;
    private Object lastA, lastB;

    public HistogramSortVisualizer() {
        dynamicWind = new AnimationTimer() {
            @Override
            public void handle(long now) {
                waveTimer += 0.04; // 极致忧郁的慢速摆动
                if (lastSortData != null) {
                    drawSortContent(lastSortData, lastA, lastB);
                }
            }
        };
        dynamicWind.start();
    }

    @Override
    protected void drawSortContent(BaseSort<T> sortData, Object a, Object b) {
        this.lastSortData = sortData;
        this.lastA = a;
        this.lastB = b;

        T[] data = sortData.getData();
        int n = data.length;

        double canvasW = canvas.getWidth();
        double canvasH = canvas.getHeight();

        double paddingSide = 40.0;
        double horizonY = canvasH - 100.0;
        double maxAvailableH = canvasH - 180.0;

        double barW = (canvasW - 2 * paddingSide) / n;
        double maxVal = getMaxValue(data);
        double scale = maxVal == 0 ? 1 : maxAvailableH / maxVal;

        int indexA = (a instanceof Integer) ? (int) a : -1;
        int indexB = (b instanceof Integer) ? (int) b : -1;

        gc.clearRect(0, 0, canvasW, canvasH);

        for (int i = 0; i < n; i++) {
            double val = Double.parseDouble(data[i].toString());
            double h = Math.max(25, val * scale);
            double x = paddingSide + i * barW;

            // 获取《乱》体系下的角色色
            Color statusColor = getRanColor(i, sortData, a, b);
            boolean isFocused = (i == indexA || i == indexB);

            renderCompactClanFlag(x, horizonY, barW, h, statusColor, isFocused, i);
        }
    }

    private void renderCompactClanFlag(double x, double horizonY, double w, double h, Color color, boolean isFocused,
            int index) {
        double poleX = x + w / 2;
        double topY = horizonY - h;

        // 旗杆
        gc.setStroke(RAN_WHITE.deriveColor(0, 1, 1, 0.2));
        gc.setLineWidth(Math.max(0.6, w * 0.1));
        gc.strokeLine(poleX, horizonY, poleX, topY);

        // 旗面比例逻辑
        double flagLen = w > 10 ? w * 1.5 : w * 1.1;
        double flagH = Math.min(h * 0.5, 45);

        // 动态摆动
        double wave = Math.sin(waveTimer + index * 0.5) * (flagLen * 0.08);

        // 绘制三角旗面
        gc.beginPath();
        gc.moveTo(poleX, topY);
        gc.lineTo(poleX - flagLen + wave, topY + flagH / 2);
        gc.lineTo(poleX, topY + flagH);
        gc.closePath();

        LinearGradient grad = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, color.deriveColor(0, 1, 0.9, 0.95)),
                new Stop(1, color.deriveColor(0, 0.8, 1.2, 0.6)));
        gc.setFill(grad);
        gc.fill();

        // --- 统一家纹绘制逻辑 ---
        if (w > 15) {
            double monX = poleX - flagLen * 0.4 + (wave * 0.3);
            double monY = topY + flagH / 2;
            double monSize = flagH * 0.35;

            // 调用基类 BaseVisualizer 中的 drawClanMon
            // 确保符号与迷宫完全同步：红圆、蓝横、黄三角
            drawClanMon(monX, monY, monSize, color, RAN_BLACK.deriveColor(0, 1, 1, 0.5));
        }

        // 焦点效果保持不变
        if (isFocused) {
            gc.setEffect(new javafx.scene.effect.Bloom(0.3));
            gc.setStroke(RAN_WHITE);
            gc.setLineWidth(1.2);
            gc.strokeOval(poleX - 4, topY - 4, 8, 8);
            gc.setEffect(null);
        }
    }
}