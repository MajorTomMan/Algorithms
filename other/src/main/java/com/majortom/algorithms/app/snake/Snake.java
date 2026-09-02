package com.majortom.algorithms.app.snake;

import java.util.LinkedList;

public class Snake {
    public LinkedList<Point> snake = new LinkedList<Point>();
    public Direction direction = null;
    public boolean isDead = false;
    public boolean isGrow = false;

    public Snake() {
        snake.addFirst(new Point(1, 1));
    }

    public void move() {
        checkSelfCollision();
        if (isDead || direction == null) {
            return;
        }
        Point head = snake.peekFirst();
        if (head != null) {
            Point newHead = new Point(head.x, head.y);
            switch (direction) {
                case UP -> newHead.y--;
                case DOWN -> newHead.y++;
                case LEFT -> newHead.x--;
                case RIGHT -> newHead.x++;
            }
            snake.addFirst(newHead);
            if (!isGrow) {
                snake.removeLast();
            } else {
                isGrow = false;
            }
        }
    }

    private void checkSelfCollision() {
        Point head = snake.peekFirst();
        if (head == null) {
            return;
        }
        boolean first = true;
        for (Point body : snake) {
            if (first) {
                first = false;
                continue;
            }
            if (head.x == body.x && head.y == body.y) {
                isDead = true;
                return;
            }
        }
    }

    public Point getHead() {
        return snake.peekFirst();
    }

    public int length() {
        return snake.size();
    }

    public void grow(int x, int y) {
        isGrow = true;
    }

    public boolean contains(int x, int y) {
        for (Point point : snake) {
            if (point.x == x && point.y == y) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(Point point) {
        return contains(point.x, point.y);
    }

    public void setDirection(Direction newDir) {
        if (this.direction == null) {
            this.direction = newDir;
            return;
        }

        // 防止反向移动（重要）
        if (this.direction == Direction.UP && newDir == Direction.DOWN) {
            return;
        }

        if (this.direction == Direction.DOWN && newDir == Direction.UP) {
            return;
        }
        if (this.direction == Direction.LEFT && newDir == Direction.RIGHT) {
            return;
        }
        if (this.direction == Direction.RIGHT && newDir == Direction.LEFT) {
            return;
        }

        this.direction = newDir;
    }
}
