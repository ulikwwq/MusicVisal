package com.example.musicvisualizer;

import javafx.application.Application;
import javafx.animation.FadeTransition;
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
    private boolean darkTheme = true; // Текущая тема (по умолчанию темная)
    private boolean isThemeTransitioning = false; // Флаг для предотвращения повторных переходов

    private Button playBtn, prevBtn, nextBtn, playlistBtn, themeBtn, volumeBtn;
    private HBox visualizer;
    private StackPane root;
    private StackPane glass;

    /* ===== VISUALIZER COLORS ===== */
    private Color colorLow = Color.web("#4facfe");
    private Color colorHigh = Color.web("#ff4e50");

    @Override
    public void start(Stage stage) {

        /* ================= VISUALIZER ================= */
        visualizer = new HBox(-0.88);
        visualizer.setAlignment(Pos.BOTTOM_CENTER);
        visualizer.setPrefHeight(220);
        visualizer.setOpacity(0);

        for (int i = 0; i < BANDS; i++) {
            Rectangle bar = new Rectangle(10, 5);
            bar.setArcWidth(6);
            bar.setArcHeight(6);
            bars[i] = bar;
            smoothedHeights[i] = 5;
            visualizer.getChildren().add(bar);
        }

        emptyLabel = new Label("🎵 Add music files to start\nSupported: MP3, WAV");
        emptyLabel.setStyle("-fx-text-fill:#AAAAAA; -fx-font-size:16px;");
        emptyLabel.setAlignment(Pos.CENTER);

        // Контейнер для центрирования визуализатора
        HBox visualizerWrapper = new HBox(visualizer);
        visualizerWrapper.setAlignment(Pos.CENTER);
        visualizerWrapper.setPrefHeight(220);
        visualizerWrapper.setMinHeight(220);
        visualizerWrapper.setMaxHeight(220);

        // Визуализатор и метка в StackPane
        StackPane visualArea = new StackPane(visualizerWrapper, emptyLabel);
        visualArea.setMinHeight(220);
        visualArea.setPrefHeight(220);
        visualArea.setMaxHeight(220);

        /* ================= CONTROLS ================= */
        prevBtn = createButton("⏮");
        playBtn = createButton("▶");
        nextBtn = createButton("⏭");
        playlistBtn = createButton("☰");
        themeBtn = createButton("🌙"); // Кнопка переключения темы

        volumeBtn = createButton("🔊");
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

        // Убрана кнопка загрузки с основной панели
        HBox controls = new HBox(10,
                volumeBtn, volumeSlider,
                prevBtn, playBtn, nextBtn, playlistBtn, themeBtn
        );
        controls.setAlignment(Pos.CENTER);

        /* ================= COLOR BUTTONS ================= */
        HBox colorControls = new HBox(10,
                createColorBtn(Color.web("#4facfe"), Color.web("#ff4e50")),
                createColorBtn(Color.web("#00c6ff"), Color.web("#0072ff")),
                createColorBtn(Color.web("#43e97b"), Color.web("#38f9d7")),
                createColorBtn(Color.web("#fa709a"), Color.web("#fee140")),
                createColorBtn(Color.web("#667eea"), Color.web("#764ba2"))
        );
        colorControls.setAlignment(Pos.CENTER);

        nowPlayingLabel = new Label("—");
        nowPlayingLabel.setStyle(
                "-fx-text-fill:#E0E0E0;" +
                        "-fx-background-color:rgba(255,255,255,0.14);" +
                        "-fx-padding:6 14;" +
                        "-fx-background-radius:8;"
        );

        progressSlider = new Slider();
        progressSlider.setPrefWidth(FIXED_WIDTH);

        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill:#B0B0B0;");

        VBox progressBox = new VBox(6, progressSlider, timeLabel);
        progressBox.setAlignment(Pos.CENTER);

        VBox content = new VBox(
                18,
                visualArea,
                nowPlayingLabel,
                progressBox,
                controls,
                colorControls
        );
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(26));

        glass = new StackPane(content);
        glass.setStyle("-fx-background-color:rgba(20,20,20,0.55); -fx-background-radius:18;");
        glass.setMaxWidth(820);

        root = new StackPane(glass);
        root.setPadding(new Insets(30));

        // Сначала устанавливаем темную тему напрямую
        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(
                        0,0,1,1,true,CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#0f2027")),
                        new Stop(0.5, Color.web("#16222a")),
                        new Stop(1, Color.web("#000000"))
                ),
                CornerRadii.EMPTY, Insets.EMPTY
        )));

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Music Visualizer");
        stage.show();

        /* ================= EVENTS ================= */
        // Кнопка загрузки теперь только в окне плейлиста
        playlistBtn.setOnAction(e -> showPlaylistWindow());
        themeBtn.setOnAction(e -> toggleTheme());

        playBtn.setOnAction(e -> togglePlay());
        nextBtn.setOnAction(e -> playNext());
        prevBtn.setOnAction(e -> playPrevious());

        // Исправлено: добавлена обработка перетаскивания ползунка
        progressSlider.setOnMousePressed(e -> isDragging = true);
        progressSlider.setOnMouseDragged(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.millis(progressSlider.getValue()));
                timeLabel.setText(format(Duration.millis(progressSlider.getValue())) + " / " +
                        format(mediaPlayer.getTotalDuration()));
            }
        });
        progressSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.millis(progressSlider.getValue()));
            }
            isDragging = false;
        });

        loadPlaylist();
        updateControlsState();
        refreshBarsColor(); // Инициализация цвета при запуске
    }

    /* ================= THEME MANAGEMENT ================= */
    private void toggleTheme() {
        if (isThemeTransitioning) return; // Не запускать новый переход, пока старый не завершен

        isThemeTransitioning = true;

        // Создаем плавный fade out для текущей темы
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), glass);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.3);

        fadeOut.setOnFinished(e -> {
            // Меняем тему
            darkTheme = !darkTheme;
            if (darkTheme) {
                applyDarkTheme();
                themeBtn.setText("🌙");
            } else {
                applyLightTheme();
                themeBtn.setText("☀");
            }

            // Плавный fade in для новой темы
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), glass);
            fadeIn.setFromValue(0.3);
            fadeIn.setToValue(1.0);

            fadeIn.setOnFinished(event -> {
                isThemeTransitioning = false;
            });

            fadeIn.play();
        });

        fadeOut.play();
    }

    private void applyDarkTheme() {
        // Плавный переход для фона
        Background newBackground = new Background(new BackgroundFill(
                new LinearGradient(
                        0,0,1,1,true,CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#0f2027")),
                        new Stop(0.5, Color.web("#16222a")),
                        new Stop(1, Color.web("#000000"))
                ),
                CornerRadii.EMPTY, Insets.EMPTY
        ));

        root.setBackground(newBackground);

        // Плавный переход для стеклянной панели
        glass.setStyle("-fx-background-color:rgba(20,20,20,0.55); -fx-background-radius:18;");

        // Плавный переход для меток
        FadeTransition labelTransition = new FadeTransition(Duration.millis(150), nowPlayingLabel);
        labelTransition.setFromValue(0.7);
        labelTransition.setToValue(1.0);
        labelTransition.play();

        nowPlayingLabel.setStyle(
                "-fx-text-fill:#E0E0E0;" +
                        "-fx-background-color:rgba(255,255,255,0.14);" +
                        "-fx-padding:6 14;" +
                        "-fx-background-radius:8;"
        );

        FadeTransition timeTransition = new FadeTransition(Duration.millis(150), timeLabel);
        timeTransition.setFromValue(0.7);
        timeTransition.setToValue(1.0);
        timeTransition.play();

        timeLabel.setStyle("-fx-text-fill:#B0B0B0;");

        // Сохраняем текущую прозрачность emptyLabel перед сменой темы
        double currentOpacity = emptyLabel.getOpacity();

        // Устанавливаем только стиль цвета, не меняем прозрачность
        emptyLabel.setStyle("-fx-text-fill:#AAAAAA; -fx-font-size:16px;");
        // Восстанавливаем прозрачность
        emptyLabel.setOpacity(currentOpacity);

        // Плавный переход для слайдеров
        FadeTransition sliderTransition1 = new FadeTransition(Duration.millis(150), progressSlider);
        sliderTransition1.setFromValue(0.7);
        sliderTransition1.setToValue(1.0);
        sliderTransition1.play();

        FadeTransition sliderTransition2 = new FadeTransition(Duration.millis(150), volumeSlider);
        sliderTransition2.setFromValue(0.7);
        sliderTransition2.setToValue(1.0);
        sliderTransition2.play();

        progressSlider.setStyle("-fx-control-inner-background: #333;");
        volumeSlider.setStyle("-fx-control-inner-background: #333;");

        // Плавный переход для кнопок
        updateButtonStylesWithAnimation();
    }

    private void applyLightTheme() {
        // Плавный переход для фона
        Background newBackground = new Background(new BackgroundFill(
                new LinearGradient(
                        0,0,1,1,true,CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#f5f7fa")),
                        new Stop(0.5, Color.web("#c3cfe2")),
                        new Stop(1, Color.web("#e4e8f0"))
                ),
                CornerRadii.EMPTY, Insets.EMPTY
        ));

        root.setBackground(newBackground);

        // Плавный переход для стеклянной панели
        glass.setStyle("-fx-background-color:rgba(255,255,255,0.75); -fx-background-radius:18;");

        // Плавный переход для меток
        FadeTransition labelTransition = new FadeTransition(Duration.millis(150), nowPlayingLabel);
        labelTransition.setFromValue(0.7);
        labelTransition.setToValue(1.0);
        labelTransition.play();

        nowPlayingLabel.setStyle(
                "-fx-text-fill:#333333;" +
                        "-fx-background-color:rgba(0,0,0,0.08);" +
                        "-fx-padding:6 14;" +
                        "-fx-background-radius:8;"
        );

        FadeTransition timeTransition = new FadeTransition(Duration.millis(150), timeLabel);
        timeTransition.setFromValue(0.7);
        timeTransition.setToValue(1.0);
        timeTransition.play();

        timeLabel.setStyle("-fx-text-fill:#666666;");

        // Сохраняем текущую прозрачность emptyLabel перед сменой темы
        double currentOpacity = emptyLabel.getOpacity();

        // Устанавливаем только стиль цвета, не меняем прозрачность
        emptyLabel.setStyle("-fx-text-fill:#777777; -fx-font-size:16px;");
        // Восстанавливаем прозрачность
        emptyLabel.setOpacity(currentOpacity);

        // Плавный переход для слайдеров
        FadeTransition sliderTransition1 = new FadeTransition(Duration.millis(150), progressSlider);
        sliderTransition1.setFromValue(0.7);
        sliderTransition1.setToValue(1.0);
        sliderTransition1.play();

        FadeTransition sliderTransition2 = new FadeTransition(Duration.millis(150), volumeSlider);
        sliderTransition2.setFromValue(0.7);
        sliderTransition2.setToValue(1.0);
        sliderTransition2.play();

        progressSlider.setStyle("-fx-control-inner-background: #e0e0e0;");
        volumeSlider.setStyle("-fx-control-inner-background: #e0e0e0;");

        // Плавный переход для кнопок
        updateButtonStylesWithAnimation();
    }

    private void updateButtonStylesWithAnimation() {
        String buttonStyle;
        if (darkTheme) {
            buttonStyle = "-fx-background-color:rgba(255,255,255,0.14); -fx-text-fill:white; -fx-background-radius:10;";
        } else {
            buttonStyle = "-fx-background-color:rgba(0,0,0,0.08); -fx-text-fill:#333333; -fx-background-radius:10;";
        }

        // Плавный переход для всех кнопок
        Button[] buttons = {prevBtn, playBtn, nextBtn, playlistBtn, themeBtn, volumeBtn};

        for (Button button : buttons) {
            FadeTransition fade = new FadeTransition(Duration.millis(150), button);
            fade.setFromValue(0.7);
            fade.setToValue(1.0);
            fade.setOnFinished(e -> button.setStyle(buttonStyle));
            fade.play();
        }
    }

    private void updateButtonStyles() {
        String buttonStyle;
        if (darkTheme) {
            buttonStyle = "-fx-background-color:rgba(255,255,255,0.14); -fx-text-fill:white; -fx-background-radius:10;";
        } else {
            buttonStyle = "-fx-background-color:rgba(0,0,0,0.08); -fx-text-fill:#333333; -fx-background-radius:10;";
        }

        // Обновляем стили всех основных кнопок
        prevBtn.setStyle(buttonStyle);
        playBtn.setStyle(buttonStyle);
        nextBtn.setStyle(buttonStyle);
        playlistBtn.setStyle(buttonStyle);
        themeBtn.setStyle(buttonStyle);
        volumeBtn.setStyle(buttonStyle);
    }

    /* ================= STATE ================= */
    private void updateControlsState() {
        boolean hasPlaylist = !playlist.isEmpty();

        // Кнопки плеера должны быть неактивны только когда нет плейлиста
        playBtn.setDisable(!hasPlaylist);
        prevBtn.setDisable(!hasPlaylist);
        nextBtn.setDisable(!hasPlaylist);

        // Кнопка плейлиста должна быть всегда активна!
        playlistBtn.setDisable(false);

        // Ползунок должен быть активен всегда, когда есть плейлист
        progressSlider.setDisable(!hasPlaylist);

        // Делаем метку "Add music..." полностью прозрачной когда есть плейлист
        FadeTransition ft = new FadeTransition(Duration.millis(300), emptyLabel);
        ft.setToValue(hasPlaylist ? 0 : 1);
        ft.play();

        // Делаем визуализатор видимым когда есть плейлист
        visualizer.setVisible(hasPlaylist);

        // Также делаем визуализатор непрозрачным когда есть плейлист
        if (hasPlaylist) {
            FadeTransition visualizerFade = new FadeTransition(Duration.millis(300), visualizer);
            visualizerFade.setToValue(1.0);
            visualizerFade.play();
        } else {
            visualizer.setOpacity(0);
        }
    }

    /* ================= PLAYER ================= */
    private void togglePlay() {
        if (mediaPlayer == null) {
            playTrack(currentIndex >= 0 ? currentIndex : 0);
            return;
        }

        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playBtn.setText("▶");
        } else {
            mediaPlayer.play();
            playBtn.setText("⏸");
        }
    }

    private void playTrack(int index) {
        if (playlist.isEmpty()) return;
        if (index < 0 || index >= playlist.size()) index = 0;

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

            // Плавное появление визуализатора
            FadeTransition ft = new FadeTransition(Duration.millis(400), visualizer);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

            // Сбрасываем значение ползунка при загрузке новой песни
            progressSlider.setValue(0);
            timeLabel.setText("00:00 / " + format(mediaPlayer.getTotalDuration()));
        });

        mediaPlayer.currentTimeProperty().addListener((o,a,b)->{
            if (!isDragging) {
                progressSlider.setValue(b.toMillis());
                timeLabel.setText(format(b)+" / "+format(mediaPlayer.getTotalDuration()));
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

    /* ================= SPECTRUM ================= */
    private void setupSpectrum() {
        mediaPlayer.setAudioSpectrumInterval(0.045);
        mediaPlayer.setAudioSpectrumNumBands(BANDS);
        mediaPlayer.setAudioSpectrumThreshold(-60);

        DropShadow glow = new DropShadow(12, Color.rgb(255,255,255,0.18));

        mediaPlayer.setAudioSpectrumListener((t,d,mags,ph)->{
            double sum = 0;
            for (double m : mags) sum += (m + 60);
            double energy = Math.min(1.0, sum / (mags.length * 60));

            Color base = colorLow.interpolate(colorHigh, energy);
            double center = (BANDS - 1) / 2.0;

            for (int i = 0; i < BANDS; i++) {
                double dist = Math.abs(i - center) / center;
                int idx = Math.min((int)(dist * (BANDS / 2)), mags.length - 1);

                double h = (mags[idx] + 60) * 3.2;
                smoothedHeights[i] += (h - smoothedHeights[i]) * 0.18;

                bars[i].setHeight(Math.max(6, smoothedHeights[i]));
                bars[i].setFill(new Color(
                        base.getRed(), base.getGreen(), base.getBlue(),
                        Math.min(1, 0.4 + smoothedHeights[i] / 180)
                ));
                bars[i].setEffect(glow);
            }
        });
    }

    /* ================= COLOR UPDATE ================= */
    private void refreshBarsColor() {
        double max = 0;
        for (double h : smoothedHeights) if (h > max) max = h;

        double energy = Math.min(1.0, max / 180);
        Color base = colorLow.interpolate(colorHigh, energy);
        DropShadow glow = new DropShadow(12, Color.rgb(255,255,255,0.18));

        for (int i = 0; i < BANDS; i++) {
            bars[i].setFill(new Color(
                    base.getRed(),
                    base.getGreen(),
                    base.getBlue(),
                    Math.min(1, 0.4 + smoothedHeights[i] / 180)
            ));
            bars[i].setEffect(glow);
        }
    }

    /* ================= PLAYLIST ================= */
    private void showPlaylistWindow() {
        Stage win = new Stage();
        ListView<String> list = new ListView<>();
        refreshPlaylistView(list);

        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int selectedIndex = list.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0 && selectedIndex < playlist.size()) {
                    playTrack(selectedIndex);
                    win.close();
                }
            }
        });

        Button add = new Button("➕ Add");
        Button del = new Button("❌ Delete");

        add.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav")
            );

            var files = fc.showOpenMultipleDialog(win);
            if (files == null || files.isEmpty()) return;

            playlist.addAll(files);
            savePlaylist();
            playTrack(playlist.size() - files.size());
            refreshPlaylistView(list);
            updateControlsState();
        });

        del.setOnAction(e -> {
            int idx = list.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                playlist.remove(idx);
                savePlaylist();
                refreshPlaylistView(list);
                updateControlsState();
            }
        });

        HBox buttons = new HBox(10, add, del);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(list, buttons);
        root.setPadding(new Insets(10));

        win.setScene(new Scene(root, 320, 420));
        win.initModality(Modality.APPLICATION_MODAL);
        win.show();
    }

    /* ================= HELPERS ================= */
    private void refreshPlaylistView(ListView<String> list) {
        list.getItems().clear();
        for (File f : playlist) list.getItems().add(f.getName());
    }

    private Button createButton(String text) {
        Button b = new Button(text);
        b.setPrefSize(40, 36);
        b.setCursor(Cursor.HAND);
        b.setStyle("-fx-background-color:rgba(255,255,255,0.14); -fx-text-fill:white; -fx-background-radius:10;");
        return b;
    }

    private Button createColorBtn(Color low, Color high) {
        Button b = new Button();
        b.setPrefSize(26, 26);
        b.setCursor(Cursor.HAND);
        b.setStyle("-fx-background-radius:50%; -fx-background-color:linear-gradient(to bottom right,"
                + toHex(low) + "," + toHex(high) + ");");
        b.setOnAction(e -> {
            colorLow = low;
            colorHigh = high;
            refreshBarsColor(); // Теперь цвет меняется сразу при нажатии на кнопку
        });
        return b;
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int)(c.getRed()*255),
                (int)(c.getGreen()*255),
                (int)(c.getBlue()*255));
    }

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
                File a = new File(line);
                if (a.exists()) playlist.add(a);
            }
        } catch (Exception ignored) {}
    }

    private String format(Duration d) {
        int s = (int) d.toSeconds();
        return String.format("%02d:%02d", s/60, s%60);
    }

    public static void main(String[] args) {
        launch();
    }
}