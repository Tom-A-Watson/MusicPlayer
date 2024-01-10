/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model.serializable;

import app.musicplayer.model.Library;
import app.musicplayer.model.Playlist;
import app.musicplayer.model.Song;
import app.musicplayer.util.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * @author Almas Baim (https://github.com/AlmasB)
 */
public final class Serializer {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
    }

    public static SerializableSong toSerializable(Song song) {
        return new SerializableSong(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getAlbum(),
                (int) song.getLengthInSeconds(),
                song.getTrackNumber(),
                song.getDiscNumber(),
                song.getPlayCount(),
                song.getPlayDate(),
                song.getLocation()
        );
    }

    public static Song fromSerializable(SerializableSong song) {
        return new Song(
                song.id(),
                song.title(),
                song.artistTitle(),
                song.albumTitle(),
                Duration.ofSeconds(song.lengthInSeconds()),
                song.trackNumber(),
                song.discNumber(),
                song.playCount(),
                song.playDate(),
                song.filePath()
        );
    }

    public static SerializablePlaylist toSerializable(Playlist playlist) {
        return new SerializablePlaylist(
                playlist.getId(),
                playlist.getTitle(),
                playlist.getSongs().stream().map(Song::getId).toList()
        );
    }

    public static SerializableLibrary toSerializable(Library library) {
        return new SerializableLibrary(
                Library.getMusicDirectory().toAbsolutePath().toString(),
                Library.getSongs().stream().map(Serializer::toSerializable).toList(),
                Library.getPlaylists().stream().map(Serializer::toSerializable).toList()
        );
    }

    public static Library fromSerializable(SerializableLibrary library) {
        Library.importFromLibraryFile(
                library.musicDirectoryPath(),
                library.songs().stream().map(Serializer::fromSerializable).toList()
        );

        return new Library();
    }

    public static void writeToFile(Path file) {
        try {
            var lib = Serializer.toSerializable(new Library());

            var writer = mapper.writerWithDefaultPrettyPrinter();

            String json = writer.writeValueAsString(lib);

            Files.writeString(file, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void readFromFile(Path file) {
        try {
            var lib = mapper.readValue(file.toFile(), SerializableLibrary.class);
            fromSerializable(lib);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
