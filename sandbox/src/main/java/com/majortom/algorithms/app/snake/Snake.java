package com.majortom.algorithms.app.snake;

import com.majortom.algorithms.core.basic.LinkedList;
import com.majortom.algorithms.core.basic.node.ListNode;

public class Snake {
    public LinkedList<Point> snake = new LinkedList<Point>();
    public Direction direction = null;
    public boolean isDead = false;
    public boolean isGrow = false;

    public Snake() {
        snake.push(new Point(1, 1));
    }

    public void move() {
        checkSelfCollision();
        if (isDead || direction == null) {
            return;
        }
        Point head = snake.peak();
        if (head != null) {
            Point newHead = new Point(head.x, head.y);
            switch (direction) {
                case UP -> newHead.y--;
                case DOWN -> newHead.y++;
                case LEFT -> newHead.x--;
                case RIGHT -> newHead.x++;
            }
            snake.push(newHead);
            if (!isGrow) {
                snake.removeTail();
            } else {
                isGrow = false;
            }
        }
    }

    private void checkSelfCollision() {
        ListNode<Point> head = snake.getHead();
        ListNode<Point> body = head.next;
        while (body != null) {
            if (head.data.x == body.data.x && head.data.y == body.data.y) {
                isDead = true;
            }
            body = body.next;
        }

    }

    public Point getHead() {
        return snake.getHead().data;
    }

    public int length() {
        return snake.size();
    }

    public void grow(int x, int y) {
        isGrow = true;
    }

    public boolean contains(int x, int y) {
        ListNode<Point> node = snake.getHead();
        while (node != null) {
            if (node.data.x == x && node.data.y == y) {
                return true;
            }
            node = node.next;
        }
        return false;
    }

    public boolean contains(Point point) {
        ListNode<Point> node = snake.getHead();
        while (node != null) {
            if (node.data.x == point.x && node.data.y == point.y) {
                return true;
            }
            node = node.next;
        }
        return false;
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
