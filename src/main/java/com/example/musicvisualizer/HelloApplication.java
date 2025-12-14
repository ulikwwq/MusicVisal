package com.example.musicvisualizer;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class HelloApplication extends Application {

    private File selectedFile;
    private MediaPlayer mediaPlayer;

    @Override
    public void start(Stage stage) {

        Button loadButton = new Button("Загрузить трек");
        Button playButton = new Button("Play");

        loadButton.setPrefWidth(200);
        playButton.setPrefWidth(200);

        Label trackLabel = new Label("Трек не выбран");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите аудиофайл");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav")
        );

        // Загрузка трека
        loadButton.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                selectedFile = file;
                trackLabel.setText("Выбран: " + file.getName());

                // Если уже был плеер — останавливаем
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }

                Media media = new Media(file.toURI().toString());
                mediaPlayer = new MediaPlayer(media);

                System.out.println("Загружен файл: " + file.getAbsolutePath());
            }
        });

        // Кнопка Play
        playButton.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.play();
                System.out.println("Воспроизведение началось");
            } else {
                System.out.println("Сначала выбери трек!");
            }
        });

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(loadButton, playButton, trackLabel);

        Scene scene = new Scene(root, 600, 400);
        stage.setTitle("Music Visualizer");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
