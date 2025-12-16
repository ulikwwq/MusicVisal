module com.example.musicvisualizer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;

    opens com.example.musicvisualizer to javafx.fxml;
    exports com.example.musicvisualizer;
}
