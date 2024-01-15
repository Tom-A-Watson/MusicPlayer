/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model;

import java.nio.file.Path;
import java.util.*;

/**
 * A library is a collection songs, artists, albums and playlists.
 */
public final class Library {

    private List<Song> songs = new ArrayList<>();
    private List<Playlist> playlists = new ArrayList<>();

    private Path musicDirectory;

    /**
     * Ctor for importing library from music directory.
     */
    public Library(Path musicDirectory, List<Song> songs) {
        this(musicDirectory, songs, List.of(
                new MostPlayedPlaylist(-2),
                new RecentlyPlayedPlaylist(-1)
        ));
    }

    /**
     * Ctor for loading library from its serialized form.
     */
    public Library(Path musicDirectory, List<Song> songs, List<Playlist> playlists) {
        this.musicDirectory = musicDirectory;
        this.songs.addAll(songs);
        this.playlists.addAll(playlists);

        Collections.sort(this.songs);
        Collections.sort(this.playlists);
    }

    public Path getMusicDirectory() {
        return musicDirectory;
    }

    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }

    public List<Playlist> getPlaylists() {
        return new ArrayList<>(playlists);
    }

    public Optional<Song> findSongByTitle(String title) {
        return songs.stream()
                .filter(song -> title.equals(song.getTitle()))
                .findFirst();
    }

    public Optional<Playlist> findPlaylistByTitle(String title) {
        return playlists.stream()
                .filter(playlist -> title.equals(playlist.getTitle()))
                .findFirst();
    }

    public void addPlaylist(String title) {
        int highestID = playlists.stream()
                .max(Comparator.comparingInt(Playlist::getId))
                .map(Playlist::getId)
                .orElse(0);

        playlists.add(new Playlist(highestID + 1, title));
    }

    public void removePlaylist(Playlist playlist) {
        playlists.remove(playlist);
    }
}
