package com.example.musicvisualizer;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainApp extends Application {

    // ===== НАСТРОЙКИ =====
    private static final int BANDS = 32;

    // ===== ПОЛЯ =====
    private MediaPlayer mediaPlayer;
    private Rectangle[] bars = new Rectangle[BANDS];
    private double[] smoothedHeights = new double[BANDS];

    @Override
    public void start(Stage stage) {

        System.out.println("Приложение запущено");

        // ===== КНОПКИ =====
        Button loadButton = new Button("Загрузить трек");
        Button playButton = new Button("Play");
        Button stopButton = new Button("Stop");

        loadButton.setPrefWidth(220);
        playButton.setPrefWidth(220);
        stopButton.setPrefWidth(220);

        // ===== LABEL =====
        Label trackLabel = new Label("Трек не выбран");

        // ===== FILE CHOOSER =====
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите аудиофайл");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav")
        );

        // ===== ВИЗУАЛИЗАТОР =====
        HBox visualizer = new HBox(6);
        visualizer.setAlignment(Pos.BOTTOM_CENTER);
        visualizer.setPrefHeight(240);

        for (int i = 0; i < BANDS; i++) {
            Rectangle bar = new Rectangle(14, 5);
            bar.setArcWidth(6);
            bar.setArcHeight(6);
            bar.setFill(Color.LIMEGREEN);
            bars[i] = bar;
            visualizer.getChildren().add(bar);
        }

        // ===== ЗАГРУЗКА ТРЕКА =====
        loadButton.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(stage);

            if (file == null) {
                System.out.println("Файл не выбран");
                return;
            }

            trackLabel.setText("Выбран: " + file.getName());
            System.out.println("Выбран файл: " + file.getAbsolutePath());

            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                System.out.println("Старый плеер остановлен");
            }

            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            // ===== НАСТРОЙКА СПЕКТРА =====
            mediaPlayer.setAudioSpectrumInterval(0.05);
            mediaPlayer.setAudioSpectrumNumBands(BANDS);
            mediaPlayer.setAudioSpectrumThreshold(-60);

            mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
                for (int i = 0; i < BANDS; i++) {

                    double target = (magnitudes[i] + 60) * 4;
                    if (target < 5) target = 5;

                    // сглаживание
                    smoothedHeights[i] += (target - smoothedHeights[i]) * 0.2;
                    bars[i].setHeight(smoothedHeights[i]);

                    // цвет (зелёный → жёлтый → красный)
                    double hue = 120 - (smoothedHeights[i] / 220) * 120;
                    bars[i].setFill(Color.hsb(hue, 1.0, 1.0));
                }
            });

            playButton.setText("Play");
            System.out.println("MediaPlayer готов, статус: " + mediaPlayer.getStatus());
        });

        // ===== PLAY / PAUSE =====
        playButton.setOnAction(e -> {
            if (mediaPlayer == null) {
                System.out.println("Сначала выбери трек!");
                return;
            }

            MediaPlayer.Status status = mediaPlayer.getStatus();
            System.out.println("Статус: " + status);

            if (status == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playButton.setText("Play");
                System.out.println("Пауза");
            } else {
                mediaPlayer.play();
                playButton.setText("Pause");
                System.out.println("Воспроизведение");
            }
        });

        // ===== STOP =====
        stopButton.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                playButton.setText("Play");
                System.out.println("Стоп");
            }
        });

        // ===== LAYOUT =====
        VBox root = new VBox(18);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        root.getChildren().addAll(
                loadButton,
                playButton,
                stopButton,
                visualizer,
                trackLabel
        );

        Scene scene = new Scene(root, 720, 520);
        stage.setTitle("Music Visualizer");
        stage.setScene(scene);
        stage.show();

        System.out.println("Окно отображено");
    }

    public static void main(String[] args) {
        launch();
    }
}
