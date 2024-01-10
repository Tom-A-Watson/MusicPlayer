package app.musicplayer.model;

import app.musicplayer.util.Config;
import com.almasb.fxgl.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public final class Library {

    private static final Logger log = Logger.get(Library.class);

    private static Path musicDirectory = Paths.get("./");

    private static List<Song> songs = new ArrayList<>();
    private static List<Artist> artists = new ArrayList<>();
    private static List<Album> albums = new ArrayList<>();
    private static List<Playlist> playlists = new ArrayList<>();

    public static Path getMusicDirectory() {
        return musicDirectory;
    }

    public static Task<Boolean> newImportMusicTask(Path directory) {
        return new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                musicDirectory = directory;
                loadSongs(directory.toFile());

                return true;
            }
        };
    }

    private static void loadSongs(File directory) {
        int id = 0;

        File[] files = directory.listFiles();

        for (File file : files) {
            if (file.isFile() && isSupportedFileType(file.getName())) {
                try {
                    AudioFile audioFile = AudioFileIO.read(file);
                    Tag tag = audioFile.getTag();
                    AudioHeader header = audioFile.getAudioHeader();

                    String artistTitle = tag.getFirst(FieldKey.ALBUM_ARTIST);
                    if (artistTitle == null || artistTitle.equals("") || artistTitle.equals("null")) {
                        artistTitle = tag.getFirst(FieldKey.ARTIST);
                    }

                    String albumTitle = tag.getFirst(FieldKey.ALBUM);
                    int lengthSeconds = header.getTrackLength();
                    String track = tag.getFirst(FieldKey.TRACK);
                    int trackNumber = (track == null || track.equals("") || track.equals("null")) ? 0 : Integer.parseInt(track);

                    String disc = tag.getFirst(FieldKey.DISC_NO);
                    int discNumber = (disc == null || disc.equals("") || disc.equals("null")) ? 0 : Integer.parseInt(disc);

                    songs.add(new Song(
                            id++,
                            tag.getFirst(FieldKey.TITLE),
                            artistTitle,
                            albumTitle,
                            Duration.ofSeconds(lengthSeconds),
                            trackNumber,
                            discNumber,
                            0,
                            LocalDateTime.now(),
                            file.getAbsolutePath()
                    ));

                } catch (Exception ex) {

                    ex.printStackTrace();
                }

            } else if (file.isDirectory()) {

                loadSongs(file);
            }
        }
    }

    public static boolean isSupportedFileType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1).toLowerCase();
        }
        switch (extension) {
            // MP3
            case "mp3":
                // MP4
            case "mp4":
            case "m4a":
            case "m4v":
                // WAV
            case "wav":
                return true;
            default:
                return false;
        }
    }

    /**
     * Gets a list of songs.
     * @return observable list of songs
     */
    public static ObservableList<Song> getSongs() {
        // If the observable list of songs has not been initialized.
        if (songs == null) {
            songs = new ArrayList<>();
            // Updates the songs array list.
            updateSongsList();
        }
        return FXCollections.observableArrayList(songs);
    }

    private static Song getSong(int id) {
        if (songs == null) {
            getSongs();
        }
        return songs.get(id);
    }

    public static Song getSong(String title) {
        if (songs == null) {
            getSongs();
        }
        return songs.stream().filter(song -> title.equals(song.getTitle())).findFirst().get();
    }

    private static void updateSongsList() {
        // TODO:
        System.out.println("UPDATING SONGS LIST");

//        try {
//
//            XMLInputFactory factory = XMLInputFactory.newInstance();
//            factory.setProperty("javax.xml.stream.isCoalescing", true);
//            FileInputStream is = new FileInputStream(new File(Resources.JAR + "library.xml"));
//            XMLStreamReader reader = factory.createXMLStreamReader(is, "UTF-8");
//
//            String element = "";
//            int id = -1;
//            String title = null;
//            String artist = null;
//            String album = null;
//            Duration length = null;
//            int trackNumber = -1;
//            int discNumber = -1;
//            int playCount = -1;
//            LocalDateTime playDate = null;
//            String location = null;
//
//            while(reader.hasNext()) {
//                reader.next();
//
//                if (reader.isWhiteSpace()) {
//                    continue;
//                } else if (reader.isStartElement()) {
//                    element = reader.getName().getLocalPart();
//                } else if (reader.isCharacters()) {
//                    String value = reader.getText();
//
//                    switch (element) {
//                        case ID:
//                            id = Integer.parseInt(value);
//                            break;
//                        case TITLE:
//                            title = value;
//                            break;
//                        case ARTIST:
//                            artist = value;
//                            break;
//                        case ALBUM:
//                            album = value;
//                            break;
//                        case LENGTH:
//                            length = Duration.ofSeconds(Long.parseLong(value));
//                            break;
//                        case TRACKNUMBER:
//                            trackNumber = Integer.parseInt(value);
//                            break;
//                        case DISCNUMBER:
//                            discNumber = Integer.parseInt(value);
//                            break;
//                        case PLAYCOUNT:
//                            playCount = Integer.parseInt(value);
//                            break;
//                        case PLAYDATE:
//                            playDate = LocalDateTime.parse(value);
//                            break;
//                        case LOCATION:
//                            location = value;
//                            break;
//                    } // End switch
//                } else if (reader.isEndElement() && reader.getName().getLocalPart().equals("song")) {
//
//                    songs.add(new Song(id, title, artist, album, length, trackNumber, discNumber, playCount, playDate, location));
//                    id = -1;
//                    title = null;
//                    artist = null;
//                    album = null;
//                    length = null;
//                    trackNumber = -1;
//                    discNumber = -1;
//                    playCount = -1;
//                    playDate = null;
//                    location = null;
//
//                } else if (reader.isEndElement() && reader.getName().getLocalPart().equals("songs")) {
//
//                    reader.close();
//                    break;
//                }
//            } // End while
//
//            reader.close();
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }

    /**
     * Gets a list of albums.
     *
     * @return observable list of albums
     */
    public static ObservableList<Album> getAlbums() {
        // If the observable list of albums has not been initialized.
        if (albums == null) {
            if (songs == null) {
                getSongs();
            }
            // Updates the albums array list.
            updateAlbumsList();
        }
        return FXCollections.observableArrayList(albums);
    }

    public static Album getAlbum(String title) {
        if (albums == null) {
            getAlbums();
        }
        return albums.stream().filter(album -> title.equals(album.title())).findFirst().get();
    }

    private static void updateAlbumsList() {
        albums = new ArrayList<>();

        TreeMap<String, List<Song>> albumMap = new TreeMap<>(
                songs.stream()
                        .filter(song -> song.getAlbum() != null)
                        .collect(Collectors.groupingBy(Song::getAlbum))
        );

        int id = 0;

        for (Map.Entry<String, List<Song>> entry : albumMap.entrySet()) {
            ArrayList<Song> songs = new ArrayList<>();

            songs.addAll(entry.getValue());

            TreeMap<String, List<Song>> artistMap = new TreeMap<>(
                    songs.stream()
                            .filter(song -> song.getArtist() != null)
                            .collect(Collectors.groupingBy(Song::getArtist))
            );

            for (Map.Entry<String, List<Song>> e : artistMap.entrySet()) {
                ArrayList<Song> albumSongs = new ArrayList<>();
                String artist = e.getValue().get(0).getArtist();

                albumSongs.addAll(e.getValue());

                albums.add(new Album(id++, entry.getKey(), artist, getArtwork(songs), albumSongs));
            }
        }
    }

    private static Image getArtwork(List<Song> songs) {
        Image artwork = null;

        if (artwork == null) {

            try {
                String location = songs.get(0).getLocation();
                AudioFile audioFile = AudioFileIO.read(new File(location));
                Tag tag = audioFile.getTag();
                byte[] bytes = tag.getFirstArtwork().getBinaryData();
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                artwork = new Image(in, 300, 300, true, true);

                if (artwork.isError()) {
                    artwork = new Image(Config.IMG + "albumsIcon.png");
                }

            } catch (Exception ex) {
                artwork = new Image(Config.IMG + "albumsIcon.png");
            }
        }
        return artwork;
    }

    /**
     * Gets a list of artists.
     *
     * @return observable list of artists
     */
    public static ObservableList<Artist> getArtists() {
        if (artists == null) {
            if (albums == null) {
                getAlbums();
            }
            // Updates the artists array list.
            updateArtistsList();
        }
        return FXCollections.observableArrayList(artists);
    }

    public static Artist getArtist(String title) {
        if (artists == null) {
            getArtists();
        }
        return artists.stream().filter(artist -> title.equals(artist.title())).findFirst().get();
    }

    private static void updateArtistsList() {
        artists = new ArrayList<>();

        TreeMap<String, List<Album>> artistMap = new TreeMap<>(
                albums.stream()
                        .filter(album -> album.artistTitle() != null)
                        .collect(Collectors.groupingBy(Album::artistTitle))
        );

        for (Map.Entry<String, List<Album>> entry : artistMap.entrySet()) {

            ArrayList<Album> albums = new ArrayList<>();

            albums.addAll(entry.getValue());

            artists.add(new Artist(entry.getKey(), new Image(Config.IMG + "artistsIcon.png"), albums));
        }
    }

    public static void addPlaylist(String title) {
        int id = playlists.stream()
                .max(Comparator.comparingInt(Playlist::getId))
                .map(Playlist::getId)
                .orElse(0);

        playlists.add(new Playlist(id, title));
    }

    public static void removePlaylist(Playlist playlist) {
        playlists.remove(playlist);
    }

    public static ObservableList<Playlist> getPlaylists() {
        System.out.println("GETTING PLAYLISTS");

        if (playlists == null) {

            playlists = new ArrayList<>();

            // TODO:
//            int id = 0;
//
//            try {
//                XMLInputFactory factory = XMLInputFactory.newInstance();
//                factory.setProperty("javax.xml.stream.isCoalescing", true);
//                FileInputStream is = new FileInputStream(new File(Resources.JAR + "library.xml"));
//                XMLStreamReader reader = factory.createXMLStreamReader(is, "UTF-8");
//
//                String element;
//                boolean isPlaylist = false;
//                String title = null;
//                ArrayList<Song> songs = new ArrayList<>();
//
//                while(reader.hasNext()) {
//                    reader.next();
//                    if (reader.isWhiteSpace()) {
//                        continue;
//                    } else if (reader.isStartElement()) {
//                        element = reader.getName().getLocalPart();
//
//                        // If the element is a play list, reads the element attributes to retrieve
//                        // the play list id and title.
//                        if (element.equals("playlist")) {
//                            isPlaylist = true;
//
//                            id = Integer.parseInt(reader.getAttributeValue(0));
//                            title = reader.getAttributeValue(1);
//                        }
//                    } else if (reader.isCharacters() && isPlaylist) {
//                        // Retrieves the reader value (song ID), gets the song and adds it to the songs list.
//                        String value = reader.getText();
//                        songs.add(getSong(Integer.parseInt(value)));
//                    } else if (reader.isEndElement() && reader.getName().getLocalPart().equals("playlist")) {
//                        // If the play list id, title, and songs have been retrieved, a new play list is created
//                        // and the values reset.
//                        var p = new Playlist(id, title);
//                        p.addSongsNoXML(songs);
//
//                        playlists.add(p);
//                        id = -1;
//                        title = null;
//                        songs = new ArrayList<>();
//                    } else if (reader.isEndElement() && reader.getName().getLocalPart().equals("playlists")) {
//                        reader.close();
//                        break;
//                    }
//                }
//                reader.close();
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//
//            playlists.sort((x, y) -> {
//                if (x.getId() < y.getId()) {
//                    return 1;
//                } else if (x.getId() > y.getId()) {
//                    return -1;
//                } else {
//                    return 0;
//                }
//            });

            playlists.add(new MostPlayedPlaylist(-2));
            playlists.add(new RecentlyPlayedPlaylist(-1));
        } else {
            playlists.sort((x, y) -> {
                if (x.getId() < y.getId()) {
                    return 1;
                } else if (x.getId() > y.getId()) {
                    return -1;
                } else {
                    return 0;
                }
            });
        }
        return FXCollections.observableArrayList(playlists);
    }

    public static Playlist getPlaylist(int id) {
        if (playlists == null) {
            getPlaylists();
        }
        // Gets the play list size.
        int playListSize = Library.getPlaylists().size();
        // The +2 takes into account the two default play lists.
        // The -1 is used because size() starts at 1 but indexes start at 0.
        return playlists.get(playListSize - (id + 2) - 1);
    }

    public static Playlist getPlaylist(String title) {
        if (playlists == null) {
            getPlaylists();
        }
        return playlists.stream().filter(playlist -> title.equals(playlist.getTitle())).findFirst().get();
    }

    public static ArrayList<Song> loadPlayingList() {
        // TODO:
        System.out.println("LOADING PLAYING LIST");

        ArrayList<Song> nowPlayingList = new ArrayList<>();
//
//        try {
//
//            XMLInputFactory factory = XMLInputFactory.newInstance();
//            FileInputStream is = new FileInputStream(new File(Resources.JAR + "library.xml"));
//            XMLStreamReader reader = factory.createXMLStreamReader(is, "UTF-8");
//
//            String element = "";
//            boolean isNowPlayingList = false;
//
//            while(reader.hasNext()) {
//                reader.next();
//                if (reader.isWhiteSpace()) {
//                    continue;
//                } else if (reader.isCharacters() && isNowPlayingList) {
//                    String value = reader.getText();
//                    if (element.equals(ID)) {
//                        nowPlayingList.add(getSong(Integer.parseInt(value)));
//                    }
//                } else if (reader.isStartElement()) {
//                    element = reader.getName().getLocalPart();
//                    if (element.equals("nowPlayingList")) {
//                        isNowPlayingList = true;
//                    }
//                } else if (reader.isEndElement() && reader.getName().getLocalPart().equals("nowPlayingList")) {
//                    reader.close();
//                    break;
//                }
//            }
//
//            reader.close();
//
//        } catch (Exception ex) {
//
//            ex.printStackTrace();
//        }

        return nowPlayingList;
    }
}
