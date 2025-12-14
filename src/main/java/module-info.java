module com.example.musicvisualizer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    opens com.example.musicvisualizer to javafx.fxml;
    exports com.example.musicvisualizer;
}
