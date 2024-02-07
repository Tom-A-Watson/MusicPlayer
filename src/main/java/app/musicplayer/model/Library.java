/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model;

import javafx.collections.ObservableList;

import javax.swing.text.html.Option;
import java.util.*;

import static app.musicplayer.model.Playlist.PlaylistType.*;

/**
 * A library is a collection of playlists.
 */
public final class Library {

    private List<Playlist> playlists = new ArrayList<>();

    /**
     * Ctor for importing library from music directory.
     */
    public Library() {
        this(List.of(
                new Playlist(ALL_SONGS, "All songs"),
                new Playlist(MOST_PLAYED, "Most Played"),
                new Playlist(RECENTLY_PLAYED, "Recently Played")
        ));
    }

    /**
     * Ctor for loading library from its serialized form.
     */
    public Library(List<Playlist> playlists) {
        boolean allPlaylistTypesArePresent = findPlaylistByType(ALL_SONGS).isPresent() &&
                findPlaylistByType(MOST_PLAYED).isPresent() &&
                findPlaylistByType(RECENTLY_PLAYED).isPresent();

        if (allPlaylistTypesArePresent) {
            this.playlists.addAll(playlists);
        }

        // TODO: check that all 3 built-in playlist types are present
    }

    public void addSongsNoDuplicateCheck(List<Song> songs) {
        getLibraryPlaylist().getSongs().addAll(songs);
    }

    public void addSong(Song song) {
        getLibraryPlaylist().addSong(song);
    }

    public void removeSong(Song song) {
        getLibraryPlaylist().getSongs().remove(song);
    }

    public ObservableList<Song> getSongs() {
        return getLibraryPlaylist().getSongs();
    }

    public List<Playlist> getPlaylists() {
        return new ArrayList<>(playlists);
    }

    public Optional<Song> findSongByTitle(String title) {
        return getSongs().stream()
                .filter(song -> title.equals(song.getTitle()))
                .findFirst();
    }

    public Playlist getLibraryPlaylist() {
        return findPlaylistByType(ALL_SONGS).get();
    }

    public Optional<Playlist> findPlaylistByType(Playlist.PlaylistType type) {
        return playlists.stream()
                .filter(playlist -> playlist.getType() == type)
                .findFirst();
    }

    public Optional<Playlist> findPlaylistByTitle(String title) {
        return playlists.stream()
                .filter(playlist -> title.equals(playlist.getTitle()))
                .findFirst();
    }

    public Playlist addPlaylist(String title) {
        var p = new Playlist(USER_CREATED, title);

        playlists.add(p);

        return p;
    }

    public void removePlaylist(Playlist playlist) {
        playlists.remove(playlist);
    }
}
