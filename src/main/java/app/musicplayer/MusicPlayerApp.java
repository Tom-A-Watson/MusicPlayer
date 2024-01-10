package app.musicplayer;

import app.musicplayer.controllers.ImportMusicController;
import app.musicplayer.controllers.MainController;
import app.musicplayer.controllers.NowPlayingController;
import app.musicplayer.model.Album;
import app.musicplayer.model.Artist;
import app.musicplayer.model.Library;
import app.musicplayer.model.Song;
import app.musicplayer.util.Config;
import com.almasb.fxgl.logging.ConsoleOutput;
import com.almasb.fxgl.logging.Logger;
import com.almasb.fxgl.logging.LoggerLevel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.LogManager;

public class MusicPlayerApp extends Application {

    private static final Logger log = Logger.get(MusicPlayerApp.class);

    private static MainController mainController;
    private static MediaPlayer mediaPlayer;
    private static ArrayList<Song> nowPlayingList;
    private static int nowPlayingIndex;
    private static Song nowPlaying;
    private static Timer timer;
    private static int timerCounter;
    private static int secondsPlayed;
    private static boolean isLoopActive = false;
    private static boolean isShuffleActive = false;
    private static boolean isMuted = false;
    private static Object draggedItem;

    private static Stage stage;

    // Stores the number of files in library
    // This will then be compared to the number of files in the music directory when starting up the application to
    // determine if the xml file needs to be updated by adding or deleting songs.
    private static int xmlFileNum;

    // Stores the last id that was assigned to a song.
    // This is important when adding new songs after others have been deleted because the last id assigned
    // may not necessarily be equal to the number of songs in the xml file if songs have been deleted.
    private static int lastIdAssigned;

    public static class Launcher {
        public static void main(String[] args) {
            Application.launch(MusicPlayerApp.class);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            // disable java.util.logging.Logger from jaudiotagger lib
            LogManager.getLogManager().reset();

            Logger.addOutput(new ConsoleOutput(), LoggerLevel.DEBUG);
            log.info("start(Stage)");

            timer = new Timer();
            timerCounter = 0;
            secondsPlayed = 0;

            MusicPlayerApp.stage = stage;
            MusicPlayerApp.stage.setMinWidth(650);
            MusicPlayerApp.stage.setMinHeight(480);
            MusicPlayerApp.stage.setTitle("Music Player");
            MusicPlayerApp.stage.getIcons().add(new Image(this.getClass().getResource(Config.IMG + "Icon.png").toString()));
            MusicPlayerApp.stage.setOnCloseRequest(event -> {
                Platform.exit();

                // TODO: find alive threads
                System.exit(0);
            });

            // TODO: splash screen animation while loading?

            // Load main layout from fxml file.
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource(Config.FXML + "SplashScreen.fxml"));
            Parent view = loader.load();

            // Shows the scene containing the layout.
            Scene scene = new Scene(view);
            stage.setScene(scene);
            stage.show();

            if (Files.exists(Config.LIBRARY_FILE)) {
                initInBackground();
            } else {
                showImportMusicView();
            }

        } catch (Exception e) {
            log.fatal("Cannot start MusicPlayer", e);
            System.exit(0);
        }
    }

    private void initInBackground() {
        Thread thread = new Thread(() -> {
            // Retrieves song, album, artist, and playlist data from library.
            Library.getSongs();
            Library.getAlbums();
            Library.getArtists();
            Library.getPlaylists();

            nowPlayingList = Library.loadPlayingList();

            if (nowPlayingList.isEmpty()) {

                Artist artist = Library.getArtists().get(0);

                for (Album album : artist.albums()) {
                    nowPlayingList.addAll(album.songs());
                }

                Collections.sort(nowPlayingList, (first, second) -> {
                    Album firstAlbum = Library.getAlbum(first.getAlbum());
                    Album secondAlbum = Library.getAlbum(second.getAlbum());
                    if (firstAlbum.compareTo(secondAlbum) != 0) {
                        return firstAlbum.compareTo(secondAlbum);
                    } else {
                        return first.compareTo(second);
                    }
                });
            }

            nowPlaying = nowPlayingList.get(0);
            nowPlayingIndex = 0;
            nowPlaying.setPlaying(true);
            timer = new Timer();
            timerCounter = 0;
            secondsPlayed = 0;
            String path = nowPlaying.getLocation();
            Media media = new Media(Paths.get(path).toUri().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(0.5);
            mediaPlayer.setOnEndOfMedia(new SongSkipper());

            // Calls the function to initialize the main layout.
            Platform.runLater(this::initMain);
        });

        thread.start();
    }

    private void showImportMusicView() throws Exception {
        FXMLLoader loader = new FXMLLoader(MusicPlayerApp.class.getResource(Config.FXML + "ImportMusicDialog.fxml"));
        Parent view = loader.load();

        ImportMusicController controller = loader.getController();
        controller.setOwnerStage(stage);
        controller.setOnFinished(() -> {
            initInBackground();
        });

        VBox root = (VBox) stage.getScene().getRoot();
        root.getChildren().add(view);
    }

    /**
     * Initializes the main layout.
     */
    private void initMain() {
        try {
            // Load main layout from fxml file.
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource(Config.FXML + "Main.fxml"));
            BorderPane view = loader.load();

            // Shows the scene containing the layout.
            double width = stage.getScene().getWidth();
            double height = stage.getScene().getHeight();

            view.setPrefWidth(width);
            view.setPrefHeight(height);

            Scene scene = new Scene(view);
            stage.setScene(scene);

            // Gives the controller access to the music player main application.
            mainController = loader.getController();
            mediaPlayer.volumeProperty().bind(mainController.getVolumeSlider().valueProperty().divide(200));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class SongSkipper implements Runnable {
        @Override
        public void run() {
            skip();
        }
    }

    private static class TimeUpdater extends TimerTask {
        private int length = (int) getNowPlaying().getLengthInSeconds() * 4;

        @Override
        public void run() {
            Platform.runLater(() -> {
                if (timerCounter < length) {
                    if (++timerCounter % 4 == 0) {
                        mainController.updateTimeLabels();
                        secondsPlayed++;
                    }
                    if (!mainController.isTimeSliderPressed()) {
                        mainController.updateTimeSlider();
                    }
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
            timer.scheduleAtFixedRate(new TimeUpdater(), 0, 250);
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
            timer.cancel();
            timer = new Timer();
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
        MusicPlayerApp.isMuted = !isMuted;
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
            Collections.sort(nowPlayingList, (first, second) -> {
                int result = Library.getAlbum(first.getAlbum()).compareTo(Library.getAlbum(second.getAlbum()));
                if (result != 0) {
                    return result;
                }
                result = Library.getAlbum(first.getAlbum()).compareTo(Library.getAlbum(second.getAlbum()));
                if (result != 0) {
                    return result;
                }
                result = first.compareTo(second);
                return result;
            });
        }

        nowPlayingIndex = nowPlayingList.indexOf(nowPlaying);

        if (mainController.getSubViewController() instanceof NowPlayingController) {
            mainController.loadView("nowPlaying");
        }
    }

    public static boolean isShuffleActive() {
        return isShuffleActive;
    }

    public static Stage getStage() {
        return stage;
    }

    /**
     * Gets main controller object.
     * @return MainController
     */
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
            //Library.savePlayingList();
        }
    }

    public static void setNowPlayingList(List<Song> list) {
        nowPlayingList = new ArrayList<>(list);
        //Library.savePlayingList();
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
            }
            if (timer != null) {
                timer.cancel();
            }
            timer = new Timer();
            timerCounter = 0;
            secondsPlayed = 0;
            String path = song.getLocation();
            Media media = new Media(Paths.get(path).toUri().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.volumeProperty().bind(mainController.getVolumeSlider().valueProperty().divide(200));
            mediaPlayer.setOnEndOfMedia(new SongSkipper());
            mediaPlayer.setMute(isMuted);
            mainController.updateNowPlayingButton();
            mainController.initializeTimeSlider();
            mainController.initializeTimeLabels();
        }
    }

    private static void updatePlayCount() {
        if (nowPlaying != null) {
            int length = (int) nowPlaying.getLengthInSeconds();
            if ((100 * secondsPlayed / length) > 50) {
                songPlayed(nowPlaying);
            }
        }
    }

    private static void songPlayed(Song song) {
        song.playCountProperty().set(song.playCountProperty().get() + 1);
        song.playDate = LocalDateTime.now();

        // TODO:
//
//        Thread thread = new Thread(() -> {
//
//            try {
//
//                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
//                Document doc = docBuilder.parse(Resources.JAR + "library.xml");
//
//                XPathFactory xPathfactory = XPathFactory.newInstance();
//                XPath xpath = xPathfactory.newXPath();
//
//                XPathExpression expr = xpath.compile("/library/songs/song/playCount[../id/text() = \"" + song.getId() + "\"]");
//                Node playCount = ((NodeList) expr.evaluate(doc, XPathConstants.NODESET)).item(0);
//
//                expr = xpath.compile("/library/songs/song/playDate[../id/text() = \"" + song.getId() + "\"]");
//                Node playDate = ((NodeList) expr.evaluate(doc, XPathConstants.NODESET)).item(0);
//
//                playCount.setTextContent(Integer.toString(song.getPlayCount()));
//                playDate.setTextContent(song.getPlayDate().toString());
//
//                TransformerFactory transformerFactory = TransformerFactory.newInstance();
//                Transformer transformer = transformerFactory.newTransformer();
//                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
//                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//                DOMSource source = new DOMSource(doc);
//                File xmlFile = new File(Resources.JAR + "library.xml");
//                StreamResult result = new StreamResult(xmlFile);
//                transformer.transform(source, result);
//
//            } catch (Exception ex) {
//
//                ex.printStackTrace();
//            }
//
//        });
//
//        thread.start();
    }

    public static Song getNowPlaying() {
        return nowPlaying;
    }

    public static String getTimePassed() {
        int secondsPassed = timerCounter / 4;
        int minutes = secondsPassed / 60;
        int seconds = secondsPassed % 60;
        return Integer.toString(minutes) + ":" + (seconds < 10 ? "0" + seconds : Integer.toString(seconds));
    }

    public static String getTimeRemaining() {
        long secondsPassed = timerCounter / 4;
        long totalSeconds = getNowPlaying().getLengthInSeconds();
        long secondsRemaining = totalSeconds - secondsPassed;
        long minutes = secondsRemaining / 60;
        long seconds = secondsRemaining % 60;
        return Long.toString(minutes) + ":" + (seconds < 10 ? "0" + seconds : Long.toString(seconds));
    }

    public static void setDraggedItem(Object item) {
        draggedItem = item;
    }

    public static Object getDraggedItem() {
        return draggedItem;
    }

    public static int getXMLFileNum() {
        return xmlFileNum;
    }

    public static void setXMLFileNum(int i) {
        xmlFileNum = i;
    }

    public static int getLastIdAssigned() {
        return lastIdAssigned;
    }

    public static void setLastIdAssigned(int i) {
        lastIdAssigned = i;
    }
}
