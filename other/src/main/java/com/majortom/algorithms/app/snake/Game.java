package com.majortom.algorithms.app.snake;

import java.io.IOException;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

public class Game {
    private static final String CLEAR_SCREEN = "\033[H\033[2J";

    private final Map map = new Map();
    private final SnakeView view = new SnakeView();
    private Snake snake;
    private Food food;
    private GameState state = GameState.WELCOME;
    private Terminal terminal;
    private Attributes originalAttributes;

    public Game() {
        resetGame();
    }

    private void resetGame() {
        snake = new Snake();
        food = new Food(snake, map);
        state = GameState.WELCOME;
    }

    private void startGame() {
        snake = new Snake();
        food = new Food(snake, map);
        state = GameState.PLAYING;
        render();
    }

    private void tick() {
        snake.move();
        checkEating();
        checkGameIsOver();
        render();
    }

    private void checkEating() {
        Point head = snake.getHead();
        if (head.x == food.x && head.y == food.y) {
            snake.grow(food.x, food.y);
            food.respawn(snake, map);
        }
    }

    private void checkGameIsOver() {
        Point head = snake.getHead();
        if (snake.isDead || map.isOverBorder(head)) {
            state = GameState.GAME_OVER;
        }
    }

    private void clearScreen() {
        terminal.writer().print(CLEAR_SCREEN);
        terminal.writer().flush();
    }

    private void render() {
        clearScreen();
        terminal.writer().print(view.render(state, snake, food, map));
        terminal.writer().flush();
    }

    private void initTerminal() throws IOException {
        terminal = TerminalBuilder.builder()
            .system(true)
            .build();
        originalAttributes = terminal.enterRawMode();
    }

    private void restoreTerminal() {
        if (terminal == null) {
            return;
        }
        if (originalAttributes != null) {
            terminal.setAttributes(originalAttributes);
        }
        terminal.writer().print("\n");
        terminal.writer().flush();
    }

    private void handleInput() throws IOException {
        NonBlockingReader reader = terminal.reader();
        int ch = reader.read();
        if (ch < 0) {
            state = GameState.EXIT;
            return;
        }

        if (ch == 27) {
            handleArrowKey(reader);
            return;
        }

        handleCharInput(Character.toLowerCase((char) ch));
    }

    private void handleArrowKey(NonBlockingReader reader) throws IOException {
        int next = reader.read(5);
        if (next != '[') {
            return;
        }

        int direction = reader.read(5);
        Direction move = switch (direction) {
            case 'A' -> Direction.UP;
            case 'B' -> Direction.DOWN;
            case 'C' -> Direction.RIGHT;
            case 'D' -> Direction.LEFT;
            default -> null;
        };

        if (move != null) {
            handleMove(move);
        }
    }

    private void handleCharInput(char ch) {
        switch (ch) {
            case 'w' -> handleMove(Direction.UP);
            case 's' -> {
                if (state == GameState.WELCOME) {
                    startGame();
                } else {
                    handleMove(Direction.DOWN);
                }
            }
            case 'a' -> handleMove(Direction.LEFT);
            case 'd' -> handleMove(Direction.RIGHT);
            case 'p' -> togglePause();
            case 'r' -> restartFromCurrentState();
            case 'q' -> state = GameState.EXIT;
            case '\r', '\n', ' ' -> {
                if (state == GameState.WELCOME) {
                    startGame();
                }
            }
            default -> {
            }
        }
    }

    private void handleMove(Direction direction) {
        if (state == GameState.WELCOME) {
            startGame();
        }
        if (state != GameState.PLAYING) {
            return;
        }
        snake.setDirection(direction);
        tick();
    }

    private void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
            render();
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            render();
        }
    }

    private void restartFromCurrentState() {
        if (state == GameState.PLAYING || state == GameState.PAUSED || state == GameState.GAME_OVER) {
            startGame();
        }
    }

    private void run() {
        try {
            initTerminal();
            render();
            while (state != GameState.EXIT) {
                handleInput();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize terminal", e);
        } finally {
            restoreTerminal();
        }
    }

    public static void main(String[] args) {
        Game game = new Game();
        game.run();
    }
}
