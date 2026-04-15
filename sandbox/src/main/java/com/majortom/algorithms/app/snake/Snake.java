package com.majortom.algorithms.app.snake;

import com.majortom.algorithms.core.basic.LinkedList;

public class Snake {
    public LinkedList<Point> snake = new LinkedList<Point>();
    public Direction direction = Direction.DOWN;

    public Snake() {
        snake.push(new Point(1, 1));
    }

    public void move() {
        Point head = snake.peak();
        if (head != null) {
            Point newHead = new Point(head.x, head.y);
            switch (direction) {
                case UP -> {
                    newHead.y = head.y - 1;
                }
                case DOWN -> {
                    newHead.y = head.y + 1;
                }
                case LEFT -> {
                    newHead.x = head.x - 1;
                }
                case RIGHT -> {
                    newHead.x = head.x + 1;
                }
            }
            snake.push(newHead);
            snake.removeTail();
        }
    }
}
