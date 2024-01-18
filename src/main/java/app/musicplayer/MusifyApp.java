/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer;

import app.musicplayer.controllers.MainController;
import app.musicplayer.controllers.SplashScreenController;
import app.musicplayer.model.Library;
import app.musicplayer.model.Song;
import app.musicplayer.model.serializable.Serializer;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.logging.ConsoleOutput;
import com.almasb.fxgl.logging.Logger;
import com.almasb.fxgl.logging.LoggerLevel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.DataFormat;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import static app.musicplayer.Config.*;

// TODO: adding new songs after app closed
// TODO: adding new songs when app is running (add rescan button?)
// TODO: serialization version for future updates
// TODO: most played and recently played are not serialized back as instances of classes
// TODO: volume is not serialized
// TODO: light and dark themes
// TODO: on startup "Your Library" is not focused
// TODO: rename playlist option
// TODO: remember stage width height
// TODO: remember last played song
// TODO: update to high res app icon
// TODO: update project title as Musify is taken
// TODO: consider free streaming music API online
// TODO: playlists panel scroll bar apply css
// TODO: playlists + panel should be outside of the scroll bar
// TODO: remove songs from library
// TODO: if song is removed from folder
// TODO: check shuffle and loop
// TODO: fix song table view selection when moving from a different playlist, that song is selected in the new one
public class MusifyApp extends Application {

    private static final Logger log = Logger.get(MusifyApp.class);

    private static MainController mainController;
    private static MediaPlayer mediaPlayer;
    private static List<Song> nowPlayingList;
    private static int nowPlayingIndex;
    private static Song nowPlaying;
    private static int timerCounter = 0;
    private static int secondsPlayed = 0;
    private static boolean isLoopActive = false;
    private static boolean isShuffleActive = false;
    private static boolean isMuted = false;
    private static List<Song> draggedItems = new ArrayList<>();
    private static ScheduledExecutorService executorService;

    private static Library library;

    private static Stage stage;

    public static class Launcher {
        public static void main(String[] args) {
            Application.launch(MusifyApp.class);
        }
    }

    private static class GameApp extends GameApplication {

        @Override
        protected void initSettings(GameSettings settings) {

        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            // disable java.util.logging.Logger from jaudiotagger lib
            LogManager.getLogManager().reset();

            //Logger.addOutput(new ConsoleOutput(), LoggerLevel.DEBUG);
            log.info("start(Stage)");

            // TODO: fully headless start if no game app needed?
            GameApplication.embeddedLaunch(new GameApp());

            executorService = Executors.newScheduledThreadPool(4);
            executorService.scheduleAtFixedRate(new TimeUpdater(), 0, 250, TimeUnit.MILLISECONDS);

            MusifyApp.stage = stage;
            MusifyApp.stage.setMinWidth(850);
            MusifyApp.stage.setMinHeight(600);
            MusifyApp.stage.setTitle("Musify " + VERSION);
            MusifyApp.stage.getIcons().add(new Image(this.getClass().getResource(IMG + "Logo.png").toString()));
            MusifyApp.stage.setOnCloseRequest(event -> {

                try {
                    if (library != null)
                        Serializer.writeToFile(library, LIBRARY_FILE);

                    executorService.shutdownNow();

                    FXGL.getGameController().exit();
                } catch (Exception e) {
                    log.warning("Error during exit", e);
                }
            });

            if (Files.exists(LIBRARY_FILE)) {
                showSplashScreenView(stage);
            } else {
                library = new Library();
                showMainView(stage);
            }

            stage.show();

        } catch (Exception e) {
            log.fatal("Cannot start Musify", e);
            System.exit(0);
        }
    }

    private void showSplashScreenView(Stage stage) throws Exception {
        // TODO: view and css loading
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource(FXML + "scenes/SplashScreenScene.fxml"));
        Parent view = loader.load();

        SplashScreenController controller = loader.getController();

        var task = new DeserializeLibraryTask(LIBRARY_FILE);

        controller.getProgressLabel().textProperty().bind(task.messageProperty());
        controller.getProgressBar().progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            library = task.getValue();

            log.info("Loaded: " + LIBRARY_FILE);
            log.info("Playlists: " + library.getPlaylists());

            try {
                showMainView(stage);
            } catch (Exception ex) {
                log.fatal("Cannot open main view", ex);
            }
        });

        executorService.submit(task);

        Scene scene = new Scene(view);
        scene.getStylesheets().add(getClass().getResource(CSS + "Global.css").toExternalForm());
        stage.setScene(scene);
    }

    private void showMainView(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource(FXML + "scenes/MainScene.fxml"));
        Parent view = loader.load();

        mainController = loader.getController();

        Scene scene = new Scene(view);
        scene.getStylesheets().add(getClass().getResource(CSS + "Global.css").toExternalForm());
        stage.setScene(scene);
    }

    private static class TimeUpdater implements Runnable {

        @Override
        public void run() {
            if (!isPlaying())
                return;

            int length = getNowPlaying().getLengthInSeconds() * 4;

            Platform.runLater(() -> {
                if (timerCounter < length) {
                    if (++timerCounter % 4 == 0) {
                        mainController.updateTimeLabels();
                        secondsPlayed++;
                    }

                    // called every tick (250 ms) because in main controller max value is length in seconds * 4
                    mainController.updateTimeSlider();
                }
            });
        }
    }

    /**
     * Plays selected song.
     */
    public static void play() {
        if (mediaPlayer != null && !isPlaying()) {
            mediaPlayer.play();
            mainController.updatePlayPauseIcon(true);
        }
    }

    /**
     * Checks if a song is playing.
     */
    public static boolean isPlaying() {
        return mediaPlayer != null && MediaPlayer.Status.PLAYING.equals(mediaPlayer.getStatus());
    }

    /**
     * Pauses selected song.
     */
    public static void pause() {
        if (isPlaying()) {
            mediaPlayer.pause();
            mainController.updatePlayPauseIcon(false);
        }
    }

    public static void seek(int seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(new Duration(seconds * 1000));
            timerCounter = seconds * 4;
            mainController.updateTimeLabels();
        }
    }

    /**
     * Skips song.
     */
    public static void skip() {
        if (nowPlayingIndex < nowPlayingList.size() - 1) {
            boolean isPlaying = isPlaying();
            mainController.updatePlayPauseIcon(isPlaying);
            setNowPlaying(nowPlayingList.get(nowPlayingIndex + 1));
            if (isPlaying) {
                play();
            }
        } else if (isLoopActive) {
            boolean isPlaying = isPlaying();
            mainController.updatePlayPauseIcon(isPlaying);
            nowPlayingIndex = 0;
            setNowPlaying(nowPlayingList.get(nowPlayingIndex));
            if (isPlaying) {
                play();
            }
        } else {
            mainController.updatePlayPauseIcon(false);
            nowPlayingIndex = 0;
            setNowPlaying(nowPlayingList.get(nowPlayingIndex));
        }
    }

    public static void back() {
        if (timerCounter > 20 || nowPlayingIndex == 0) {
            mainController.initializeTimeSlider();
            seek(0);
        } else {
            boolean isPlaying = isPlaying();
            setNowPlaying(nowPlayingList.get(nowPlayingIndex - 1));
            if (isPlaying) {
                play();
            }
        }
    }

    public static void mute(boolean isMuted) {
        MusifyApp.isMuted = !isMuted;
        if (mediaPlayer != null) {
            mediaPlayer.setMute(!isMuted);
        }
    }

    public static void toggleLoop() {
        isLoopActive = !isLoopActive;
    }

    public static boolean isLoopActive() {
        return isLoopActive;
    }

    public static void toggleShuffle() {
        isShuffleActive = !isShuffleActive;

        if (isShuffleActive) {
            Collections.shuffle(nowPlayingList);
        } else {
            Collections.sort(nowPlayingList);
        }

        nowPlayingIndex = nowPlayingList.indexOf(nowPlaying);
    }

    public static boolean isShuffleActive() {
        return isShuffleActive;
    }

    public static Stage getStage() {
        return stage;
    }

    public static MainController getMainController() {
        return mainController;
    }

    /**
     * Gets currently playing song list.
     * @return arraylist of now playing songs
     */
    public static ArrayList<Song> getNowPlayingList() {
        return nowPlayingList == null ? new ArrayList<>() : new ArrayList<>(nowPlayingList);
    }

    public static void addSongToNowPlayingList(Song song) {
        if (!nowPlayingList.contains(song)) {
            nowPlayingList.add(song);
        }
    }

    public static void setNowPlayingList(List<Song> list) {
        nowPlayingList = new ArrayList<>(list);
    }

    public static void setNowPlaying(Song song) {
        if (nowPlayingList.contains(song)) {

            updatePlayCount();
            nowPlayingIndex = nowPlayingList.indexOf(song);
            if (nowPlaying != null) {
                nowPlaying.setPlaying(false);
            }
            nowPlaying = song;
            nowPlaying.setPlaying(true);
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            timerCounter = 0;
            secondsPlayed = 0;

            Media media = new Media(song.getFile().toUri().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.volumeProperty().bind(mainController.volumeProperty().divide(200));
            mediaPlayer.setOnEndOfMedia(MusifyApp::skip);
            mediaPlayer.setMute(isMuted);
            mainController.updateNowPlayingButton();
            mainController.initializeTimeSlider();
            mainController.initializeTimeLabels();
        }
    }

    private static void updatePlayCount() {
        if (nowPlaying != null) {
            int length = nowPlaying.getLengthInSeconds();
            if ((100 * secondsPlayed / length) > 50) {
                songPlayed(nowPlaying);
            }
        }
    }

    private static void songPlayed(Song song) {
        song.playCountProperty().set(song.playCountProperty().get() + 1);
        song.setPlayDate(LocalDateTime.now());
    }

    public static Song getNowPlaying() {
        return nowPlaying;
    }

    public static String getTimePassed() {
        int secondsPassed = timerCounter / 4;
        int minutes = secondsPassed / 60;
        int seconds = secondsPassed % 60;
        return minutes + ":" + (seconds < 10 ? "0" + seconds : Integer.toString(seconds));
    }

    public static String getTimeRemaining() {
        int secondsPassed = timerCounter / 4;
        int totalSeconds = getNowPlaying().getLengthInSeconds();
        int secondsRemaining = totalSeconds - secondsPassed;
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        return minutes + ":" + (seconds < 10 ? "0" + seconds : Integer.toString(seconds));
    }

    public static void setDraggedItems(List<Song> draggedItems) {
        MusifyApp.draggedItems = draggedItems;
    }

    public static List<Song> getDraggedItems() {
        return draggedItems;
    }

    public static Library getLibrary() {
        return library;
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    private static class DeserializeLibraryTask extends Task<Library> {

        private final Path file;

        private int songIndex = 0;
        private int playlistIndex = 0;

        private DeserializeLibraryTask(Path file) {
            this.file = file;
        }

        @Override
        protected Library call() throws Exception {
            updateMessage("Loading library");

            var library = Serializer.readFromFile(file);

            updateMessage("Loading songs");

            var numSongs = library.songs().size();
            var numPlaylists = library.playlists().size();

            var songs = library.songs()
                    .stream()
                    .map(song -> {
                        updateProgress(songIndex++, numSongs);

                        return Serializer.fromSerializable(song);
                    })
                    .toList();

            updateMessage("Loading playlists");

            var playlists = library.playlists()
                    .stream()
                    .map(p -> {
                        updateProgress(playlistIndex++, numPlaylists);

                        var playlist = Serializer.fromSerializable(p);

                        p.songIDs().forEach(id -> {
                            songs.stream()
                                    .filter(s -> s.getId() == id)
                                    .findAny()
                                    .ifPresent(playlist::addSong);
                        });

                        return playlist;
                    })
                    .toList();

            return new Library(
                    playlists
            );
        }
    }
}
