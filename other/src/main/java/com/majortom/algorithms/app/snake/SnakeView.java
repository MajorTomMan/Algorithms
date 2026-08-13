package com.majortom.algorithms.app.snake;

import java.util.ArrayList;
import java.util.List;

public class SnakeView {
    private static final int PANEL_WIDTH = 47;

    public String render(GameState state, Snake snake, Food food, Map map) {
        StringBuilder sb = new StringBuilder();
        switch (state) {
            case WELCOME -> renderWelcome(sb);
            case PLAYING -> renderPlaying(sb, snake, food, map, false);
            case PAUSED -> renderPlaying(sb, snake, food, map, true);
            case GAME_OVER -> renderGameOver(sb, snake, food, map);
            case EXIT -> {
            }
        }
        return sb.toString();
    }

    private void renderWelcome(StringBuilder sb) {
        appendPanel(
            sb,
            "贪 吃 蛇",
            "一个运行在终端里的小游戏",
            "",
            "操作说明",
            "W A S D 或方向键  -> 每次移动一格",
            "P                -> 游戏中暂停/继续",
            "Q                -> 退出游戏",
            "",
            "按 Enter、空格或 S 开始"
        );
        sb.append("\n");
        appendPanel(
            sb,
            "小提示",
            "这是一步一动的回合制版本。",
            "每次输入只会让蛇前进一步。",
            "规划好路线，别撞墙，也别咬到自己。"
        );
    }

    private void renderPlaying(StringBuilder sb, Snake snake, Food food, Map map, boolean paused) {
        Point head = snake.getHead();
        int score = Math.max(0, snake.length() - 1);

        appendPanel(
            sb,
            paused ? "贪 吃 蛇  [已暂停]" : "贪 吃 蛇",
            "分数   : " + score,
            "长度   : " + snake.length(),
            "蛇头坐标: (" + head.x + ", " + head.y + ")",
            "食物坐标: (" + food.x + ", " + food.y + ")",
            paused ? "按 P 继续，按 R 重开，按 Q 退出" :
                "WASD/方向键移动，P 暂停，R 重开，Q 退出"
        );
        sb.append("\n");
        appendBoard(sb, snake, food, map);
        if (paused) {
            sb.append("\n");
            appendPanel(
                sb,
                "已暂停",
                "当前棋盘已冻结。",
                "按 P 继续，或按 R 重新开始。"
            );
        }
    }

    private void renderGameOver(StringBuilder sb, Snake snake, Food food, Map map) {
        Point head = snake.getHead();
        int score = Math.max(0, snake.length() - 1);

        appendPanel(
            sb,
            "游 戏 结 束",
            "分数     : " + score,
            "长度     : " + snake.length(),
            "最终蛇头坐标: (" + head.x + ", " + head.y + ")",
            "按 R 重新开始，按 Q 退出"
        );
        sb.append("\n");
        appendBoard(sb, snake, food, map);
    }

    private void appendBoard(StringBuilder sb, Snake snake, Food food, Map map) {
        Point head = snake.getHead();
        sb.append("+");
        for (int i = 0; i < map.width - 1; i++) {
            sb.append("-");
        }
        sb.append("+\n");

        for (int y = 1; y < map.height; y++) {
            sb.append("|");
            for (int x = 1; x < map.width; x++) {
                if (head.x == x && head.y == y) {
                    sb.append("O");
                } else if (snake.contains(x, y)) {
                    sb.append("o");
                } else if (food.x == x && food.y == y) {
                    sb.append("*");
                } else {
                    sb.append(" ");
                }
            }
            sb.append("|\n");
        }

        sb.append("+");
        for (int i = 0; i < map.width - 1; i++) {
            sb.append("-");
        }
        sb.append("+\n");
    }

    private void appendPanel(StringBuilder sb, String title, String... lines) {
        List<String> content = new ArrayList<>();
        if (title != null && !title.isEmpty()) {
            content.add(title);
        }
        for (String line : lines) {
            content.add(line == null ? "" : line);
        }

        sb.append(".").append(repeat('-', PANEL_WIDTH - 2)).append(".\n");
        for (String line : content) {
            sb.append("| ")
                .append(padRight(line, PANEL_WIDTH - 4))
                .append(" |\n");
        }
        sb.append("'").append(repeat('-', PANEL_WIDTH - 2)).append("'\n");
    }

    private String repeat(char ch, int count) {
        return String.valueOf(ch).repeat(Math.max(0, count));
    }

    private String padRight(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        return text + " ".repeat(width - text.length());
    }
}
