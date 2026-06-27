module org.example.lab8javafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.media;

    requires org.controlsfx.controls;
    requires java.desktop;

    opens org.example.lab8javafx to javafx.fxml;
    exports org.example.lab8javafx;
}