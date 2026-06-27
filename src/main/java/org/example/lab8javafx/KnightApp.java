package org.example.lab8javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

import java.net.URL;

public class KnightApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        URL fxmlLocation = getClass().getResource("/org/example/lab8javafx/KnightGUI.fxml");
        if (fxmlLocation == null) {
            throw new RuntimeException("Не удалось найти KnightGUI.fxml в ресурсах");
        }
        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        KnightController controller = loader.getController();
        if (controller != null) {
            // Передаем Stage в контроллер для управления (перемещение, закрытие и т.д.)
            controller.setPrimaryStage(primaryStage);
        } else {
            System.err.println("Не удалось получить контроллер из FXML");
        }
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        Scene scene = new Scene(loader.load(), 600, 700);
        
        primaryStage.setResizable(false); // Фиксируем размер окна
        scene.setFill(Color.TRANSPARENT);
        // Создаем кастомные кнопки управления
        HBox windowControls = new HBox(5);
        windowControls.setAlignment(Pos.CENTER_RIGHT);
        windowControls.setPadding(new javafx.geometry.Insets(5));
        windowControls.setPrefWidth(600);

        Button minimizeButton = new Button("—");
        minimizeButton.getStyleClass().add("window-button");
        minimizeButton.setOnAction(e -> primaryStage.setIconified(true));

        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("window-button");
        closeButton.setOnAction(e -> primaryStage.close());

        windowControls.getChildren().addAll(minimizeButton, closeButton);

        // Добавляем кнопки в корневой VBox
        VBox root = (VBox) scene.getRoot();
        root.getChildren().add(0, windowControls);

        primaryStage.setTitle("Шахматный конь");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}