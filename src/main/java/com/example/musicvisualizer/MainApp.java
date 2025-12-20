package com.example.musicvisualizer;

import javafx.application.Application;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.*;
import javafx.util.Duration;

import java.io.*;
import java.util.ArrayList;

public class MainApp extends Application {

    private static final int BANDS = 68;
    private static final double FIXED_WIDTH = 520;
    private static final String PLAYLIST_FILE = "playlist.txt";

    private MediaPlayer mediaPlayer;

    private final Rectangle[] bars = new Rectangle[BANDS];
    private final double[] smoothedHeights = new double[BANDS];

    private final ArrayList<File> playlist = new ArrayList<>();
    private int currentIndex = -1;

    private Label nowPlayingLabel;
    private Label emptyLabel;
    private Label timeLabel;

    private Slider progressSlider;
    private Slider volumeSlider;

    private boolean muted = false;
    private boolean isDragging = false;
    private double lastVolume = 1.0;

    private Button playBtn, prevBtn, nextBtn, playlistBtn;

    /* ====== ADAPTIVE GRADIENT PALETTE ====== */
    private static final Color COLOR_LOW = Color.web("#4facfe");   // cold
    private static final Color COLOR_HIGH = Color.web("#ff4e50");  // warm

    @Override
    public void start(Stage stage) {

        /* ================== ВИЗУАЛИЗАТОР ================== */
        HBox visualizer = new HBox(-0.88);
        visualizer.setAlignment(Pos.BOTTOM_CENTER);
        visualizer.setPrefHeight(220);
        visualizer.setOpacity(0);

        for (int i = 0; i < BANDS; i++) {
            Rectangle bar = new Rectangle(10, 5);
            bar.setArcWidth(6);
            bar.setArcHeight(6);
            bars[i] = bar;
            visualizer.getChildren().add(bar);
        }

        emptyLabel = new Label("🎵 Add music files to start\nSupported: MP3, WAV");
        emptyLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 16px;");
        emptyLabel.setAlignment(Pos.CENTER);

        StackPane visualArea = new StackPane(visualizer, emptyLabel);

        /* ================== КНОПКИ ================== */
        prevBtn = createButton("⏮");
        playBtn = createButton("▶");
        nextBtn = createButton("⏭");
        Button loadBtn = createButton("⏏");
        playlistBtn = createButton("☰");

        Button volumeBtn = createButton("🔊");
        volumeSlider = new Slider(0, 1, 1);

        volumeBtn.setOnAction(e -> {
            if (mediaPlayer == null) return;
            if (!muted) {
                lastVolume = volumeSlider.getValue();
                volumeSlider.setValue(0);
                mediaPlayer.setVolume(0);
                volumeBtn.setText("🔇");
                muted = true;
            } else {
                volumeSlider.setValue(lastVolume);
                mediaPlayer.setVolume(lastVolume);
                volumeBtn.setText("🔊");
                muted = false;
            }
        });

        volumeSlider.valueProperty().addListener((o, a, b) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(b.doubleValue());
        });

        HBox controls = new HBox(10,
                volumeBtn, volumeSlider,
                loadBtn, prevBtn, playBtn, nextBtn, playlistBtn
        );
        controls.setAlignment(Pos.CENTER);

        nowPlayingLabel = new Label("—");
        nowPlayingLabel.setStyle(
                "-fx-text-fill: #E0E0E0;" +
                        "-fx-background-color: rgba(255,255,255,0.14);" +
                        "-fx-padding: 6 14;" +
                        "-fx-background-radius: 8;"
        );

        progressSlider = new Slider();
        progressSlider.setPrefWidth(FIXED_WIDTH);

        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill: #B0B0B0;");

        VBox progressBox = new VBox(6, progressSlider, timeLabel);
        progressBox.setAlignment(Pos.CENTER);

        VBox content = new VBox(20, visualArea, nowPlayingLabel, progressBox, controls);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(26));

        StackPane glass = new StackPane(content);
        glass.setStyle("-fx-background-color: rgba(20,20,20,0.55); -fx-background-radius: 18;");
        glass.setMaxWidth(820);

        StackPane root = new StackPane(glass);
        root.setPadding(new Insets(30));
        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(
                        0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#0f2027")),
                        new Stop(0.5, Color.web("#16222a")),
                        new Stop(1, Color.web("#000000"))
                ),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Music Visualizer");
        stage.show();

        /* ================== СОБЫТИЯ ================== */
        loadBtn.setOnAction(e -> addTracks(stage));
        nextBtn.setOnAction(e -> playNext());
        prevBtn.setOnAction(e -> playPrevious());
        playlistBtn.setOnAction(e -> showPlaylistWindow());

        playBtn.setOnAction(e -> {
            if (mediaPlayer == null) return;

            ScaleTransition st = new ScaleTransition(Duration.millis(120), playBtn);
            st.setFromX(1);
            st.setFromY(1);
            st.setToX(1.12);
            st.setToY(1.12);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();

            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playBtn.setText("▶");
            } else {
                mediaPlayer.play();
                playBtn.setText("⏸");
            }
        });

        progressSlider.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> isDragging = true);
        progressSlider.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (mediaPlayer != null)
                mediaPlayer.seek(Duration.millis(progressSlider.getValue()));
            isDragging = false;
        });

        loadPlaylist();
        updateControlsState();
    }

    /* ================== STATE ================== */
    private void updateControlsState() {
        boolean hasMedia = mediaPlayer != null;
        boolean hasPlaylist = !playlist.isEmpty();

        playBtn.setDisable(!hasMedia);
        prevBtn.setDisable(!hasMedia);
        nextBtn.setDisable(!hasMedia);
        playlistBtn.setDisable(!hasPlaylist);
        progressSlider.setDisable(!hasMedia);

        FadeTransition ft = new FadeTransition(Duration.millis(300), emptyLabel);
        ft.setToValue(hasPlaylist ? 0 : 1);
        ft.play();
    }

    /* ================== PLAYER ================== */
    private void addTracks(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav")
        );

        var files = fc.showOpenMultipleDialog(stage);
        if (files == null || files.isEmpty()) return;

        int startIndex = playlist.size();
        playlist.addAll(files);

        playTrack(startIndex);
        savePlaylist();
        updateControlsState();
    }

    private void playTrack(int index) {
        if (index < 0 || index >= playlist.size()) return;
        currentIndex = index;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        File file = playlist.get(index);
        mediaPlayer = new MediaPlayer(new Media(file.toURI().toString()));
        mediaPlayer.setVolume(volumeSlider.getValue());

        nowPlayingLabel.setText(file.getName());

        mediaPlayer.setOnReady(() -> {
            progressSlider.setMax(mediaPlayer.getTotalDuration().toMillis());
            mediaPlayer.play();
            playBtn.setText("⏸");

            FadeTransition ft = new FadeTransition(Duration.millis(600), bars[0].getParent());
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

            updateControlsState();
        });

        mediaPlayer.currentTimeProperty().addListener((o, a, b) -> {
            if (!isDragging) {
                progressSlider.setValue(b.toMillis());
                timeLabel.setText(format(b) + " / " + format(mediaPlayer.getTotalDuration()));
            }
        });

        setupSpectrum();
    }

    private void playNext() {
        playTrack((currentIndex + 1) % playlist.size());
    }

    private void playPrevious() {
        playTrack((currentIndex - 1 + playlist.size()) % playlist.size());
    }

    /* ================== ADAPTIVE GRADIENT VISUALIZER ================== */
    private void setupSpectrum() {
        mediaPlayer.setAudioSpectrumInterval(0.045);
        mediaPlayer.setAudioSpectrumNumBands(BANDS);
        mediaPlayer.setAudioSpectrumThreshold(-60);

        DropShadow glow = new DropShadow(12, Color.rgb(255, 255, 255, 0.18));

        mediaPlayer.setAudioSpectrumListener((t, d, mags, ph) -> {

            double sum = 0;
            for (double m : mags) sum += (m + 60);
            double energyNorm = Math.min(1.0, sum / (mags.length * 60.0));

            Color baseColor = COLOR_LOW.interpolate(COLOR_HIGH, energyNorm);

            double center = (BANDS - 1) / 2.0;

            for (int i = 0; i < BANDS; i++) {
                double dist = Math.abs(i - center) / center;
                int idx = Math.min((int)(dist * (BANDS / 2)), mags.length - 1);

                double energy = (mags[idx] + 60) * 3.2;
                smoothedHeights[i] += (energy - smoothedHeights[i]) * 0.18;

                double h = Math.max(6, smoothedHeights[i]);
                bars[i].setHeight(h);

                double brightness = Math.min(1.0, 0.4 + h / 180);
                bars[i].setFill(new Color(
                        baseColor.getRed(),
                        baseColor.getGreen(),
                        baseColor.getBlue(),
                        brightness
                ));

                bars[i].setEffect(glow);
            }
        });
    }

    /* ================== PLAYLIST ================== */
    private void showPlaylistWindow() {
        if (playlist.isEmpty()) return;

        Stage win = new Stage();
        ListView<String> list = new ListView<>();
        for (File f : playlist) list.getItems().add(f.getName());

        list.setOnMouseClicked(e -> {
            int idx = list.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                playTrack(idx);
                win.close();
            }
        });

        win.setScene(new Scene(list, 300, 400));
        win.initModality(Modality.APPLICATION_MODAL);
        win.show();
    }

    /* ================== FILE ================== */
    private void savePlaylist() {
        try (PrintWriter w = new PrintWriter(new FileWriter(PLAYLIST_FILE))) {
            for (File f : playlist) w.println(f.getAbsolutePath());
        } catch (Exception ignored) {}
    }

    private void loadPlaylist() {
        File f = new File(PLAYLIST_FILE);
        if (!f.exists()) return;

        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                File audio = new File(line);
                if (audio.exists()) playlist.add(audio);
            }
        } catch (Exception ignored) {}
    }

    private Button createButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(40, 36);
        btn.setCursor(Cursor.HAND);
        btn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.14);" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;"
        );
        return btn;
    }

    private String format(Duration d) {
        int s = (int) d.toSeconds();
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    public static void main(String[] args) {
        launch();
    }
}
