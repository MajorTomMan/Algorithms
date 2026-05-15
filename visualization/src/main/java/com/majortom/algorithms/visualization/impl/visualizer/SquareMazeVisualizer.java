package com.majortom.algorithms.visualization.impl.visualizer;

import java.util.LinkedList;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeConstant;
import com.majortom.algorithms.visualization.VisualizationEvent;
import com.majortom.algorithms.visualization.base.BaseMazeVisualizer;
import javafx.scene.paint.Color;

public class SquareMazeVisualizer extends BaseMazeVisualizer<BaseMaze<int[][]>> {

    private final LinkedList<int[]> armyQueue = new LinkedList<>();
    private static final int MAX_ARMY_SIZE = 6;
    private int lastTerrain = MazeConstant.ROAD;
    private long skirmishStartTime = 0;
    private long operationAccentUntilMillis = 0L;
    private String operationLabel = "";

    @Override
    protected void drawMaze(BaseMaze<int[][]> mazeEntity, Object a, Object b, double cellW, double cellH) {
        int[][] grid = mazeEntity.getData();
        if (grid == null)
            return;
        for (int r = 0; r < mazeEntity.getRows(); r++) {
            for (int c = 0; c < mazeEntity.getCols(); c++) {
                renderRanCell(r, c, cellW, cellH, grid[r][c]);
            }
        }
        drawOperationAccent(cellW, cellH);
    }

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
        }
    }

    // --- 修复：保持与父类抽象方法签名一致 ---
    @Override
    protected void drawFocus(Object currA, Object currB, double w, double h) {
        if (!(currA instanceof Integer r && currB instanceof Integer c))
            return;

        // 状态更新：注意 LinkedList 的方法调用
        if (armyQueue.isEmpty() || armyQueue.getLast()[0] != r || armyQueue.getLast()[1] != c) {
            armyQueue.add(new int[] { r, c });
            if (armyQueue.size() > MAX_ARMY_SIZE)
                armyQueue.removeFirst();
        }

        int[][] grid = getLastData().getData();
        int currentTerrain = grid[r][c];
        boolean isBreaching = (currentTerrain == MazeConstant.WALL);
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

        // 绘制军队队列
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

        boolean shaking = (currentTerrain == MazeConstant.WALL || currentTerrain == MazeConstant.BACKTRACK);
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

    @Override
    public void onControlAction(VisualizationEvent event) {
        super.onControlAction(event);
        operationAccentUntilMillis = System.currentTimeMillis() + FEEDBACK_DURATION_MS;
        Object rows = event.metadata().get("rows");
        Object cols = event.metadata().get("cols");
        if (rows instanceof Integer rowCount && cols instanceof Integer colCount) {
            operationLabel = rowCount + "x" + colCount;
        }
    }

    @Override
    public void onVisualizationReset() {
        operationAccentUntilMillis = 0L;
        operationLabel = "";
        super.onVisualizationReset();
    }

    private void drawOperationAccent(double cellW, double cellH) {
        if (System.currentTimeMillis() >= operationAccentUntilMillis) {
            return;
        }

        gc.save();
        gc.setGlobalAlpha(Math.max(0.0, Math.min(1.0, (operationAccentUntilMillis - System.currentTimeMillis()) / (double) FEEDBACK_DURATION_MS)));
        gc.setStroke(RAN_GOLD);
        gc.setLineWidth(2.4);
        gc.strokeRoundRect(10, 10, Math.max(0, canvas.getWidth() - 20), Math.max(0, canvas.getHeight() - 20), 20, 20);

        if (!operationLabel.isBlank()) {
            gc.setFill(RAN_BLACK.deriveColor(0, 1, 1, 0.78));
            gc.fillRoundRect(canvas.getWidth() - 118, 18, 96, 32, 12, 12);
            gc.setFill(RAN_GOLD);
            gc.fillText(operationLabel, canvas.getWidth() - 98, 39);
        }
        gc.restore();
    }

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

    private void renderArmyFlag(double x, double y, double w, double h, int idx) {
        double poleX = x + w * 0.75;
        // 增加摇摆幅度，使其更灵动
        double wave = Math.sin(System.currentTimeMillis() * 0.015 + idx) * (w * 0.15);

        // 1. 强化旗杆：改用生铁色，增加厚度
        gc.setStroke(RAN_IRON);
        gc.setLineWidth(2.0);
        gc.strokeLine(poleX, y + h * 0.9, poleX, y + h * 0.1);

        // 2. 绘制旗面投影（可选，增加悬浮感）
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillPolygon(
                new double[] { poleX + 2, poleX - w * 0.5 + wave + 2, poleX + 2 },
                new double[] { y + h * 0.15 + 2, y + h * 0.4 + 2, y + h * 0.65 + 2 }, 3);

        // 3. 核心：高亮度旗面
        // 使用不透明的颜色，idx 越小（越靠后）颜色越暗，形成深浅透视
        Color flagColor = RAN_CYAN.interpolate(RAN_WHITE, 0.5); // 荧蓝白
        gc.setFill(flagColor);
        gc.setGlobalAlpha(1.0); // 强制不透明，确保对比度

        gc.fillPolygon(
                new double[] { poleX, poleX - w * 0.5 + wave, poleX },
                new double[] { y + h * 0.1, y + h * 0.4, y + h * 0.7 }, 3);

        // 4. 增加“描金”边缘：这是让它不淡的关键
        gc.setStroke(RAN_GOLD);
        gc.setLineWidth(1.0);
        gc.strokePolygon(
                new double[] { poleX, poleX - w * 0.5 + wave, poleX },
                new double[] { y + h * 0.1, y + h * 0.4, y + h * 0.7 }, 3);
    }

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
