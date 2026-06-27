package org.example.lab8javafx;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.*;

import javafx.scene.layout.VBox;

import java.io.IOException;

public class KnightController {
    @FXML private VBox mainRootPane;
    @FXML private AnchorPane boardContainer;
    @FXML private Button calculateButton;
    @FXML private Button showPathButton;
    @FXML private Button resizeButton;
    @FXML private Button openMovesWindowButton;
    @FXML private Spinner<Integer> maxMovesSpinner;
    @FXML private Spinner<Integer> rowsSpinner;
    @FXML private Spinner<Integer> colsSpinner;
    @FXML private Button clearVisitedButton;

    private Pane pieceLayer;
    private GameBoard board;
    private Button[][] boardButtons;
    private int knightX = -1, knightY = -1;
    private ImageView knightView;
    private GridPane boardPane;
    private int rows = 8, cols = 8;
    private Animation currentAnimation;
    private MovesWindowController movesWindowControllerInstance = null;
    private Set<String> visitedCells = new HashSet<>();
    private static final String VISITED_CELL_STYLE_CLASS = "visited-cell";
    private Stage primaryStage; // Ссылка на главное окно
    private double[] mainDragDelta;
    private Set<String> visitedCellsSet = new HashSet<>();
    private Map<String, String> visitedCellStyles = new HashMap<>();
    private static final String DIRECT_VISIT_STYLE = "-fx-background-color: rgba(180, 180, 180, 0.6);";
    private double xOffset = 0;
    private double yOffset = 0;

    private final Color[] branchBaseColors = {
            Color.rgb(255, 100, 100), // Red-ish
            Color.rgb(255, 180, 100), // Orange-ish
            Color.rgb(255, 255, 100), // Yellow-ish
            Color.rgb(150, 255, 100), // Lime-ish
            Color.rgb(100, 255, 200), // Cyan-ish
            Color.rgb(100, 200, 255), // Blue-ish
            Color.rgb(180, 100, 255), // Purple-ish
            Color.rgb(255, 100, 200)  // Pink-ish
    };

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        primaryStage.getScene().setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        primaryStage.getScene().setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        });
    }

    @FXML
    public void initialize() {
        Image knightImage = new Image(getClass().getResourceAsStream("/org/example/lab8javafx/knight.png"));
        knightView = new ImageView(knightImage);
        knightView.setFitWidth(32);
        knightView.setFitHeight(32);

        board = new GameBoard(rows, cols);
        boardPane = createBoardPane(rows, cols);
        boardContainer.getChildren().add(boardPane);

        pieceLayer = new Pane();
        pieceLayer.setPickOnBounds(false);
        boardContainer.getChildren().add(pieceLayer);

        pieceLayer.getChildren().add(knightView);
        knightView.setVisible(false);
        adjustBoardPaneSizeAndPosition(); // Масштабируем и центрируем изначально

        maxMovesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 2));
        rowsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 20, 8));
        colsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 20, 8));

        calculateButton.setOnAction(e -> showReachableCells());
        showPathButton.setOnAction(e -> showKnightPathWithTimeline());
        resizeButton.setOnAction(e -> resizeBoard());
        openMovesWindowButton.setOnAction(e -> openMovesWindow());
        clearVisitedButton.setOnAction(e -> clearBoardAndVisited());

        // При инициализации сразу настроить видимость кнопки очистки
        updateClearButtonState();
    }


    // Пример метода для добавления кнопок управления (если не добавлены в FXML)
    private void makeWindowMovable() {
        primaryStage.getScene().setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        primaryStage.getScene().setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        });
    }

    private void markCellAsVisited(int x, int y) {
        String coord = x + "," + y;
        if (visitedCells.add(coord)) { // Добавляем, только если еще не посещена
            if (boardButtons[x][y] != null) {
                // Добавляем CSS класс
                boardButtons[x][y].getStyleClass().add(VISITED_CELL_STYLE_CLASS);
            }
        }
        updateClearButtonState(); // Обновить состояние кнопки
    }

    private void clearBoardAndVisited() {
        // Остановить анимацию
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
        }
        knightX = -1; knightY = -1;
        if (knightView != null) {
            knightView.setVisible(false);
        }

        // Очистка коллекций для посещенных клеток
        visitedCellsSet.clear();
        visitedCellStyles.clear();

        // Восстановление базового вида всех клеток
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (boardButtons == null || i >= boardButtons.length || boardButtons[i] == null || j >= boardButtons[i].length || boardButtons[i][j] == null) continue;
                Button button = boardButtons[i][j];
                button.setStyle(null); // Убрать все инлайн стили
                // Восстановить базовые классы
                if (!board.isWall(i, j)) {
                    boolean isLight = (i + j) % 2 == 0;
                    String baseClass = isLight ? "light-cell" : "dark-cell";
                    String otherClass = isLight ? "dark-cell" : "light-cell";
                    button.getStyleClass().clear(); // Очистить ВСЕ классы
                    button.getStyleClass().add("button"); // Добавить стандартный класс кнопки javafx
                    button.getStyleClass().add(baseClass); // Добавить базовый цвет
                } else {
                    button.getStyleClass().clear();
                    button.getStyleClass().add("button");
                    button.getStyleClass().add("wall-cell"); // Добавить стиль стены
                }
            }
        }

        // Очистка подсветки в окне ходов
        if (movesWindowControllerInstance != null) {
            movesWindowControllerInstance.clearHighlight();
        }

        updateClearButtonState(); // Обновить кнопку
        System.out.println("Поле очищено.");
    }

    // Метод для обновления состояния кнопки "Очистить"
    private void updateClearButtonState() {
        if (clearVisitedButton != null) {
            clearVisitedButton.setDisable(visitedCellsSet.isEmpty() && knightX == -1);
        }
    }

    private void openMovesWindow() {
        // Проверяем, не открыто ли уже окно
        if (movesWindowControllerInstance != null && movesWindowControllerInstance.getStage() != null && movesWindowControllerInstance.getStage().isShowing()) {
            movesWindowControllerInstance.getStage().toFront(); // Просто вывести вперед, если уже открыто
            return;
        }
        if (knightX == -1 || knightY == -1) {
            System.out.println("Поставьте коня на поле перед открытием окна ходов!");
            // Опционально: показать Alert
            return;
        }


        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/lab8javafx/MovesWindow.fxml"));
            // Создаем контроллер ПЕРЕД загрузкой FXML, если он не создается через FXML
            MovesWindowController controller = new MovesWindowController(this);
            loader.setController(controller); // Устанавливаем контроллер
            Parent root = loader.load();

            Stage stage = new Stage();
            // --- НАСТРОЙКА ОКНА БЕЗ РАМКИ ---
            stage.initStyle(StageStyle.TRANSPARENT); // <--- Убираем стандартную рамку

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            // Подключаем CSS (уже было, проверяем путь)
            String cssPath = getClass().getResource("/org/example/lab8javafx/style.css").toExternalForm();
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath);
            } else {
                System.err.println("Не удалось найти style.css!");
            }

            stage.setScene(scene);
            stage.setTitle("Ходы коня"); // Заголовок не будет виден, но полезен для ОС
            controller.setStage(stage); // Передаем Stage в контроллер

            // Сохраняем ссылку на контроллер
            this.movesWindowControllerInstance = controller;

            // Устанавливаем обработчик закрытия, чтобы сбросить ссылку
            stage.setOnCloseRequest(event -> this.movesWindowControllerInstance = null);
            // Также можно использовать setOnHidden
            stage.setOnHidden(event -> this.movesWindowControllerInstance = null);


            int maxMoves = maxMovesSpinner.getValue();
            List<KnightMove> moves = board.findReachableCells(knightX, knightY, maxMoves);
            controller.setMoves(moves); // Передаем ходы

            stage.show();
        } catch (IOException e) {
            System.err.println("Ошибка загрузки MovesWindow.fxml:");
            e.printStackTrace();
        } catch (NullPointerException npe) {
            System.err.println("Ошибка: Возможно, не найден FXML или CSS файл.");
            npe.printStackTrace();
        }
    }

    // Метод для передачи цветов в MovesWindowController (если потребуется)
    public Color[] getBranchBaseColors() {
        return branchBaseColors;
    }

    // Метод для конвертации цвета (если потребуется в MovesWindowController)
    public String toWebColorPublic(Color color) {
        return toWebColor(color); // Вызов приватного метода
    }

    private String toWebColor(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void placeKnight(int x, int y) {
        // Проверки входных данных
        if (x < 0 || x >= rows || y < 0 || y >= cols || boardButtons == null || boardButtons[x] == null || boardButtons[x][y] == null) {
            System.err.println("Ошибка: Неверные координаты или доска не инициализирована в placeKnight.");
            return;
        }

        // Получаем размер ячейки
        double cellSize = boardButtons[x][y].getPrefWidth();
        if (cellSize <= 0) {
            cellSize = (pieceLayer.getPrefWidth() > 0 && cols > 0) ? pieceLayer.getPrefWidth() / cols : 60.0;
            System.err.println("Предупреждение: Размер ячейки <= 0 в placeKnight, используется " + cellSize);
        }
        // Получаем размер коня
        double knightWidth = knightView.getFitWidth();
        double knightHeight = knightView.getFitHeight();
        if (knightWidth <= 0 || knightHeight <= 0){
            System.err.println("Предупреждение: Размер коня <= 0 в placeKnight.");
            // Можно установить размер по умолчанию или на основе cellSize
            knightWidth = cellSize * 0.8;
            knightHeight = cellSize * 0.8;
            knightView.setFitWidth(knightWidth);
            knightView.setFitHeight(knightHeight);
        }


        // *** УПРОЩЕННЫЙ РАСЧЕТ КООРДИНАТ ***
        // Рассчитываем центр целевой ячейки (x, y) в координатах pieceLayer
        double cellCenterXInParent = (y + 0.5) * cellSize;
        double cellCenterYInParent = (x + 0.5) * cellSize;

        // Рассчитываем layoutX/Y (верхний левый угол) для центрирования коня
        double targetLayoutX = cellCenterXInParent - knightWidth / 2;
        double targetLayoutY = cellCenterYInParent - knightHeight / 2;

        // Устанавливаем позицию
        knightView.setLayoutX(targetLayoutX);
        knightView.setLayoutY(targetLayoutY);

        // Сбрасываем любые предыдущие смещения от анимаций
        knightView.setTranslateX(0);
        knightView.setTranslateY(0);

        knightView.setVisible(true);

        // Обновляем логические координаты коня
        knightX = x;
        knightY = y;

        updateClearButtonState();
        // System.out.println("placeKnight (Simplified): Placed at [" + x + "," + y + "], Layout=(" + targetLayoutX + ", " + targetLayoutY + ")"); // Оставим для отладки если нужно
    }

    private void showKnightPathWithTimeline() {
        if (knightX == -1 || knightY == -1) {
            System.out.println("Поставьте коня на поле!");
            return;
        }

        int maxMoves = maxMovesSpinner.getValue();
        // Изменяем тип на List<KnightMove>
        List<KnightMove> reachable = board.findReachableCells(knightX, knightY, maxMoves);
        if (reachable.isEmpty()) {
            System.out.println("Нет достижимых клеток!");
            return;
        }

        // Останавливаем предыдущую анимацию, если она есть
        if (currentAnimation != null) {
            currentAnimation.stop();
            placeKnight(knightX, knightY);
        }

        Timeline timeline = new Timeline();
        double timePerSegment = 0.4; // Секунд на один "прыжок" коня

        Point2D startCenter = getButtonCenterInParent(knightX, knightY, pieceLayer);
        if (startCenter == null) {
            System.err.println("Timeline: Не удалось получить стартовые координаты.");
            return;
        }
        double startLayoutX = startCenter.getX() - knightView.getFitWidth() / 2;
        double startLayoutY = startCenter.getY() - knightView.getFitHeight() / 2;

        knightView.setLayoutX(startLayoutX);
        knightView.setLayoutY(startLayoutY);
        knightView.setTranslateX(0);
        knightView.setTranslateY(0);

        timeline.getKeyFrames().add(new KeyFrame(Duration.ZERO,
                new KeyValue(knightView.layoutXProperty(), knightView.getLayoutX()),
                new KeyValue(knightView.layoutYProperty(), knightView.getLayoutY())
        ));

        double currentTime = 0.0;
        for (KnightMove pos : reachable) { // Используем KnightMove вместо int[]
            Point2D targetCenter = getButtonCenterInParent(pos.getX(), pos.getY(), pieceLayer);
            if (targetCenter == null) {
                System.err.println("Timeline: Пропуск точки (" + pos.getX() + "," + pos.getY() + ") - не удалось получить координаты.");
                continue;
            }

            double targetLayoutX = targetCenter.getX() - knightView.getFitWidth() / 2;
            double targetLayoutY = targetCenter.getY() - knightView.getFitHeight() / 2;

            currentTime += timePerSegment;
            timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(currentTime),
                    new KeyValue(knightView.layoutXProperty(), targetLayoutX, Interpolator.EASE_BOTH),
                    new KeyValue(knightView.layoutYProperty(), targetLayoutY, Interpolator.EASE_BOTH)
            ));
        }

        currentTime += timePerSegment;
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(currentTime),
                new KeyValue(knightView.layoutXProperty(), startLayoutX, Interpolator.EASE_BOTH),
                new KeyValue(knightView.layoutYProperty(), startLayoutY, Interpolator.EASE_BOTH)
        ));

        timeline.setOnFinished(e -> {
            knightView.setLayoutX(startLayoutX);
            knightView.setLayoutY(startLayoutY);
            knightView.setTranslateX(0);
            knightView.setTranslateY(0);
            currentAnimation = null;
            System.out.println("Timeline анимация пути завершена.");
        });

        currentAnimation = timeline;
        timeline.play();
    }

    private void adjustBoardPaneSizeAndPosition() {
        // Размеры контейнера
        double containerWidth = boardContainer.getPrefWidth();  // 600.0
        double containerHeight = boardContainer.getPrefHeight(); // 480.0

        // Вычисляем размер ячейки исходя из доступного пространства и размеров доски
        double cellWidth = containerWidth / cols;
        double cellHeight = containerHeight / rows;
        // Используем минимальный размер для сохранения пропорций клеток
        double cellSize = Math.min(cellWidth, cellHeight);

        // Устанавливаем предпочтительный размер каждой кнопки (ячейки)
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // Используем setPrefSize, т.к. GridPane будет управлять итоговым размером
                if (boardButtons[i] != null && boardButtons[i][j] != null) {
                    boardButtons[i][j].setPrefSize(cellSize, cellSize);
                }
            }
        }

        // Вычисляем реальные размеры доски после установки размеров ячеек
        double boardWidth = cols * cellSize;
        double boardHeight = rows * cellSize;

        // Устанавливаем РАЗМЕРЫ для boardPane, чтобы AnchorPane мог его центрировать
        // GridPane сам изменит свой размер под содержимое, но для AnchorPane лучше задать явно
        boardPane.setPrefSize(boardWidth, boardHeight);
        boardPane.setMaxSize(boardWidth, boardHeight); // Ограничиваем максимальный размер

        // Масштабирование не нужно, если мы управляем размером ячеек
        boardPane.setScaleX(1.0);
        boardPane.setScaleY(1.0);

        // Центрируем boardPane в AnchorPane
        double leftAnchor = (containerWidth - boardWidth) / 2;
        double topAnchor = (containerHeight - boardHeight) / 2;
        AnchorPane.setLeftAnchor(boardPane, leftAnchor);
        AnchorPane.setTopAnchor(boardPane, topAnchor);
        // Очистим правый/нижний якоря, если они были установлены ранее
        AnchorPane.setRightAnchor(boardPane, null);
        AnchorPane.setBottomAnchor(boardPane, null);


        // *** ДОБАВЛЕНО: Позиционируем pieceLayer точно так же, как boardPane ***
        AnchorPane.setLeftAnchor(pieceLayer, leftAnchor);
        AnchorPane.setTopAnchor(pieceLayer, topAnchor);
        // Опционально: задать размер pieceLayer равным доске. Это может помочь,
        // если у pieceLayer есть фон или эффекты, но для позиционирования детей не обязательно.
        pieceLayer.setPrefSize(boardWidth, boardHeight);
        // Очистим правый/нижний якоря и для pieceLayer
        AnchorPane.setRightAnchor(pieceLayer, null);
        AnchorPane.setBottomAnchor(pieceLayer, null);


        // Корректируем размер knightView под новый размер ячейки
        // Делаем коня немного меньше ячейки
        double knightSize = cellSize * 0.8; // 80% от размера ячейки
        knightView.setFitWidth(knightSize);
        knightView.setFitHeight(knightSize);

        // Если конь уже стоит на доске, обновим его позицию после изменения размера
        if (knightX != -1 && knightY != -1) {
            placeKnight(knightX, knightY);
        }
    }

    private GridPane createBoardPane(int rows, int cols) {
        GridPane newBoardPane = new GridPane();
        boardButtons = new Button[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Button cell = new Button();
                cell.setPrefSize(60, 60);
                // Применяем CSS-класс в зависимости от цвета клетки
                cell.getStyleClass().add((i + j) % 2 == 0 ? "light-cell" : "dark-cell");
                boardButtons[i][j] = cell;
                int finalI = i, finalJ = j;
                cell.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        moveKnight(finalI, finalJ);
                    } else if (event.getButton() == MouseButton.SECONDARY) {
                        toggleWall(finalI, finalJ);
                    }
                });
                newBoardPane.add(cell, j, i);
            }
        }
        return newBoardPane;
    }

    private Point2D getButtonCenterInParent(int r, int c, Pane parentPane) { // parentPane это pieceLayer
        if (r < 0 || r >= rows || c < 0 || c >= cols || boardButtons == null || boardButtons[r] == null) return null;

        Button button = boardButtons[r][c];
        if (button == null) return null;

        // Получаем размер ячейки (предполагаем, что он одинаковый для всех и установлен)
        double cellSize = button.getPrefWidth();
        // Можно взять и из boardButtons[0][0].getPrefWidth(), если уверены, что он существует
        if (cellSize <= 0) {
            // Попытка получить из другого места или значение по умолчанию
            cellSize = (pieceLayer.getPrefWidth() > 0 && cols > 0) ? pieceLayer.getPrefWidth() / cols : 60.0;
            System.err.println("Предупреждение: Размер ячейки <= 0 в getButtonCenterInParent, используется " + cellSize);
        }


        // *** УПРОЩЕННЫЙ РАСЧЕТ ***
        // Рассчитываем координаты центра ячейки (r, c) напрямую
        // относительно начала координат parentPane (pieceLayer), которое
        // теперь должно совпадать с началом boardPane.
        // Индексы GridPane: (columnIndex, rowIndex) -> (c, r)
        double cellCenterXInParent = (c + 0.5) * cellSize;
        double cellCenterYInParent = (r + 0.5) * cellSize;

        return new Point2D(cellCenterXInParent, cellCenterYInParent);
    }

    private void clearReachableCells() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (boardButtons == null || i >= boardButtons.length || boardButtons[i] == null || j >= boardButtons[i].length || boardButtons[i][j] == null) {
                    continue;
                }
                Button button = boardButtons[i][j];
                button.setStyle(null); // Убираем инлайн стили (от подсветки пути/достижимых)
                button.getStyleClass().remove("path-cell"); // Убираем старый класс пути (если использовался)

                // Не трогаем класс visited-cell

                // Восстанавливаем базовый стиль, если это не стена
                if (!board.isWall(i, j)) {
                    boolean isLight = (i + j) % 2 == 0;
                    String baseClass = isLight ? "light-cell" : "dark-cell";
                    String otherClass = isLight ? "dark-cell" : "light-cell";
                    // Убираем только противоположный базовый класс
                    button.getStyleClass().remove(otherClass);
                    // Добавляем нужный базовый класс, если его нет И если нет класса стены
                    if (!button.getStyleClass().contains(baseClass) && !button.getStyleClass().contains("wall-cell")) {
                        button.getStyleClass().add(baseClass);
                    }
                } else { // Если это стена
                    // Убедимся, что нет базовых классов и есть класс стены
                    button.getStyleClass().removeAll("light-cell", "dark-cell");
                    if (!button.getStyleClass().contains("wall-cell")) {
                        button.getStyleClass().add("wall-cell");
                    }
                }
            }
        }
        // Переприменяем стили посещенных клеток поверх базовых
        reapplyVisitedStyles();
        // Очищаем подсветку в списке
        if (movesWindowControllerInstance != null) {
            movesWindowControllerInstance.clearHighlight();
        }
    }


    private void resizeBoard() {
        int newRows = rowsSpinner.getValue();
        int newCols = colsSpinner.getValue();
        if (newRows == rows && newCols == cols) return;

        GaussianBlur blur = new GaussianBlur(0);
        boardPane.setEffect(blur);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), boardPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            blur.setRadius(10);
            board = new GameBoard(newRows, newCols);
            GridPane newBoardPane = createBoardPane(newRows, newCols);
            boardContainer.getChildren().set(0, newBoardPane);

            boardPane = newBoardPane;
            rows = newRows;
            cols = newCols;
            knightX = -1;
            knightY = -1;

            clearBoardAndVisited();

            adjustBoardPaneSizeAndPosition(); // Масштабируем и центрируем

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), boardPane);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void moveKnight(int newX, int newY) {
        // ... (проверки границ, стен, той же ячейки) ...
        if (newX >= rows || newY >= cols || newX < 0 || newY < 0 || board.isWall(newX, newY)) return;

        int oldX = knightX;
        int oldY = knightY;

        if (knightX == -1) { // Первая постановка
            placeKnight(newX, newY);
            clearTemporaryHighlights(); // Очищаем только временные
            updateClearButtonState();
            return;
        }
        if (knightX == newX && knightY == newY) return;


        // --- Конечная позиция для анимации ---
        // Получаем центр целевой ячейки (используя УПРОЩЕННЫЙ метод)
        Point2D targetCenterInPieceLayer = getButtonCenterInParent(newX, newY, pieceLayer);
        if (targetCenterInPieceLayer == null) {
            System.err.println("MoveKnight: Не удалось получить координаты цели.");
            return;
        }

        // Конечные layoutX/Y (верхний левый угол коня)
        double finalLayoutX = targetCenterInPieceLayer.getX() - knightView.getFitWidth() / 2;
        double finalLayoutY = targetCenterInPieceLayer.getY() - knightView.getFitHeight() / 2;

        // --- Анимация (TranslateTransition остается прежней) ---
        double startTranslateX = knightView.getTranslateX();
        double startTranslateY = knightView.getTranslateY();
        // Целевое смещение = конечный layout - текущий layout
        double targetTranslateX = finalLayoutX - knightView.getLayoutX();
        double targetTranslateY = finalLayoutY - knightView.getLayoutY();

        TranslateTransition transition = new TranslateTransition(Duration.millis(300), knightView);
        transition.setFromX(startTranslateX);
        transition.setFromY(startTranslateY);
        transition.setToX(targetTranslateX);
        transition.setToY(targetTranslateY);

        transition.setOnFinished(e -> {
            // ... (обновление layout, translate, логических координат knightX/Y) ...
            knightX = newX; // Обновляем логику
            knightY = newY;
            // ОТМЕЧАЕМ НОВУЮ КЛЕТКУ КАК ПОСЕЩЕННУЮ ПОСЛЕ ПЕРЕМЕЩЕНИЯ
            markCellAsVisitedFirstTime(newX, newY, DIRECT_VISIT_STYLE);

//            clearReachableCells(); // Очищаем временную подсветку
//            // Очищаем подсветку в списке ходов
//            if (movesWindowControllerInstance != null) {
//                movesWindowControllerInstance.clearHighlight();
//            }
            clearTemporaryHighlights();
            updateClearButtonState(); // Обновляем кнопку очистки
            System.out.println("MoveKnight finished: Moved to [" + knightX + "," + knightY + "]");
        });

        // Останавливаем анимацию пути, если она идет
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
            // На всякий случай сбросим translate перед новой анимацией
            knightView.setTranslateX(0);
            knightView.setTranslateY(0);
        }

        transition.play();
    }


    private void toggleWall(int x, int y) {
        if (x >= rows || y >= cols || (x == knightX && y == knightY)) return;
        String coord = x + "," + y;
        Button button = boardButtons[x][y];
        if (!board.isWall(x, y)) {
            board.setWall(x, y);
            button.setGraphic(null);
            button.getStyleClass().removeAll("light-cell", "dark-cell");
            visitedCellsSet.remove(coord);
            visitedCellStyles.remove(coord);
            if (!button.getStyleClass().contains("wall-cell")) {
                button.getStyleClass().add("wall-cell");
            }
            FillTransition fadeToBlack = new FillTransition(Duration.millis(300), button.getShape());
            fadeToBlack.setFromValue((x + y) % 2 == 0 ? Color.valueOf("#e0e0e0") : Color.valueOf("#808080"));
            fadeToBlack.setToValue(Color.BLACK);
            fadeToBlack.play();
            button.getStyleClass().remove(VISITED_CELL_STYLE_CLASS);
            visitedCells.remove(x + "," + y);
        } else {
            board.clearWall(x, y);
            button.getStyleClass().remove("wall-cell");
            boolean isLight = (x + y) % 2 == 0;
            button.getStyleClass().add(isLight ? "light-cell" : "dark-cell");
            FillTransition fadeToBase = new FillTransition(Duration.millis(300), button.getShape());
            fadeToBase.setFromValue(Color.BLACK);
            fadeToBase.setToValue((x + y) % 2 == 0 ? Color.valueOf("#e0e0e0") : Color.valueOf("#808080"));
            fadeToBase.play();
        }
        clearReachableCells();
        updateClearButtonState();
    }

    private void showReachableCells() {
        if (knightX == -1 || knightY == -1) {
            System.out.println("Поставьте коня на поле!");
            return;
        }
        clearTemporaryHighlights();

        int maxMoves = maxMovesSpinner.getValue();
        if (maxMoves <= 0) {
            System.out.println("Глубина должна быть > 0 для отображения ходов.");
            return;
        }

        List<KnightMove> reachable = board.findReachableCells(knightX, knightY, maxMoves);

        for (KnightMove move : reachable) {
            int x = move.getX();
            int y = move.getY();
            String coord = x + "," + y;

            // Применяем стиль достижимой клетки ТОЛЬКО ЕСЛИ
            // клетка не посещена постоянно (нет в карте стилей)
            // И если это не стена
            if (!visitedCellStyles.containsKey(coord) && !board.isWall(x,y)) {
                int depth = move.getDepth();
                int branchIndex = move.getBranchIndex();
                if (depth == 0 || branchIndex < 0 || branchIndex >= branchBaseColors.length) continue;

                // Расчет цвета (без изменений)
                Color baseColor = branchBaseColors[branchIndex];
                double darknessFactor = Math.min(0.8, (double)(depth - 1) / maxMoves);
                Color finalColor = baseColor.interpolate(Color.BLACK, darknessFactor);

                if (x >= 0 && x < rows && y >= 0 && y < cols && boardButtons[x][y] != null) {
                    Button button = boardButtons[x][y];
                    // Убираем базовые классы перед инлайн стилем
                    button.getStyleClass().removeAll("light-cell", "dark-cell");
                    // Применяем временный стиль
                    button.setStyle("-fx-background-color: " + toWebColor(finalColor) + ";");
                }
            }
        }
        // В конце еще раз переприменяем постоянные стили, чтобы они были поверх временных
        reapplyPersistentStyles();
    }

    public void highlightAndAnimatePath(KnightMove move) {
        // Шаг 1: Сбор пути (остается без изменений)
        List<KnightMove> path = new ArrayList<>();
        KnightMove current = move;
        while (current != null) {
            path.add(0, current); // Добавляем в начало списка для правильного порядка
            current = current.getParent();
        }

        // Шаг 2: Подсветка пути с использованием логики цветов ветвей/глубины
        clearTemporaryHighlights(); // Убираем старую подсветку
        int maxDepthInPath = move.getDepth(); // Глубина конечной точки - максимальная в этом пути

        for (KnightMove step : path) {
            int x = step.getX(); int y = step.getY();
            String coord = x + "," + y;
            // Подсвечиваем только если клетка не посещена постоянно И не стена
            if (!visitedCellStyles.containsKey(coord) && !board.isWall(x,y)) {
                int depth = step.getDepth(); int branchIndex = step.getBranchIndex();
                if (depth == 0 || branchIndex < 0 || branchIndex >= branchBaseColors.length) continue;
                // Расчет цвета
                Color baseColor = branchBaseColors[branchIndex];
                double darknessFactor = (maxDepthInPath > 0) ? Math.min(0.8, (double)(depth - 1) / maxDepthInPath) : 0.0;
                Color finalColor = baseColor.interpolate(Color.BLACK, darknessFactor);
                // Применяем стиль
                if (x >= 0 && x < rows && y >= 0 && y < cols && boardButtons[x][y] != null) {
                    Button button = boardButtons[x][y];
                    button.getStyleClass().removeAll("light-cell", "dark-cell"); // Убрать базовые
                    button.setStyle("-fx-background-color: " + toWebColor(finalColor) + ";");
                }
            }
        }
        reapplyPersistentStyles();
        if (movesWindowControllerInstance != null) {
            movesWindowControllerInstance.setHighlightedPath(path, maxDepthInPath); // Передаем путь и макс. глубину
        }
        animateKnightAlongPath(path);
    }

    private void animateKnightAlongPath(List<KnightMove> path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        // Останавливаем предыдущую анимацию, если она есть
        if (currentAnimation != null) {
            currentAnimation.stop();
            // После остановки лучше вернуть коня в начальное положение пути
            KnightMove startMove = path.get(0);
            placeKnight(startMove.getX(), startMove.getY());
        }

        Timeline timeline = new Timeline();
        double timePerSegment = 0.4; // Секунд на один "прыжок" коня

        // Устанавливаем начальную позицию (без анимации)
        KnightMove startMove = path.get(0);
        placeKnight(startMove.getX(), startMove.getY()); // Ставим коня в начало

        // Добавляем KeyFrame для начальной позиции (важно для плавной интерполяции с первого шага)
        Point2D startCenter = getButtonCenterInParent(startMove.getX(), startMove.getY(), pieceLayer);
        if (startCenter == null) {
            System.err.println("AnimatePath: Не удалось получить стартовые координаты.");
            return; // Не можем начать анимацию без стартовой точки
        }
        double startLayoutX = startCenter.getX() - knightView.getFitWidth() / 2;
        double startLayoutY = startCenter.getY() - knightView.getFitHeight() / 2;

        // Убедимся, что конь точно в начальной точке перед анимацией
        knightView.setLayoutX(startLayoutX);
        knightView.setLayoutY(startLayoutY);
        knightView.setTranslateX(0);
        knightView.setTranslateY(0);

        timeline.getKeyFrames().add(new KeyFrame(Duration.ZERO,
                new KeyValue(knightView.layoutXProperty(), startLayoutX),
                new KeyValue(knightView.layoutYProperty(), startLayoutY)
        ));


        double currentTime = 0.0;
        List<KnightMove> pathCopy = new ArrayList<>(path);
        // Начинаем итерацию со второго элемента, так как первый - это старт
        for (int i = 1; i < path.size(); i++) {
            // ... (расчет координат, добавление KeyFrame с KeyValues) ...
            KnightMove pos = path.get(i);
            Point2D targetCenter = getButtonCenterInParent(pos.getX(), pos.getY(), pieceLayer);
            if (targetCenter == null) continue;
            double targetLayoutX = targetCenter.getX() - knightView.getFitWidth() / 2;
            double targetLayoutY = targetCenter.getY() - knightView.getFitHeight() / 2;
            currentTime += timePerSegment;
            timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(currentTime),
                    new KeyValue(knightView.layoutXProperty(), targetLayoutX, Interpolator.EASE_BOTH),
                    new KeyValue(knightView.layoutYProperty(), targetLayoutY, Interpolator.EASE_BOTH)
            ));
        }

        // Обработчик завершения анимации
        timeline.setOnFinished(e -> {
            KnightMove endMove = pathCopy.get(pathCopy.size() - 1);
            knightX = endMove.getX();
            knightY = endMove.getY();
            placeKnight(knightX, knightY); // Финальная установка

            // --- ОТМЕТКА ПОСЕЩЕННЫХ КЛЕТОК ПУТИ ЦВЕТОМ ВЕТКИ ---
            int maxDepthInPath = endMove.getDepth(); // Макс. глубина для расчета цвета
            for (int i = 1; i < pathCopy.size(); i++) { // Начинаем с 1 (пропускаем старт)
                KnightMove step = pathCopy.get(i);
                // Рассчитываем цвет ветки/глубины
                int branchIndex = step.getBranchIndex();
                Color stepColor = Color.GRAY; // Цвет по умолчанию, если что-то не так
                if (branchIndex >= 0 && branchIndex < branchBaseColors.length && step.getDepth() > 0) {
                    Color baseColor = branchBaseColors[branchIndex];
                    double darknessFactor = (maxDepthInPath > 0) ? Math.min(0.8, (double)(step.getDepth() - 1) / maxDepthInPath) : 0.0;
                    stepColor = baseColor.interpolate(Color.BLACK, darknessFactor);
                }
                String stepStyle = "-fx-background-color: " + toWebColor(stepColor) + ";";
                // Отмечаем клетку этим стилем, ЕСЛИ это первое посещение
                markCellAsVisitedFirstTime(step.getX(), step.getY(), stepStyle);
            }
            // --- КОНЕЦ ОТМЕТКИ ПОСЕЩЕННЫХ ---

            currentAnimation = null;
            clearTemporaryHighlights(); // Очищаем временную подсветку пути на доске
            updateClearButtonState();
            System.out.println("Анимация по выбранному пути завершена.");
        });

        currentAnimation = timeline; // Сохраняем ссылку на текущую анимацию
        timeline.play();
    }

    private void reapplyVisitedStyles() {
        for (String coord : visitedCells) {
            try {
                String[] parts = coord.split(",");
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                if (x >= 0 && x < rows && y >= 0 && y < cols && boardButtons[x][y] != null) {
                    if (!boardButtons[x][y].getStyleClass().contains(VISITED_CELL_STYLE_CLASS)) {
                        boardButtons[x][y].getStyleClass().add(VISITED_CELL_STYLE_CLASS);
                    }
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.err.println("Ошибка при парсинге координат для reapplyVisitedStyles: " + coord);
            }
        }
    }

    private void markCellAsVisitedFirstTime(int x, int y, String style) {
        String coord = x + "," + y;
        // Проверяем через Set, действительно ли это первое посещение
        if (visitedCellsSet.add(coord)) {
            // Сохраняем стиль в карте
            visitedCellStyles.put(coord, style);
            // Применяем стиль к кнопке
            if (boardButtons[x][y] != null) {
                // Убираем базовые классы перед применением инлайн стиля
                boardButtons[x][y].getStyleClass().removeAll("light-cell", "dark-cell", "wall-cell");
                boardButtons[x][y].setStyle(style);
                // Можно добавить общий маркерный класс, если нужно, но стиль важнее
                // boardButtons[x][y].getStyleClass().add("persistent-visited");
            }
            updateClearButtonState(); // Обновляем кнопку "Очистить"
        }
        // Если клетка уже была в visitedCellsSet, ничего не делаем (сохраняем первый цвет)
    }

    private void reapplyPersistentStyles() {
        // Сначала восстанавливаем базовый стиль для всех НЕстен
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (boardButtons == null || i >= boardButtons.length || boardButtons[i] == null || j >= boardButtons[i].length || boardButtons[i][j] == null) continue;
                Button button = boardButtons[i][j];
                String coord = i + "," + j;

                if (!visitedCellStyles.containsKey(coord)) { // Если клетка не имеет сохраненного стиля
                    // Восстанавливаем базовый стиль (светлый/темный/стена)
                    button.setStyle(null); // Сбрасываем инлайн стиль на всякий случай
                    button.getStyleClass().remove("persistent-visited"); // Убираем маркерный класс (если используется)

                    if (!board.isWall(i,j)) {
                        boolean isLight = (i + j) % 2 == 0;
                        String baseClass = isLight ? "light-cell" : "dark-cell";
                        String otherClass = isLight ? "dark-cell" : "light-cell";
                        button.getStyleClass().remove(otherClass);
                        if (!button.getStyleClass().contains(baseClass)) {
                            button.getStyleClass().add(baseClass);
                        }
                        // Убираем класс стены на всякий случай
                        button.getStyleClass().remove("wall-cell");
                    } else {
                        // Это стена, ставим класс стены
                        button.getStyleClass().removeAll("light-cell", "dark-cell");
                        if (!button.getStyleClass().contains("wall-cell")) {
                            button.getStyleClass().add("wall-cell");
                        }
                    }
                }
            }
        }
        // Затем применяем сохраненные стили поверх
        for (Map.Entry<String, String> entry : visitedCellStyles.entrySet()) {
            String coord = entry.getKey();
            String style = entry.getValue();
            try {
                String[] parts = coord.split(",");
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                if (x >= 0 && x < rows && y >= 0 && y < cols && boardButtons[x][y] != null) {
                    // Убираем базовые классы перед применением сохраненного стиля
                    boardButtons[x][y].getStyleClass().removeAll("light-cell", "dark-cell", "wall-cell");
                    boardButtons[x][y].setStyle(style);
                    // Если используется маркерный класс
                    // if (!boardButtons[x][y].getStyleClass().contains("persistent-visited")) {
                    //     boardButtons[x][y].getStyleClass().add("persistent-visited");
                    // }
                }
            } catch (Exception e) { // Ловим общую ошибку парсинга/индекса
                System.err.println("Ошибка при применении сохраненного стиля для " + coord);
            }
        }
    }

    private void clearTemporaryHighlights() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (boardButtons == null || i >= boardButtons.length || boardButtons[i] == null || j >= boardButtons[i].length || boardButtons[i][j] == null) continue;
                String coord = i + "," + j;
                // Если для клетки НЕТ сохраненного постоянного стиля
                if (!visitedCellStyles.containsKey(coord)) {
                    Button button = boardButtons[i][j];
                    button.setStyle(null); // Убираем инлайн стили (от showReachableCells, highlightAndAnimatePath)

                    // Восстанавливаем базовый стиль (светлый/темный/стена)
                    if (!board.isWall(i,j)) {
                        boolean isLight = (i + j) % 2 == 0;
                        String baseClass = isLight ? "light-cell" : "dark-cell";
                        String otherClass = isLight ? "dark-cell" : "light-cell";
                        button.getStyleClass().remove(otherClass);
                        if (!button.getStyleClass().contains(baseClass)) {
                            button.getStyleClass().add(baseClass);
                        }
                        button.getStyleClass().remove("wall-cell");
                    } else {
                        button.getStyleClass().removeAll("light-cell", "dark-cell");
                        if (!button.getStyleClass().contains("wall-cell")) {
                            button.getStyleClass().add("wall-cell");
                        }
                    }
                }
                // Если стиль есть в карте, НЕ ТРОГАЕМ его здесь
            }
        }
        // Переприменяем постоянные стили в конце, чтобы убедиться, что они поверх всего
        reapplyPersistentStyles();

        // Очищаем подсветку в списке
        if (movesWindowControllerInstance != null) {
            movesWindowControllerInstance.clearHighlight();
        }
    }
}