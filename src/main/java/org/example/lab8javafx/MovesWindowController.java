package org.example.lab8javafx;

import javafx.fxml.FXML;
import javafx.geometry.Point2D; // Убедитесь, что импорт есть
import javafx.scene.control.Button; // Импорт для Button
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox; // Импорт VBox
import javafx.scene.layout.HBox; // Импорт HBox (для области перетаскивания)
import javafx.scene.paint.Color; // Импорт Color
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.List;
import java.util.Set; // Импорт Set

/**
 * Контроллер окна для отображения списка ходов коня.
 */
public class MovesWindowController {

    @FXML private VBox rootPane; // Корневой контейнер окна (добавлен fx:id="rootPane" в FXML)
    @FXML private ListView<KnightMove> movesList; // Список для отображения ходов
    @FXML private Button minimizeButton; // Кнопка свернуть
    @FXML private Button closeButton; // Кнопка закрыть
    @FXML private HBox titleBar; // Панель с кнопками для перетаскивания

    private final KnightController mainController; // Ссылка на основной контроллер
    private Stage stage;                // Ссылка на окно для управления

    // Для подсветки строк в списке (Требование 3)
    private Set<KnightMove> highlightedPathMoves = new HashSet<>();
    private Color[] branchBaseColors;
    private int maxPathDepth;
    private double[] dragDelta; // Для перетаскивания

    /**
     * Конструктор класса.
     * @param mainController Основной контроллер приложения
     */
    public MovesWindowController(KnightController mainController) {
        this.mainController = mainController;
        // Получаем цвета из основного контроллера сразу
        this.branchBaseColors = mainController.getBranchBaseColors();
    }

    /**
     * Устанавливает ссылку на Stage для управления окном.
     * @param stage Окно, связанное с контроллером
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Возвращает ссылку на Stage этого окна.
     * @return Stage
     */
    public Stage getStage() {
        return stage;
    }


    /**
     * Инициализация интерфейса после загрузки FXML.
     */
    @FXML
    public void initialize() {
        // Настройка отображения элементов в ListView (обновлено для Требования 3)
        configureListViewCellFactory();

        // Обработчик выбора элемента в списке (остается)
        movesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && mainController != null) { // Добавлена проверка mainController
                mainController.highlightAndAnimatePath(newVal);
            }
        });

        // Обработчики для новых кнопок управления окном
        if (minimizeButton != null) {
            minimizeButton.setOnAction(event -> {
                if (stage != null) stage.setIconified(true);
            });
        }
        if (closeButton != null) {
            closeButton.setOnAction(event -> {
                if (stage != null) stage.close();
            });
        }

        // Настройка перемещения окна (теперь за корневой элемент или titleBar)
        makeStageDraggable();
    }

    /**
     * Устанавливает список ходов в ListView.
     * @param moves Список объектов KnightMove
     */
    public void setMoves(List<KnightMove> moves) {
        if (movesList != null) { // Проверка на null
            movesList.getItems().setAll(moves);
        }
    }

    /**
     * Делает окно перемещаемым с помощью мыши.
     * Перетаскивание теперь привязано к корневому VBox (rootPane).
     */
    private void makeStageDraggable() {
        // Используем rootPane для перетаскивания
        if (rootPane != null) { // Проверка на null
            rootPane.setOnMousePressed(event -> {
                dragDelta = new double[]{stage.getX() - event.getScreenX(), stage.getY() - event.getScreenY()};
            });

            rootPane.setOnMouseDragged(event -> {
                if (dragDelta != null) { // Убедимся, что press был
                    stage.setX(event.getScreenX() + dragDelta[0]);
                    stage.setY(event.getScreenY() + dragDelta[1]);
                }
            });
            rootPane.setOnMouseReleased(event -> {
                dragDelta = null; // Сбросить смещение после отпускания кнопки
            });
        } else {
            System.err.println("rootPane is null in MovesWindowController. Cannot make draggable.");
        }
    }

    // --- Методы для Требования 3 ---

    /**
     * Задает путь, который нужно подсветить в списке.
     * @param path Список ходов пути.
     * @param maxDepth Максимальная глубина в этом пути (для расчета цвета).
     */
    public void setHighlightedPath(List<KnightMove> path, int maxDepth) {
        this.highlightedPathMoves.clear();
        if (path != null) {
            this.highlightedPathMoves.addAll(path);
        }
        this.maxPathDepth = maxDepth; // Сохраняем максимальную глубину
        if (movesList != null) {
            movesList.refresh(); // Обновить отображение списка
        }
    }

    /**
     * Очищает подсветку пути в списке.
     */
    public void clearHighlight() {
        if (!this.highlightedPathMoves.isEmpty()) { // Очищать и обновлять только если было что-то выделено
            this.highlightedPathMoves.clear();
            if (movesList != null) {
                movesList.refresh();
            }
        }
    }

    /** Настраивает фабрику ячеек для ListView для поддержки подсветки. */
    private void configureListViewCellFactory() {
        if (movesList == null) return; // Добавлена проверка

        movesList.setCellFactory(lv -> new ListCell<KnightMove>() {
            @Override
            protected void updateItem(KnightMove item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null); // Очистить стиль для пустых ячеек
                    // Убрать фон по умолчанию, если используется CSS
                    getStyleClass().remove("highlighted-list-cell");
                } else {
                    setText(item.toString()); // Установить текст хода

                    // Проверить, является ли этот ход частью подсвечиваемого пути
                    if (highlightedPathMoves.contains(item) && branchBaseColors != null && item.getDepth() > 0) {
                        int branchIndex = item.getBranchIndex();
                        // Рассчитать цвет ячейки (как на доске)
                        if (branchIndex >= 0 && branchIndex < branchBaseColors.length) {
                            Color baseColor = branchBaseColors[branchIndex];
                            double darknessFactor = (maxPathDepth > 0) ? Math.min(0.8, (double)(item.getDepth() - 1) / maxPathDepth) : 0.0;
                            Color finalColor = baseColor.interpolate(Color.BLACK, darknessFactor);
                            String webColor = toWebColorHelper(finalColor);

                            // Определить цвет текста для контраста
                            String textColor = (finalColor.grayscale().getRed() < 0.5) ? "white" : "black";

                            // Применить стиль
                            setStyle("-fx-background-color: " + webColor + "; -fx-text-fill: " + textColor + ";");
                            // Можно добавить класс для доп. стилизации через CSS
                            // getStyleClass().add("highlighted-list-cell");
                        } else {
                            // Некорректный индекс ветви, сбросить стиль
                            setStyle(null);
                            // getStyleClass().remove("highlighted-list-cell");
                        }
                    } else {
                        // Ход не в подсвечиваемом пути, сбросить стиль
                        setStyle(null);
                        // getStyleClass().remove("highlighted-list-cell");
                    }
                }
            }
        });
    }

    // Вспомогательный метод для конвертации цвета (дублируем или берем из mainController)
    private String toWebColorHelper(Color color) {
        // Можно вызвать метод главного контроллера, если он public
        // return mainController.toWebColorPublic(color);
        // Или дублировать логику:
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}