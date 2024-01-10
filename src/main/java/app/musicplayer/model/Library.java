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

                initAll();

                return true;
            }
        };
    }

    public static void importFromLibraryFile(String musicDirPath, List<Song> parsedSongs) {
        musicDirectory = Paths.get(musicDirPath);
        songs.addAll(parsedSongs);
        initAll();
    }

    private static void initAll() {
        updateAlbumsList();
        updateArtistsList();

        // update playlists
        playlists.add(new MostPlayedPlaylist(-2));
        playlists.add(new RecentlyPlayedPlaylist(-1));
        playlists.sort((p1, p2) -> Integer.compare(p2.getId(), p1.getId()));
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

                    String title = tag.getFirst(FieldKey.TITLE);
                    String artistTitle = tag.getFirst(FieldKey.ALBUM_ARTIST);

                    if (isEmpty(artistTitle)) {
                        artistTitle = tag.getFirst(FieldKey.ARTIST);
                    }

                    String albumTitle = tag.getFirst(FieldKey.ALBUM);

                    int lengthSeconds = header.getTrackLength();
                    String track = tag.getFirst(FieldKey.TRACK);
                    int trackNumber = isEmpty(track) ? 0 : Integer.parseInt(track);

                    String disc = tag.getFirst(FieldKey.DISC_NO);
                    int discNumber = isEmpty(disc) ? 0 : Integer.parseInt(disc);

                    songs.add(new Song(
                            id++,
                            makeSafe(title),
                            makeSafe(artistTitle),
                            makeSafe(albumTitle),
                            lengthSeconds,
                            trackNumber,
                            discNumber,
                            0,
                            LocalDateTime.now(),
                            file.toPath()
                    ));

                } catch (Exception ex) {

                    ex.printStackTrace();
                }

            } else if (file.isDirectory()) {

                loadSongs(file);
            }
        }
    }

    private static String makeSafe(String s) {
        if (isEmpty(s))
            return "Unknown";

        return s;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty() || s.equals("null");
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

    public static List<Song> getSongs() {
        return songs;
    }

    public static List<Album> getAlbums() {
        return albums;
    }

    public static List<Artist> getArtists() {
        return artists;
    }

    public static List<Playlist> getPlaylists() {
        return playlists;
    }


















    private static Song getSong(int id) {
        return songs.get(id);
    }

    public static Song getSong(String title) {
        return songs.stream().filter(song -> title.equals(song.getTitle())).findFirst().get();
    }

    public static Album getAlbum(String title) {
        return albums.stream().filter(album -> title.equals(album.title())).findFirst().get();
    }

    private static void updateAlbumsList() {
        TreeMap<String, List<Song>> albumMap = new TreeMap<>(
                songs.stream().collect(Collectors.groupingBy(Song::getAlbumTitle))
        );

        int id = 0;

        for (Map.Entry<String, List<Song>> entry : albumMap.entrySet()) {

            ArrayList<Song> songs = new ArrayList<>(entry.getValue());

            TreeMap<String, List<Song>> artistMap = new TreeMap<>(
                    songs.stream()
                            .collect(Collectors.groupingBy(Song::getArtistTitle))
            );

            for (Map.Entry<String, List<Song>> e : artistMap.entrySet()) {
                String artist = e.getValue().get(0).getArtistTitle();

                ArrayList<Song> albumSongs = new ArrayList<>(e.getValue());

                albums.add(new Album(id++, entry.getKey(), artist, getArtwork(songs), albumSongs));
            }
        }
    }

    private static Image getArtwork(List<Song> songs) {
        Image artwork = null;

        try {
            AudioFile audioFile = AudioFileIO.read(songs.get(0).getFile().toFile());
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

        return artwork;
    }

    public static Artist getArtist(String title) {
        return artists.stream().filter(artist -> title.equals(artist.title())).findFirst().get();
    }

    private static void updateArtistsList() {
        TreeMap<String, List<Album>> artistMap = new TreeMap<>(
                albums.stream()
                        .filter(album -> album.artistTitle() != null)
                        .collect(Collectors.groupingBy(Album::artistTitle))
        );

        for (Map.Entry<String, List<Album>> entry : artistMap.entrySet()) {

            ArrayList<Album> albums = new ArrayList<>(entry.getValue());

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

    public static Playlist getPlaylist(int id) {
        // Gets the play list size.
        int playListSize = Library.getPlaylists().size();
        // The +2 takes into account the two default play lists.
        // The -1 is used because size() starts at 1 but indexes start at 0.
        return playlists.get(playListSize - (id + 2) - 1);
    }

    public static Playlist getPlaylist(String title) {
        return playlists.stream().filter(playlist -> title.equals(playlist.getTitle())).findFirst().get();
    }

    public static ArrayList<Song> loadPlayingList() {
        ArrayList<Song> nowPlayingList = new ArrayList<>();

        return nowPlayingList;
    }
}
