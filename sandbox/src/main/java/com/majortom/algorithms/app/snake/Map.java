package com.majortom.algorithms.app.snake;

public class Map {
    public int width = 20;
    public int height = 20;

    public boolean isOverBorder(Point point) {
        if (point.x <= 0 || point.y <= 0) {
            return true;
        } else if (point.x >= width || point.y >= height) {
            return true;
        }
        return false;
    }
}
