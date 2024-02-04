/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.controllers;

import app.musicplayer.events.UserDataEvent;
import app.musicplayer.model.Playlist;
import app.musicplayer.model.Song;
import com.almasb.fxgl.animation.Animation;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static app.musicplayer.Config.PREFERENCES;
import static app.musicplayer.events.UserDataEvent.*;
import static com.almasb.fxgl.dsl.FXGL.*;

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
    private Text nowPlayingTitle;

    @FXML
    private Slider timeSlider;
    @FXML
    private Region frontSliderTrack;

    @FXML
    private Label timePassedLabel;
    @FXML
    private Label timeRemainingLabel;

    @FXML
    private Pane shuffleButton;

    private ScheduledExecutorService executorService;

    private boolean isReady = false;

    private BooleanProperty isPlaying = new SimpleBooleanProperty(false);
    private BooleanProperty isShuffleOn = new SimpleBooleanProperty(false);

    private MediaPlayer mediaPlayer = null;
    private Playlist playlist = null;
    private Song song = null;
    private int currentSongIndex = 0;

    private ChangeListener<Duration> timeChangeListener = (o, oldTime, currentTime) -> {
        if (mediaPlayer != null) {
            updateTimeLabels(currentTime, mediaPlayer.getMedia().getDuration());
        }
    };

    private List<Animation<?>> animations = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTimeSlider();

        isShuffleOn.addListener((o, wasActive, isActive) -> {
            shuffleButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), isActive);
        });

        nowPlayingArtwork.imageProperty().addListener((o, oldImage, newImage) -> {
            if (newImage == null) {
                fire(new UserDataEvent<>(LOAD_SONG_ARTWORK, song));
            }
        });

        volumePaneController.volumeProperty().bindBidirectional(PREFERENCES.doubleProperty("volume"));

        executorService = Executors.newScheduledThreadPool(4);
        executorService.scheduleAtFixedRate(() -> tick(), 0, 16, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        Platform.runLater(() -> {
            animations.forEach(a -> a.onUpdate(0.016));
        });
    }

    private void startNewPlayingTitleAnimation() {
        animations.forEach(Animation::stop);
        animations.clear();

        nowPlayingTitle.setClip(null);

        // TODO: what if running first time, no calculation of width?
        var width = nowPlayingTitle.getLayoutBounds().getWidth();

        if (width > 190) {
            // TODO: these values are calculated from MediaPane.fxml
            var clip = new Rectangle(190, 100);
            nowPlayingTitle.setClip(clip);

            var dist = width - 190;

            var anim1 = animationBuilder()
                    .duration(Duration.seconds(5))
                    .repeatInfinitely()
                    .autoReverse(true)
                    .translate(clip)
                    .from(new Point2D(0, -50))
                    .to(new Point2D(dist, -50))
                    .build();

            var anim2 = animationBuilder()
                    .duration(Duration.seconds(5))
                    .repeatInfinitely()
                    .autoReverse(true)
                    .translate(nowPlayingTitle)
                    .from(new Point2D(0, 0))
                    .to(new Point2D(-dist, 0))
                    .build();

            animations.add(anim1);
            animations.add(anim2);

            animations.forEach(Animation::start);
        }
    }

    private void initTimeSlider() {
        timeSlider.setOnMouseReleased(e -> {
            int seconds = (int) Math.round(timeSlider.getValue() / 4.0);
            seek(seconds);
        });

        frontSliderTrack.prefWidthProperty().bind(timeSlider.widthProperty().multiply(timeSlider.valueProperty().divide(timeSlider.maxProperty())));
    }

    /**
     * Plays the given song from the given playlist.
     */
    public void play(Playlist playlist, Song song) {
        setSong(playlist, song);
        play();
    }

    private void setSong(Playlist playlist, Song song) {
        if (this.song != null) {
            var prevSong = this.song;
            prevSong.setPlaying(false);

            releaseMediaPlayer();
        }

        this.playlist = playlist;
        this.song = song;
        currentSongIndex = playlist.getSongs().indexOf(song);
        isReady = true;

        nowPlayingTitle.textProperty().bind(song.titleProperty());
        nowPlayingArtwork.imageProperty().bind(song.artworkProperty());

        startNewPlayingTitleAnimation();

        timeSlider.setMax(song.getLengthInSeconds() * 4);

        Media media = new Media(song.getFile().toUri().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.volumeProperty().bind(volumePaneController.volumeProperty().divide(200));
        mediaPlayer.muteProperty().bind(volumePaneController.mutedProperty());
        mediaPlayer.setOnEndOfMedia(this::next);
        mediaPlayer.currentTimeProperty().addListener(timeChangeListener);

        isPlaying.bind(mediaPlayer.statusProperty().isEqualTo(MediaPlayer.Status.PLAYING));
    }

    @FXML
    private void prev() {
        if (!isReady)
            return;

        if (currentSongIndex == 0) {
            seek(0);
            return;
        }

        boolean wasPlaying = isPlaying();
        setSong(playlist, playlist.getSongs().get(currentSongIndex - 1));

        if (wasPlaying)
            play();
    }

    /**
     * Moves to the next song in the playlist based on control pane criteria (e.g. shuffle, loop).
     */
    @FXML
    private void next() {
        if (!isReady)
            return;

        boolean wasPlaying = isPlaying();

        setNextSong();

        if (wasPlaying)
            play();
    }

    private void setNextSong() {
        // TODO: set via UI buttons
        boolean isLoop1On = false;
        boolean isLoopAllOn = false;

        if (isLoop1On) {
            setSong(playlist, song);
            return;
        }

        // we reached the playlist end, if loop all is on, then play first song
        if (currentSongIndex == playlist.getSongs().size() - 1 && isLoopAllOn) {
            setSong(playlist, playlist.getSongs().get(0));
            return;
        }

        // we know there are still songs in the playlist, then play the next
        setSong(playlist, playlist.getSongs().get(currentSongIndex + 1));
    }

    /**
     * Plays (resumes) currently playing song.
     */
    @FXML
    private void play() {
        if (!isReady)
            return;

        if (mediaPlayer != null && !isPlaying()) {
            mediaPlayer.play();
            song.setPlaying(true);
            song.setPlayCount(song.getPlayCount() + 1);
            song.setPlayDate(LocalDateTime.now());
        }
    }

    /**
     * Pauses currently playing song.
     */
    @FXML
    private void pause() {
        if (!isReady)
            return;

        if (isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @FXML
    private void toggleShuffle() {
        if (!isReady)
            return;

        isShuffleOn.set(!isShuffleOn());

        if (isShuffleOn()) {
            playlist.shuffle();
        } else {
            playlist.restoreFromShuffle();
        }

        currentSongIndex = playlist.getSongs().indexOf(song);
    }

    @FXML
    private void toggleLoop() {
        if (!isReady)
            return;

        System.out.println("TODO: toggle loop");
    }

    @FXML
    private void navigateToCurrentSong() {
        if (!isReady)
            return;

        fire(new UserDataEvent<>(NAGIVATE_TO_SONG, new PlaylistAndSong(playlist, song)));
    }

    private void seek(int seconds) {
        if (mediaPlayer == null)
            return;

        mediaPlayer.seek(Duration.seconds(seconds));
    }

    private void updateTimeLabels(Duration currentTime, Duration songTime) {
        if (!timeSlider.isPressed()) {
            timeSlider.setValue(currentTime.toSeconds() * 4);
        }

        timePassedLabel.setText(format((int) currentTime.toSeconds()));
        timeRemainingLabel.setText(format((int) songTime.toSeconds() - (int) currentTime.toSeconds()));
    }

    private String format(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;

        return min + ":" + (sec < 10 ? "0" : "") + sec;
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

    public BooleanProperty shuffleOnProperty() {
        return isShuffleOn;
    }

    public boolean isShuffleOn() {
        return isShuffleOn.get();
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer == null)
            return;

        mediaPlayer.currentTimeProperty().removeListener(timeChangeListener);
        mediaPlayer.volumeProperty().unbind();
        mediaPlayer.muteProperty().unbind();
        mediaPlayer.setOnEndOfMedia(null);
        mediaPlayer.stop();
        mediaPlayer.dispose();
    }

    public void onExit() {
        releaseMediaPlayer();

        executorService.shutdownNow();
    }
}
