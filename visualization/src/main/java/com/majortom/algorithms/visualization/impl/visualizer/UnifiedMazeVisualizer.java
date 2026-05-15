package com.majortom.algorithms.visualization.impl.visualizer;

import java.util.LinkedList;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.visualization.base.BaseMazeVisualizer;

import javafx.scene.paint.Color;

/**
 * 统一迷宫可视化器。
 *
 * <p>它只依赖 {@link BaseMaze#getCell(int, int)} 读取格子状态，因此可以同时渲染
 * 二维数组迷宫和图结构迷宫。图结构中的边、权重和节点属性默认完全隐藏，只复用
 * 原方格迷宫的视觉效果，让“结构切换”不会打断用户对画面的理解。</p>
 */
public class UnifiedMazeVisualizer extends BaseMazeVisualizer<BaseMaze<?>> {

    private static final int MAX_ARMY_SIZE = 6;

    private final LinkedList<int[]> armyQueue = new LinkedList<>();
    private int lastTerrain = MazeConstant.ROAD;
    private long skirmishStartTime = 0;

    /**
     * 绘制迷宫主体网格。
     *
     * @param mazeEntity 迷宫快照，可以是数组迷宫或图迷宫
     * @param a 第一个焦点
     * @param b 第二个焦点
     * @param cellW 单元格宽度
     * @param cellH 单元格高度
     */
    @Override
    protected void drawMaze(BaseMaze<?> mazeEntity, Object a, Object b, double cellW, double cellH) {
        for (int r = 0; r < mazeEntity.getRows(); r++) {
            for (int c = 0; c < mazeEntity.getCols(); c++) {
                renderRanCell(r, c, cellW, cellH, mazeEntity.getCell(r, c));
            }
        }
    }

    /**
     * 绘制单个迷宫格子。
     *
     * @param r 行坐标
     * @param c 列坐标
     * @param w 单元格宽度
     * @param h 单元格高度
     * @param type {@link MazeConstant} 中定义的格子状态
     */
    private void renderRanCell(int r, int c, double w, double h, int type) {
        double x = getX(c, w);
        double y = getY(r, h);
        double cx = x + w / 2;
        double cy = y + h / 2;
        double monSize = Math.min(w, h) * 0.5;

        switch (type) {
            case MazeConstant.WALL -> {
                gc.setFill(RAN_WALL_STONE);
                gc.fillRect(x, y, w, h);
                drawClanMon(cx, cy, monSize, MazeConstant.WALL, RAN_ENEMY_RUST.deriveColor(0, 1, 1, 0.5));
            }
            case MazeConstant.START -> {
                gc.setFill(RAN_WHITE);
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize * 1.2, MazeConstant.START, RAN_BLACK);
            }
            case MazeConstant.END -> {
                gc.setFill(RAN_VIOLET.saturate());
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize * 1.2, MazeConstant.END, RAN_WHITE);
            }
            case MazeConstant.ROAD -> {
                gc.setFill(RAN_BLUE.deriveColor(0, 1.2, 1.0, 0.45));
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize * 1.2, MazeConstant.ROAD, RAN_BLUE.saturate());
            }
            case MazeConstant.PATH -> {
                gc.setFill(RAN_RED.saturate());
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize * 1.2, MazeConstant.PATH, Color.rgb(40, 0, 0, 0.95));
            }
            case MazeConstant.BACKTRACK, MazeConstant.DEADEND -> {
                gc.setFill(RAN_YELLOW.saturate());
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize * 1.2, MazeConstant.BACKTRACK, Color.rgb(80, 50, 0, 0.9));
            }
            default -> {
                gc.setFill(RAN_WALL_STONE);
                gc.fillRect(x, y, w, h);
            }
        }
    }

    /**
     * 绘制算法焦点。
     *
     * <p>数组迷宫算法会传入 {@code row, col} 两个整数；未来图迷宫算法如果传入
     * {@code int[]{row, col}}，这里也能渲染同样的焦点效果。</p>
     *
     * @param currA 第一个焦点对象
     * @param currB 第二个焦点对象
     * @param w 单元格宽度
     * @param h 单元格高度
     */
    @Override
    protected void drawFocus(Object currA, Object currB, double w, double h) {
        int[] position = resolveFocusPosition(currA, currB);

        if (position == null || getLastData() == null) {
            return;
        }

        int r = position[0];
        int c = position[1];

        if (getLastData().isOverBorder(r, c)) {
            return;
        }

        if (armyQueue.isEmpty() || armyQueue.getLast()[0] != r || armyQueue.getLast()[1] != c) {
            armyQueue.add(new int[] { r, c });
            if (armyQueue.size() > MAX_ARMY_SIZE) {
                armyQueue.removeFirst();
            }
        }

        int currentTerrain = getLastData().getCell(r, c);
        boolean isBreaching = currentTerrain == MazeConstant.WALL;
        long now = System.currentTimeMillis();

        if (isBreaching && lastTerrain != MazeConstant.WALL) {
            skirmishStartTime = now;
        }
        lastTerrain = currentTerrain;

        double x = getX(c, w);
        double y = getY(r, h);
        double cx = x + w / 2;
        double cy = y + h / 2;

        gc.save();

        for (int i = 0; i < armyQueue.size() - 1; i++) {
            int[] pos = armyQueue.get(i);
            double alpha = 0.5 + ((double) i / armyQueue.size() * 0.5);
            gc.setGlobalAlpha(alpha);

            double qX = getX(pos[1], w);
            double qY = getY(pos[0], h);
            gc.setFill(RAN_CYAN.deriveColor(0, 1, 1, 0.2));
            gc.fillOval(qX + w * 0.2, qY + h * 0.2, w * 0.6, h * 0.6);
            renderArmyFlag(qX, qY, w, h, i);
        }
        gc.setGlobalAlpha(1.0);

        applyFocusEffect();

        boolean shaking = currentTerrain == MazeConstant.WALL || currentTerrain == MazeConstant.BACKTRACK;
        double wobble = shaking ? Math.sin(now * 0.05) * 1.5 : 0;
        Color kabutoBaseColor = isBreaching ? RAN_BLOOD_VIVID : RAN_BLUE;

        gc.setFill(isBreaching ? RAN_BLOOD_VIVID.deriveColor(0, 1, 1, 0.3)
                : RAN_CYAN.deriveColor(0, 1, 1, 0.3));
        gc.fillOval(cx - w * 0.4, cy - h * 0.4, w * 0.8, h * 0.8);

        drawKabuto(cx + wobble, cy, w, h, kabutoBaseColor);

        if (isBreaching) {
            drawCombatSparks(cx, cy, w, h, now);
        }

        gc.setStroke(isBreaching ? RAN_GOLD : RAN_WHITE);
        gc.setLineWidth(3.0);
        gc.strokeRect(x + 1.5, y + 1.5, w - 3, h - 3);
        gc.restore();
    }

    /**
     * 将不同算法传入的焦点对象统一转换为行列坐标。
     *
     * @param currA 第一个焦点对象
     * @param currB 第二个焦点对象
     * @return {@code [row, col]}；无法识别时返回 null
     */
    private int[] resolveFocusPosition(Object currA, Object currB) {
        if (currA instanceof Integer r && currB instanceof Integer c) {
            return new int[] { r, c };
        }

        if (currA instanceof int[] position && position.length >= 2) {
            return new int[] { position[0], position[1] };
        }

        return null;
    }

    /**
     * 绘制焦点位置的武将头盔。
     */
    private void drawKabuto(double cx, double cy, double w, double h, Color baseColor) {
        double s = Math.min(w, h) * 0.85;
        gc.setFill(RAN_BLACK.deriveColor(0, 1, 0.25, 1.0));
        gc.setStroke(RAN_IRON);
        gc.setLineWidth(1.5);

        gc.beginPath();
        gc.moveTo(cx - s / 3, cy + s / 10);
        gc.arc(cx, cy + s / 10, s / 3, s / 3, 180, 180);
        gc.closePath();
        gc.fill();
        gc.stroke();

        gc.setFill(baseColor.deriveColor(0, 1.2, 0.85, 0.9));
        double fkSize = s / 4;

        gc.fillPolygon(
                new double[] { -s / 3 + cx, -fkSize + cx, -fkSize + cx, -s / 3 + cx },
                new double[] { -s / 10 + cy, -s / 10 + cy, s / 6 + cy, s / 12 + cy }, 4);
        gc.fillPolygon(
                new double[] { s / 3 + cx, fkSize + cx, fkSize + cx, s / 3 + cx },
                new double[] { -s / 10 + cy, -s / 10 + cy, s / 6 + cy, s / 12 + cy }, 4);

        gc.setStroke(RAN_BRONZE);
        gc.setLineWidth(3.0);
        gc.strokeLine(cx, cy - s / 3, cx, cy - s / 4);

        gc.setStroke(RAN_WHITE);
        gc.setLineWidth(3.8);

        double hornAngle = 15.0 + Math.sin(System.currentTimeMillis() * 0.005) * 2.5;
        double hornLength = (s / 2) * 1.6;

        double leftHornX = cx - Math.sin(Math.toRadians(hornAngle)) * hornLength;
        double leftHornY = cy - s / 4 - Math.cos(Math.toRadians(hornAngle)) * hornLength;
        gc.strokeLine(cx, cy - s / 4, leftHornX, leftHornY);

        double rightHornX = cx + Math.sin(Math.toRadians(hornAngle)) * hornLength;
        double rightHornY = cy - s / 4 - Math.cos(Math.toRadians(hornAngle)) * hornLength;
        gc.strokeLine(cx, cy - s / 4, rightHornX, rightHornY);

        gc.setFill(RAN_GOLD);
        gc.fillOval(leftHornX - 2, leftHornY - 2, 4, 4);
        gc.fillOval(rightHornX - 2, rightHornY - 2, 4, 4);
    }

    /**
     * 绘制行军队列中的旗帜。
     */
    private void renderArmyFlag(double x, double y, double w, double h, int idx) {
        double poleX = x + w * 0.75;
        double wave = Math.sin(System.currentTimeMillis() * 0.015 + idx) * (w * 0.15);

        gc.setStroke(RAN_IRON);
        gc.setLineWidth(2.0);
        gc.strokeLine(poleX, y + h * 0.9, poleX, y + h * 0.1);

        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillPolygon(
                new double[] { poleX + 2, poleX - w * 0.5 + wave + 2, poleX + 2 },
                new double[] { y + h * 0.15 + 2, y + h * 0.4 + 2, y + h * 0.65 + 2 }, 3);

        gc.setFill(RAN_CYAN.interpolate(RAN_WHITE, 0.5));
        gc.setGlobalAlpha(1.0);
        gc.fillPolygon(
                new double[] { poleX, poleX - w * 0.5 + wave, poleX },
                new double[] { y + h * 0.1, y + h * 0.4, y + h * 0.7 }, 3);

        gc.setStroke(RAN_GOLD);
        gc.setLineWidth(1.0);
        gc.strokePolygon(
                new double[] { poleX, poleX - w * 0.5 + wave, poleX },
                new double[] { y + h * 0.1, y + h * 0.4, y + h * 0.7 }, 3);
    }

    /**
     * 绘制破墙时的火花。
     */
    private void drawCombatSparks(double cx, double cy, double w, double h, long now) {
        gc.setStroke(RAN_GOLD);
        gc.setLineWidth(2.0);
        double skirmishDuration = (now - skirmishStartTime) / 1000.0;
        double intensity = Math.max(0, 1.0 - skirmishDuration);

        for (int i = 0; i < 6; i++) {
            double angle = (now * 0.6 + i * 60) % 360;
            double rad = Math.toRadians(angle);
            double len = w * (0.4 + intensity * 0.2) + Math.random() * 5;
            gc.strokeLine(cx + Math.cos(rad) * w * 0.15, cy + Math.sin(rad) * h * 0.15,
                    cx + Math.cos(rad) * len, cy + Math.sin(rad) * len);
        }

        gc.setFill(RAN_ENEMY_RUST);
        for (int i = 0; i < 4; i++) {
            double rx = cx + (Math.random() - 0.5) * w * 0.9;
            double ry = cy + (Math.random() - 0.5) * h * 0.9;
            double size = 2 + Math.random() * 1.5;
            gc.fillOval(rx, ry, size, size);
        }
    }
}
