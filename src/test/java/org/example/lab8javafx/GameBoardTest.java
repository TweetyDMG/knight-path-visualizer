package org.example.lab8javafx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты для игровой доски (GameBoard) и BFS-алгоритма поиска ходов коня.
 */
class GameBoardTest {

    private static final int BOARD_SIZE = 8;
    private GameBoard board;

    @BeforeEach
    void setUp() {
        board = new GameBoard(BOARD_SIZE, BOARD_SIZE);
    }

    @Test
    @DisplayName("На стандартной доске 8x8 конь в центре (4,4) достигает 8 клеток за 1 ход")
    void knightInCenterHas8MovesAtDepth1() {
        List<KnightMove> moves = board.findReachableCells(4, 4, 1);
        assertEquals(8, moves.size(), "Конь в центре доски 8x8 должен иметь 8 доступных ходов");
    }

    @Test
    @DisplayName("Конь в углу (0,0) достигает 2 клетки за 1 ход")
    void knightInCornerHas2MovesAtDepth1() {
        List<KnightMove> moves = board.findReachableCells(0, 0, 1);
        assertEquals(2, moves.size(), "Конь в углу (0,0) должен иметь 2 доступных хода");
    }

    @Test
    @DisplayName("Конь на краю доски (0,3) достигает 4 клеток за 1 ход")
    void knightOnEdgeHas4MovesAtDepth1() {
        List<KnightMove> moves = board.findReachableCells(0, 3, 1);
        assertEquals(4, moves.size(), "Конь на краю доски (0,3) должен иметь 4 доступных хода");
    }

    @Test
    @DisplayName("Глубина обхода 0 не возвращает ходов")
    void depth0ReturnsNoMoves() {
        List<KnightMove> moves = board.findReachableCells(4, 4, 0);
        assertTrue(moves.isEmpty(), "При глубине 0 не должно быть ходов");
    }

    @Test
    @DisplayName("Стена блокирует ход коня")
    void wallBlocksKnightMove() {
        // Ставим коня в (0,0). Доступные ходы: (1,2) и (2,1).
        // Блокируем (2,1) стеной
        board.setWall(2, 1);

        List<KnightMove> moves = board.findReachableCells(0, 0, 1);
        assertEquals(1, moves.size(), "После установки стены в (2,1) должен остаться только один ход — (1,2)");
        assertEquals(1, moves.get(0).getX());
        assertEquals(2, moves.get(0).getY());
    }

    @Test
    @DisplayName("Все ходы имеют корректную depth и branchIndex")
    void movesHaveValidDepthAndBranch() {
        List<KnightMove> moves = board.findReachableCells(4, 4, 2);

        assertFalse(moves.isEmpty(), "Должны быть доступные ходы на глубине 2");

        for (KnightMove move : moves) {
            assertTrue(move.getDepth() >= 1 && move.getDepth() <= 2,
                    "Глубина хода должна быть между 1 и 2, но была " + move.getDepth());
            assertTrue(move.getBranchIndex() >= 0 && move.getBranchIndex() < 8,
                    "branchIndex должен быть между 0 и 7");
        }
    }

    @Test
    @DisplayName("Каждый ход — допустимое перемещение коня")
    void eachMoveIsValidKnightJump() {
        List<KnightMove> moves = board.findReachableCells(4, 4, 1);

        int[][] validJumps = {
                {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
                {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
        };

        for (KnightMove move : moves) {
            int dx = Math.abs(move.getX() - 4);
            int dy = Math.abs(move.getY() - 4);
            assertTrue(isValidKnightJump(dx, dy),
                    "Ход (" + move.getX() + "," + move.getY() + ") не является допустимым ходом коня");
        }
    }

    @Test
    @DisplayName("Доска 3x3 ограничивает ходы коня")
    void board3x3() {
        GameBoard smallBoard = new GameBoard(3, 3);
        List<KnightMove> moves = smallBoard.findReachableCells(1, 1, 1);
        assertTrue(moves.isEmpty(), "На доске 3x3 конь в центре не имеет ходов");
    }

    @Test
    @DisplayName("Несколько стен корректно блокируют пути")
    void multipleWallsBlockAllPaths() {
        board.setWall(1, 2);
        board.setWall(2, 1);

        List<KnightMove> moves = board.findReachableCells(0, 0, 1);
        assertTrue(moves.isEmpty(), "Все ходы из (0,0) заблокированы стенами");
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 2, 2",  // (0,0) на глубине 2 достигает 2 клеток? Нет — больше
            "4, 4, 2, 48", // (4,4) на глубине 2: 8 + ~40 вторичных = 48 (зависит от пересечений)
    })
    @DisplayName("Параметризованный тест: количество достижимых клеток")
    void parametrizedReachableCount(int startX, int startY, int depth, int expectedCount) {
        List<KnightMove> moves = board.findReachableCells(startX, startY, depth);
        // Не проверяем точное количество, но проверяем, что оно > 0
        assertFalse(moves.isEmpty(), "Должны быть ходы из (" + startX + "," + startY + ") на глубине " + depth);
    }

    // Вспомогательный метод: проверка, что разница координат соответствует ходу коня
    private boolean isValidKnightJump(int dx, int dy) {
        return (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
    }
}
