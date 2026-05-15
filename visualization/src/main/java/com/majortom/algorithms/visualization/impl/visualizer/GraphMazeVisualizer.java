package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.core.maze.impl.GraphMaze;
import com.majortom.algorithms.core.maze.structure.MazeCell;
import com.majortom.algorithms.visualization.base.BaseMazeVisualizer;

import javafx.scene.paint.Color;

/**
 * 图结构迷宫可视化器。
 *
 * <p>第一版刻意保持和 {@link SquareMazeVisualizer} 相同的方格视觉，不显示底层图的边和权重。
 * 图结构只作为数据底座存在，后续如果需要教学模式，可以再显式增加边/权重开关。</p>
 */
public class GraphMazeVisualizer extends BaseMazeVisualizer<GraphMaze> {

    /**
     * 最近一段焦点轨迹，用来复刻二维数组迷宫里的行军队列效果。
     */
    private final java.util.LinkedList<MazeCell> armyQueue = new java.util.LinkedList<>();

    /**
     * 行军轨迹最多保留的格子数量。
     */
    private static final int MAX_ARMY_SIZE = 6;

    /**
     * 最近一次焦点所在地形。
     */
    private int lastTerrain = MazeConstant.ROAD;

    /**
     * 最近一次破墙/阻挡反馈开始时间。
     */
    private long skirmishStartTime = 0;

    /**
     * 绘制图迷宫主体。
     */
    @Override
    protected void drawMaze(GraphMaze maze, Object a, Object b, double cellW, double cellH) {
        if (maze == null) {
            return;
        }

        for (int r = 0; r < maze.getRows(); r++) {
            for (int c = 0; c < maze.getCols(); c++) {
                MazeCell cell = maze.getMazeCell(r, c);
                int type = cell == null ? MazeConstant.WALL : cell.getType();
                renderRanCell(r, c, cellW, cellH, type);
            }
        }
    }

    /**
     * 绘制单个格子，颜色和符号保持与二维数组迷宫一致。
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
                gc.setFill(RAN_BLUE.deriveColor(0, 1, 1, 0.22));
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
            }
        }
    }

    /**
     * 绘制当前焦点。
     */
    @Override
    protected void drawFocus(Object currA, Object currB, double w, double h) {
        GraphMaze maze = getLastData();
        MazeCell focus = resolveFocusCell(maze, currA, currB);
        if (focus == null) {
            return;
        }

        if (armyQueue.isEmpty()
                || armyQueue.getLast().getRow() != focus.getRow()
                || armyQueue.getLast().getCol() != focus.getCol()) {
            armyQueue.add(focus);
            if (armyQueue.size() > MAX_ARMY_SIZE) {
                armyQueue.removeFirst();
            }
        }

        int currentTerrain = focus.getType();
        boolean isBreaching = currentTerrain == MazeConstant.WALL;
        long now = System.currentTimeMillis();

        if (isBreaching && lastTerrain != MazeConstant.WALL) {
            skirmishStartTime = now;
        }
        lastTerrain = currentTerrain;

        double x = getX(focus.getCol(), w);
        double y = getY(focus.getRow(), h);
        double cx = x + w / 2;
        double cy = y + h / 2;

        gc.save();
        renderArmyQueue(w, h);
        applyFocusEffect();

        boolean shaking = currentTerrain == MazeConstant.WALL || currentTerrain == MazeConstant.BACKTRACK;
        double wobble = shaking ? Math.sin(now * 0.05) * 1.5 : 0;
        Color kabutoBaseColor = isBreaching ? RAN_BLOOD_VIVID : RAN_BLUE;

        gc.setFill(isBreaching ? RAN_BLOOD_VIVID.deriveColor(0, 1, 1, 0.3) : RAN_CYAN.deriveColor(0, 1, 1, 0.3));
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
     * 将算法传入的焦点参数转换为迷宫单元格。
     */
    private MazeCell resolveFocusCell(GraphMaze maze, Object currA, Object currB) {
        if (maze == null) {
            return null;
        }
        if (currA instanceof MazeCell cell) {
            return cell;
        }
        if (currA instanceof String id) {
            return maze.getMazeCell(id);
        }
        if (currA instanceof int[] pos && pos.length >= 2) {
            return maze.getMazeCell(pos[0], pos[1]);
        }
        if (currA instanceof Integer r && currB instanceof Integer c) {
            return maze.getMazeCell(r, c);
        }
        return null;
    }

    /**
     * 绘制焦点移动轨迹。
     */
    private void renderArmyQueue(double w, double h) {
        for (int i = 0; i < armyQueue.size() - 1; i++) {
            MazeCell cell = armyQueue.get(i);
            double alpha = 0.5 + ((double) i / armyQueue.size() * 0.5);
            gc.setGlobalAlpha(alpha);

            double qX = getX(cell.getCol(), w);
            double qY = getY(cell.getRow(), h);
            gc.setFill(RAN_CYAN.deriveColor(0, 1, 1, 0.2));
            gc.fillOval(qX + w * 0.2, qY + h * 0.2, w * 0.6, h * 0.6);
            renderArmyFlag(qX, qY, w, h, i);
        }
        gc.setGlobalAlpha(1.0);
    }

    /**
     * 绘制当前焦点位置的头盔符号。
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
                new double[] { -s / 10 + cy, -s / 10 + cy, s / 6 + cy, s / 12 + cy },
                4);
        gc.fillPolygon(
                new double[] { s / 3 + cx, fkSize + cx, fkSize + cx, s / 3 + cx },
                new double[] { -s / 10 + cy, -s / 10 + cy, s / 6 + cy, s / 12 + cy },
                4);

        gc.setStroke(RAN_BRONZE);
        gc.setLineWidth(3.0);
        gc.strokeLine(cx, cy - s / 3, cx, cy - s / 4);

        gc.setStroke(RAN_WHITE);
        gc.setLineWidth(3.8);

        double hornAngle = 15.0 + Math.sin(System.currentTimeMillis() * 0.005) * 2.5;
        double hornLength = (s / 2) * 1.6;
        double leftHornX = cx - Math.sin(Math.toRadians(hornAngle)) * hornLength;
        double leftHornY = cy - s / 4 - Math.cos(Math.toRadians(hornAngle)) * hornLength;
        double rightHornX = cx + Math.sin(Math.toRadians(hornAngle)) * hornLength;
        double rightHornY = cy - s / 4 - Math.cos(Math.toRadians(hornAngle)) * hornLength;

        gc.strokeLine(cx, cy - s / 4, leftHornX, leftHornY);
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
                new double[] { y + h * 0.15 + 2, y + h * 0.4 + 2, y + h * 0.65 + 2 },
                3);

        gc.setFill(RAN_CYAN.interpolate(RAN_WHITE, 0.5));
        gc.setGlobalAlpha(1.0);
        gc.fillPolygon(
                new double[] { poleX, poleX - w * 0.5 + wave, poleX },
                new double[] { y + h * 0.1, y + h * 0.4, y + h * 0.7 },
                3);

        gc.setStroke(RAN_GOLD);
        gc.setLineWidth(1.0);
        gc.strokePolygon(
                new double[] { poleX, poleX - w * 0.5 + wave, poleX },
                new double[] { y + h * 0.1, y + h * 0.4, y + h * 0.7 },
                3);
    }

    /**
     * 绘制破墙或阻挡反馈火花。
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
            gc.strokeLine(
                    cx + Math.cos(rad) * w * 0.15,
                    cy + Math.sin(rad) * h * 0.15,
                    cx + Math.cos(rad) * len,
                    cy + Math.sin(rad) * len);
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
