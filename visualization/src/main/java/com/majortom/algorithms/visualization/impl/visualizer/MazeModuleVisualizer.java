package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.constants.MazeCellType;
import com.majortom.algorithms.core.maze.constants.MazeDefaults;
import com.majortom.algorithms.core.maze.impl.GraphMaze;
import com.majortom.algorithms.core.maze.structure.MazeCell;
import com.majortom.algorithms.visualization.BaseVisualizer;

import javafx.scene.paint.Color;

import java.util.LinkedList;

/**
 * 迷宫模块统一可视化器。
 *
 * <p>它服务于同一个“迷宫”模块下的两种底层结构：二维数组迷宫和图结构迷宫。
 * 对控制器来说，两种结构都交给这一层渲染；对可视化器来说，数组迷宫直接按格子读取状态，
 * 图迷宫则先读取节点布局信息或二维投影，再画出同样的方格外观。</p>
 *
 * <p>这样做的目的是把“模块入口统一”和“底层结构独立”同时保住：
 * 用户仍然在迷宫模块里切换结构，算法层却不需要再把图迷宫塞回二维迷宫抽象。</p>
 */
public class MazeModuleVisualizer extends BaseVisualizer<BaseStructure<?>> {

    private static final int MAX_ARMY_SIZE = 6;

    private final LinkedList<int[]> armyQueue = new LinkedList<>();
    private int lastTerrain = MazeDefaults.DEFAULT_TERRAIN;
    private long skirmishStartTime = 0;

    @Override
    protected void draw(BaseStructure<?> data, Object a, Object b) {
        clear();
        if (data == null) {
            return;
        }

        int rows = getRows(data);
        int cols = getCols(data);
        if (rows <= 0 || cols <= 0) {
            return;
        }

        double cellW = canvas.getWidth() / cols;
        double cellH = canvas.getHeight() / rows;

        drawMaze(data, cellW, cellH);
        drawFocus(data, a, b, cellW, cellH);
        drawTransientFeedbackOverlay();
    }

    /**
     * 根据结构类型绘制迷宫主体。
     */
    private void drawMaze(BaseStructure<?> data, double cellW, double cellH) {
        if (data instanceof BaseMaze<?> maze) {
            drawArrayBackedMaze(maze, cellW, cellH);
            return;
        }
        if (data instanceof GraphMaze graphMaze) {
            drawGraphBackedMaze(graphMaze, cellW, cellH);
        }
    }

    /**
     * 绘制数组迷宫。
     */
    private void drawArrayBackedMaze(BaseMaze<?> maze, double cellW, double cellH) {
        for (int r = 0; r < maze.getRows(); r++) {
            for (int c = 0; c < maze.getCols(); c++) {
                renderRanCell(r, c, cellW, cellH, maze.getCell(r, c));
            }
        }
    }

    /**
     * 绘制图迷宫。
     *
     * <p>图迷宫不会直接暴露数组式邻接关系，因此这里读取结构层提供的二维投影，
     * 只把它当成展示数据使用。</p>
     */
    private void drawGraphBackedMaze(GraphMaze graphMaze, double cellW, double cellH) {
        int[][] grid = graphMaze.toCellTypeGrid();
        for (int r = 0; r < graphMaze.getRows(); r++) {
            for (int c = 0; c < graphMaze.getCols(); c++) {
                renderRanCell(r, c, cellW, cellH, grid[r][c]);
            }
        }
    }

    /**
     * 绘制当前算法焦点。
     *
     * <p>数组迷宫通常传入行列坐标；图迷宫更推荐传节点 ID 或 {@link MazeCell}。
     * 这里统一把它们转换成布局坐标，再复用相同的焦点视觉。</p>
     */
    private void drawFocus(BaseStructure<?> data, Object currA, Object currB, double w, double h) {
        int[] position = resolveFocusPosition(data, currA, currB);
        if (position == null || isOverBorder(data, position[0], position[1])) {
            return;
        }

        int r = position[0];
        int c = position[1];

        if (armyQueue.isEmpty() || armyQueue.getLast()[0] != r || armyQueue.getLast()[1] != c) {
            armyQueue.add(new int[] { r, c });
            if (armyQueue.size() > MAX_ARMY_SIZE) {
                armyQueue.removeFirst();
            }
        }

        int currentTerrain = terrainAt(data, r, c);
        boolean isBreaching = currentTerrain == MazeCellType.WALL;
        long now = System.currentTimeMillis();

        if (isBreaching && lastTerrain != MazeCellType.WALL) {
            skirmishStartTime = now;
        }
        lastTerrain = currentTerrain;

        double x = getX(c, w);
        double y = getY(r, h);
        double cx = x + w / 2;
        double cy = y + h / 2;

        gc.save();
        renderArmyQueue(w, h);
        applyFocusEffect();

        boolean shaking = currentTerrain == MazeCellType.WALL || currentTerrain == MazeCellType.BACKTRACK;
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
     * 把焦点参数统一解析成布局坐标。
     */
    private int[] resolveFocusPosition(BaseStructure<?> data, Object currA, Object currB) {
        if (currA instanceof Integer r && currB instanceof Integer c) {
            return new int[] { r, c };
        }
        if (currA instanceof int[] position && position.length >= 2) {
            return new int[] { position[0], position[1] };
        }
        if (data instanceof GraphMaze graphMaze) {
            if (currA instanceof MazeCell cell) {
                return new int[] { cell.getRow(), cell.getCol() };
            }
            if (currA instanceof String id) {
                MazeCell cell = graphMaze.getMazeCell(id);
                if (cell != null) {
                    return new int[] { cell.getRow(), cell.getCol() };
                }
            }
        }
        return null;
    }

    /**
     * 读取指定布局坐标上的当前地形。
     */
    private int terrainAt(BaseStructure<?> data, int r, int c) {
        if (data instanceof BaseMaze<?> maze) {
            return maze.getCell(r, c);
        }
        if (data instanceof GraphMaze graphMaze) {
            MazeCell cell = graphMaze.getMazeCell(r, c);
            return cell == null ? MazeCellType.WALL : cell.getType();
        }
        return MazeCellType.WALL;
    }

    /**
     * 判断布局坐标是否越界。
     */
    private boolean isOverBorder(BaseStructure<?> data, int r, int c) {
        if (data instanceof BaseMaze<?> maze) {
            return maze.isOverBorder(r, c);
        }
        if (data instanceof GraphMaze graphMaze) {
            return graphMaze.isOverBorder(r, c);
        }
        return true;
    }

    private int getRows(BaseStructure<?> data) {
        if (data instanceof BaseMaze<?> maze) {
            return maze.getRows();
        }
        if (data instanceof GraphMaze graphMaze) {
            return graphMaze.getRows();
        }
        return 0;
    }

    private int getCols(BaseStructure<?> data) {
        if (data instanceof BaseMaze<?> maze) {
            return maze.getCols();
        }
        if (data instanceof GraphMaze graphMaze) {
            return graphMaze.getCols();
        }
        return 0;
    }

    private void renderRanCell(int r, int c, double w, double h, int type) {
        double x = getX(c, w);
        double y = getY(r, h);
        double cx = x + w / 2;
        double cy = y + h / 2;
        double monSize = Math.min(w, h) * 0.5;

        switch (type) {
            case MazeCellType.WALL -> {
                gc.setFill(RAN_WALL_STONE);
                gc.fillRect(x, y, w, h);
                drawClanMon(cx, cy, monSize, MazeCellType.WALL, RAN_ENEMY_RUST.deriveColor(0, 1, 1, 0.5));
            }
            case MazeCellType.START -> {
                gc.setFill(RAN_WHITE);
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize * 1.2, MazeCellType.START, RAN_BLACK);
            }
            case MazeCellType.END -> {
                gc.setFill(RAN_VIOLET.saturate());
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize * 1.2, MazeCellType.END, RAN_WHITE);
            }
            case MazeCellType.ROAD -> {
                gc.setFill(RAN_BLUE.deriveColor(0, 1.2, 1.0, 0.45));
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize * 1.2, MazeCellType.ROAD, RAN_BLUE.saturate());
            }
            case MazeCellType.PATH -> {
                gc.setFill(RAN_RED.saturate());
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize * 1.2, MazeCellType.PATH, Color.rgb(40, 0, 0, 0.95));
            }
            case MazeCellType.BACKTRACK, MazeCellType.DEADEND -> {
                gc.setFill(RAN_YELLOW.saturate());
                gc.fillRect(x + 0.5, y + 0.5, w - 1, h - 1);
                drawClanMon(cx, cy, monSize * 1.2, MazeCellType.BACKTRACK, Color.rgb(80, 50, 0, 0.9));
            }
            default -> {
                gc.setFill(RAN_WALL_STONE);
                gc.fillRect(x, y, w, h);
            }
        }
    }

    private void renderArmyQueue(double w, double h) {
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
    }

    private double getX(int col, double cellW) {
        return col * cellW;
    }

    private double getY(int row, double cellH) {
        return row * cellH;
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
