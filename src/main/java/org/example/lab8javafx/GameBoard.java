package org.example.lab8javafx;

import java.util.*;

public class GameBoard {
    private int rows, cols;
    private boolean[][] walls;
    // visitedDepth хранит минимальную глубину для каждой клетки (-1 если не посещена)
    private int[][] visitedDepth;

    // Список результата генерируется каждый раз в findReachableCells

    // Ходы коня: dx, dy пары
    private static final int[] knightMoves = {-2, -1, -2, 1, -1, -2, -1, 2, 1, -2, 1, 2, 2, -1, 2, 1};

    public GameBoard(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.walls = new boolean[rows][cols];
        this.visitedDepth = new int[rows][cols];
    }

    public void setWall(int x, int y) { if (isValid(x,y)) walls[x][y] = true; }
    public void clearWall(int x, int y) { if (isValid(x,y)) walls[x][y] = false; }
    public boolean isWall(int x, int y) { return isValid(x,y) && walls[x][y]; }

    private boolean isValid(int x, int y) {
        return x >= 0 && x < rows && y >= 0 && y < cols;
    }

    /**
     * Находит достижимые клетки и возвращает список {x, y, depth, branchIndex}.
     * branchIndex - индекс первого хода (0-7), который привел к этой клетке.
     */
    public List<KnightMove> findReachableCells(int startX, int startY, int maxDepth) {
        List<KnightMove> reachableCells = new ArrayList<>();
        boolean[][] visited = new boolean[rows][cols];
        Queue<KnightMove> queue = new LinkedList<>();

        KnightMove startMove = new KnightMove(startX, startY, 0, 0, null);
        queue.add(startMove);
        visited[startX][startY] = true;

        int[] dx = {-2, -1, 1, 2, 2, 1, -1, -2};
        int[] dy = {1, 2, 2, 1, -1, -2, -2, -1};

        while (!queue.isEmpty()) {
            KnightMove current = queue.poll();
            if (current.getDepth() >= maxDepth) continue;

            for (int i = 0; i < 8; i++) {
                int newX = current.getX() + dx[i];
                int newY = current.getY() + dy[i];

                if (isValid(newX, newY) && !walls[newX][newY] && !visited[newX][newY]) {
                    KnightMove newMove = new KnightMove(newX, newY, current.getDepth() + 1, i, current);
                    queue.add(newMove);
                    visited[newX][newY] = true;
                    reachableCells.add(newMove);
                }
            }
        }
        return reachableCells;
    }

}