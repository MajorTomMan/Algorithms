package com.majortom.algorithms.app.snake;

import java.util.Random;

public class Food {
    public int x;
    public int y;
    private Random random = new Random();

    private Food() {

    }

    public Food(Snake snake, Map map) {
        respawn(snake, map);
    }

    public void respawn(Snake snake, Map map) {
        do {
            x = random.nextInt(1, map.width - 1);
            y = random.nextInt(1, map.height - 1);
        } while (snake.contains(new Point(x, y)));
    }
}
