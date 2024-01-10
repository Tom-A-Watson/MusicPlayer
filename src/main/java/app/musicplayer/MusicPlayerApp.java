package app.musicplayer;

import app.musicplayer.model.Album;
import app.musicplayer.model.Artist;
import app.musicplayer.model.Library;
import app.musicplayer.model.Song;
import app.musicplayer.util.Resources;
import app.musicplayer.util.XMLEditor;
import app.musicplayer.view.ImportMusicDialogController;
import app.musicplayer.view.MainController;
import app.musicplayer.view.NowPlayingController;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

public class MusicPlayerApp extends Application {

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

    // Stores the number of files in library.xml.
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
            Logger.addOutput(new ConsoleOutput(), LoggerLevel.DEBUG);
            Logger.get(MusicPlayerApp.class).info("start(Stage)");

            timer = new Timer();
            timerCounter = 0;
            secondsPlayed = 0;

            MusicPlayerApp.stage = stage;
            MusicPlayerApp.stage.setMinWidth(650);
            MusicPlayerApp.stage.setMinHeight(480);
            MusicPlayerApp.stage.setTitle("Music Player");
            MusicPlayerApp.stage.getIcons().add(new Image(this.getClass().getResource(Resources.IMG + "Icon.png").toString()));
            MusicPlayerApp.stage.setOnCloseRequest(event -> {
                Platform.exit();

                // TODO: find alive threads
                System.exit(0);
            });

            // TODO: splash screen animation while loading?

            // Load main layout from fxml file.
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource(Resources.FXML + "SplashScreen.fxml"));
            Parent view = loader.load();

            // Shows the scene containing the layout.
            Scene scene = new Scene(view);
            stage.setScene(scene);
            stage.show();

            // Calls the function to check if the library.xml file exists. If it does not, the file is created.
            var isLibPresent = checkLibraryXML();

            // if lib is present, init now, otherwise wait until the user imports the music dir
            if (isLibPresent) {
                initInBackground();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
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

    /**
     * @return true if exists, false otherwise
     */
    private boolean checkLibraryXML() {
        // Finds the jar file and the path of its parent folder.
        File musicPlayerJAR = null;
        try {
            musicPlayerJAR = new File(MusicPlayerApp.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String jarFilePath = musicPlayerJAR.getParentFile().getPath();

        // Assigns the filepath to the XML filepath set in Resources.java
        Resources.JAR = jarFilePath + "/";

        // Specifies library.xml file and its location.
        File libraryXML = new File(Resources.JAR + "library.xml");

        // If the file exists, check if the music directory has changed.
        if (libraryXML.exists()) {
            // Gets music directory path from xml file so that the number of files in the
            // music directory can be counted and compared to the data in the xml file.
            // It is then passed as an argument when creating the directory watch.
            Path musicDirectory = xmlMusicDirPathFinder();

            // Try/catch block to deal with case where music directory has been renamed.
            try {
                // Gets the number of files in the music directory and the number of files saved in the xml file.
                // These values will be compared to determine if the xml file needs to be updated.
                int musicDirFileNum = musicDirFileNumFinder(musicDirectory.toFile(), 0);
                xmlFileNum = xmlMusicDirFileNumFinder();

                // If the number of files stored in the xml file is not the same as the number of files in the music directory.
                // Music library has changed; update the xml file.
                if (musicDirFileNum != xmlFileNum) {
                    // Updates the xml file from the saved music directory.
                    updateLibraryXML(musicDirectory);
                }

                return true;
                // NullPointerException thrown by musicDirFileNumFinder().
                // It occurs if the music directory has been renamed
            } catch (NullPointerException npe) {
                createLibraryXML();

                return false;
            }

            // If the library.xml file does not exist, the file is created from the user specified music library location.
        } else {
            createLibraryXML();
            return false;
        }
    }

    private static Path xmlMusicDirPathFinder() {
        try {
            // Creates reader for xml file.
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty("javax.xml.stream.isCoalescing", true);
            FileInputStream is = new FileInputStream(new File(Resources.JAR + "library.xml"));
            XMLStreamReader reader = factory.createXMLStreamReader(is, "UTF-8");

            String element = null;
            String path = null;

            // Loops through xml file looking for the music directory file path.
            while(reader.hasNext()) {
                reader.next();
                if (reader.isWhiteSpace()) {
                    continue;
                } else if (reader.isStartElement()) {
                    element = reader.getName().getLocalPart();
                } else if (reader.isCharacters() && element.equals("path")) {
                    path = reader.getText();
                    break;
                }
            }
            // Closes xml reader.
            reader.close();

            return Paths.get(path);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int xmlMusicDirFileNumFinder() {
        try {
            // Creates reader for xml file.
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty("javax.xml.stream.isCoalescing", true);
            FileInputStream is = new FileInputStream(new File(Resources.JAR + "library.xml"));
            XMLStreamReader reader = factory.createXMLStreamReader(is, "UTF-8");

            String element = null;
            String fileNum = null;

            // Loops through xml file looking for the music directory file path.
            while(reader.hasNext()) {
                reader.next();
                if (reader.isWhiteSpace()) {
                    continue;
                } else if (reader.isStartElement()) {
                    element = reader.getName().getLocalPart();
                } else if (reader.isCharacters() && element.equals("fileNum")) {
                    fileNum = reader.getText();
                    break;
                }
            }
            // Closes xml reader.
            reader.close();

            // Converts the file number to an int and returns the value.
            return Integer.parseInt(fileNum);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int musicDirFileNumFinder(File musicDirectory, int i) {
        // Lists all the files in the music directory and stores them in an array.
        File[] files = musicDirectory.listFiles();

        // Loops through the files, increments counter if file is found.
        for (File file : files) {
            if (file.isFile() && Library.isSupportedFileType(file.getName())) {
                i++;
            } else if (file.isDirectory()) {
                i = musicDirFileNumFinder(file, i);
            }
        }
        return i;
    }

    private static void updateLibraryXML(Path musicDirectory) {
        // Sets the music directory for the XMLEditor.
        XMLEditor.setMusicDirectory(musicDirectory);

        // Checks if songs have to be added, deleted, or both to the xml file and
        // performs the corresponding operation.
        XMLEditor.addDeleteChecker();
    }

    // TODO: rename, e.g. show import dialog
    private void createLibraryXML() {
        try {
            FXMLLoader loader = new FXMLLoader(MusicPlayerApp.class.getResource(Resources.FXML + "ImportMusicDialog.fxml"));
            Parent view = loader.load();

            VBox root = (VBox) stage.getScene().getRoot();
            root.getChildren().add(view);

            // Set the dialog into the controller.
            ImportMusicDialogController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setOnFinished(() -> {
                // Gets the number of files saved in the xml file.
                xmlFileNum = xmlMusicDirFileNumFinder();

                initInBackground();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the main layout.
     */
    private void initMain() {
        try {
            // Load main layout from fxml file.
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource(Resources.FXML + "Main.fxml"));
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
            Library.savePlayingList();
        }
    }

    public static void setNowPlayingList(List<Song> list) {
        nowPlayingList = new ArrayList<>(list);
        Library.savePlayingList();
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

        Thread thread = new Thread(() -> {

            try {

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(Resources.JAR + "library.xml");

                XPathFactory xPathfactory = XPathFactory.newInstance();
                XPath xpath = xPathfactory.newXPath();

                XPathExpression expr = xpath.compile("/library/songs/song/playCount[../id/text() = \"" + song.getId() + "\"]");
                Node playCount = ((NodeList) expr.evaluate(doc, XPathConstants.NODESET)).item(0);

                expr = xpath.compile("/library/songs/song/playDate[../id/text() = \"" + song.getId() + "\"]");
                Node playDate = ((NodeList) expr.evaluate(doc, XPathConstants.NODESET)).item(0);

                playCount.setTextContent(Integer.toString(song.getPlayCount()));
                playDate.setTextContent(song.getPlayDate().toString());

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                DOMSource source = new DOMSource(doc);
                File xmlFile = new File(Resources.JAR + "library.xml");
                StreamResult result = new StreamResult(xmlFile);
                transformer.transform(source, result);

            } catch (Exception ex) {

                ex.printStackTrace();
            }

        });

        thread.start();
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
