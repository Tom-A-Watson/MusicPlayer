/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.model.Song;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//   TODO:
//    private static List<Song> nowPlayingList;
//    private static int nowPlayingIndex;
//    private static boolean isLoopActive = false;
//    private static boolean isShuffleActive = false;

/**
 * @author Almas Baim (https://github.com/AlmasB)
 */
public final class MediaPaneController implements Initializable {

    @FXML
    private HBox volumePane;
    @FXML
    private VolumeBoxController volumePaneController;

    @FXML
    private ImageView nowPlayingArtwork;
    @FXML
    private Label nowPlayingTitle;

    @FXML
    private Slider timeSlider;
    @FXML
    private Region frontSliderTrack;

    @FXML
    private Label timePassedLabel;
    @FXML
    private Label timeRemainingLabel;

    private ScheduledExecutorService executorService;

    /**
     * Each tick in this counter is 250ms.
     */
    private int timerCounter = 0;

    private BooleanProperty isPlaying = new SimpleBooleanProperty(false);

    private MediaPlayer mediaPlayer = null;
    private Song song = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTimeSlider();

        executorService = Executors.newScheduledThreadPool(4);
        executorService.scheduleAtFixedRate(new TimeUpdater(), 0, 250, TimeUnit.MILLISECONDS);
    }

    private void initTimeSlider() {
        timeSlider.valueChangingProperty().addListener((slider, wasChanging, isChanging) -> {
            if (wasChanging) {
                int seconds = (int) Math.round(timeSlider.getValue() / 4.0);
                timeSlider.setValue(seconds * 4);
                seek(seconds);
            }
        });

        timeSlider.valueProperty().addListener((slider, oldValue, newValue) -> {
            double previous = oldValue.doubleValue();
            double current = newValue.doubleValue();
            if (!timeSlider.isValueChanging() && current != previous + 1 && !timeSlider.isPressed()) {
                int seconds = (int) Math.round(current / 4.0);
                timeSlider.setValue(seconds * 4);
                seek(seconds);
            }
        });

        frontSliderTrack.prefWidthProperty().bind(timeSlider.widthProperty().multiply(timeSlider.valueProperty().divide(timeSlider.maxProperty())));
    }

    public BooleanProperty playingProperty() {
        return isPlaying;
    }

    /**
     * @return true if a song is currently playing
     */
    public boolean isPlaying() {
        return isPlaying.get();
    }

    public void play(Song song) {
        if (this.song != null) {
            var prevSong = this.song;
            prevSong.setPlaying(false);

            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }
        }

        this.song = song;

        song.setPlayCount(song.getPlayCount() + 1);
        song.setPlayDate(LocalDateTime.now());
        song.setPlaying(true);

        nowPlayingTitle.textProperty().bind(song.titleProperty());
        nowPlayingArtwork.imageProperty().bind(song.artworkProperty());

        timeSlider.setMax(song.getLengthInSeconds() * 4);

        timerCounter = 0;

        Media media = new Media(song.getFile().toUri().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.volumeProperty().bind(volumePaneController.volumeProperty().divide(200));
        mediaPlayer.setOnEndOfMedia(this::skip);
        mediaPlayer.muteProperty().bind(volumePaneController.mutedProperty());

        isPlaying.bind(mediaPlayer.statusProperty().isEqualTo(MediaPlayer.Status.PLAYING));

        play();
    }

    public void back() {
        // TODO: || nowPlayingIndex == 0
        if (timerCounter > 20) {
            seek(0);
        } else {
            // TODO:
//            boolean isPlaying = isPlaying();
//            setNowPlaying(nowPlayingList.get(nowPlayingIndex - 1));
//
//            if (isPlaying) {
//                play();
//            }
        }
    }

    //    public static void toggleShuffle() {
//        isShuffleActive = !isShuffleActive;
//
//        if (isShuffleActive) {
//            Collections.shuffle(nowPlayingList);
//        } else {
//            Collections.sort(nowPlayingList);
//        }
//
//        nowPlayingIndex = nowPlayingList.indexOf(nowPlaying);
//    }

    /**
     * Skips the currently playing song.
     */
    public void skip() {
//        if (nowPlayingIndex < nowPlayingList.size() - 1) {
//            boolean isPlaying = isPlaying();

//            setNowPlaying(nowPlayingList.get(nowPlayingIndex + 1));
//            if (isPlaying) {
//                play();
//            }
//        } else if (isLoopActive) {
//            boolean isPlaying = isPlaying();

//            nowPlayingIndex = 0;
//            setNowPlaying(nowPlayingList.get(nowPlayingIndex));
//            if (isPlaying) {
//                play();
//            }
//        } else {

//            nowPlayingIndex = 0;
//            setNowPlaying(nowPlayingList.get(nowPlayingIndex));
//        }
    }

    /**
     * Plays (resumes) currently playing song.
     */
    public void play() {
        if (mediaPlayer != null && !isPlaying()) {
            mediaPlayer.play();
        }
    }

    /**
     * Pauses currently playing song.
     */
    public void pause() {
        if (isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void seek(int seconds) {
        if (mediaPlayer == null)
            return;

        mediaPlayer.seek(Duration.seconds(seconds));
        timerCounter = seconds * 4;

        updateTimeLabels();
    }

    private void updateTimeLabels() {
        timePassedLabel.setText(getTimePassed());
        timeRemainingLabel.setText(getTimeRemaining());
    }

    private String getTimePassed() {
        int secondsPassed = timerCounter / 4;
        int minutes = secondsPassed / 60;
        int seconds = secondsPassed % 60;
        return minutes + ":" + (seconds < 10 ? "0" + seconds : Integer.toString(seconds));
    }

    private String getTimeRemaining() {
        int secondsPassed = timerCounter / 4;
        int totalSeconds = song.getLengthInSeconds();
        int secondsRemaining = totalSeconds - secondsPassed;
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        return minutes + ":" + (seconds < 10 ? "0" + seconds : Integer.toString(seconds));
    }

    private class TimeUpdater implements Runnable {

        @Override
        public void run() {
            if (!isPlaying())
                return;

            int length = song.getLengthInSeconds() * 4;

            Platform.runLater(() -> {
                if (timerCounter < length) {
                    timerCounter++;

                    if (timerCounter % 4 == 0) {
                        updateTimeLabels();
                    }

                    // called every tick (250 ms) because max value is length in seconds * 4
                    if (!timeSlider.isPressed()) {
                        timeSlider.setValue(timerCounter);
                    }
                }
            });
        }
    }

    @FXML
    private void navigateToCurrentSong() {
        // TODO:
    }

    public void onExit() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        executorService.shutdownNow();
    }
}
