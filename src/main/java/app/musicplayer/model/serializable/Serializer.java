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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                song.getLengthInSeconds(),
                song.getPlayCount(),
                song.getPlayDate(),
                song.getFile().toAbsolutePath().toString()
        );
    }

    public static Song fromSerializable(SerializableSong song) {
        return new Song(
                song.id(),
                song.title(),
                song.lengthInSeconds(),
                song.playCount(),
                song.playDate(),
                Paths.get(song.filePath())
        );
    }

    public static SerializablePlaylist toSerializable(Playlist playlist) {
        playlist.restoreFromShuffle();

        return new SerializablePlaylist(
                playlist.getType(),
                playlist.getTitle(),
                playlist.getSongs().stream().map(Song::getId).toList()
        );
    }

    public static Playlist fromSerializable(SerializablePlaylist playlist) {
        return new Playlist(
                playlist.type(), playlist.title()
        );
    }

    public static SerializableLibrary toSerializable(Library library) {
        return new SerializableLibrary(
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
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
