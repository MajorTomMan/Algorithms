package com.majortom.algorithms.app.snake;

import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.basic.node.ListNode;

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
        Point head = snake.peek();
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
                snake.remove(snake.size() - 1);
            } else {
                isGrow = false;
            }
        }
    }

    private void checkSelfCollision() {
        ListNode<Point> head = snake.head();
        ListNode<Point> body = head.getNext();
        while (body != null) {
            if (head.getValue().x == body.getValue().x && head.getValue().y == body.getValue().y) {
                isDead = true;
            }
            body = body.getNext();
        }

    }

    public Point getHead() {
        return snake.head().getValue();
    }

    public int length() {
        return snake.size();
    }

    public void grow(int x, int y) {
        isGrow = true;
    }

    public boolean contains(int x, int y) {
        ListNode<Point> node = snake.head();
        while (node != null) {
            if (node.getValue().x == x && node.getValue().y == y) {
                return true;
            }
            node = node.getNext();
        }
        return false;
    }

    public boolean contains(Point point) {
        ListNode<Point> node = snake.head();
        while (node != null) {
            if (node.getValue().x == point.x && node.getValue().y == point.y) {
                return true;
            }
            node = node.getNext();
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
