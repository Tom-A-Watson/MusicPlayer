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
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;

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
                song.getArtistTitle(),
                song.getAlbumTitle(),
                song.getLengthInSeconds(),
                song.getTrackNumber(),
                song.getDiscNumber(),
                song.getPlayCount(),
                song.getPlayDate(),
                song.getFile().toAbsolutePath().toString()
        );
    }

    public static Song fromSerializable(SerializableSong song) {
        return new Song(
                song.id(),
                song.title(),
                song.artistTitle(),
                song.albumTitle(),
                song.lengthInSeconds(),
                song.trackNumber(),
                song.discNumber(),
                song.playCount(),
                song.playDate(),
                Paths.get(song.filePath())
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
                library.getMusicDirectory().toAbsolutePath().toString(),
                library.getSongs().stream().map(Serializer::toSerializable).toList(),
                library.getPlaylists().stream().map(Serializer::toSerializable).toList()
        );
    }

    public static void writeToFile(Library library, Path file) {
        try {
            var lib = Serializer.toSerializable(library);

            var writer = mapper.writerWithDefaultPrettyPrinter();

            String json = writer.writeValueAsString(lib);

            Files.writeString(file, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SerializableLibrary readFromFile(Path file) {
        try {
            var lib = mapper.readValue(file.toFile(), SerializableLibrary.class);
            
            return lib;

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return new SerializableLibrary(
                Paths.get("./").toString(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
