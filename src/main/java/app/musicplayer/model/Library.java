/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model;

import com.almasb.fxgl.logging.Logger;

import java.nio.file.Path;
import java.util.*;

/**
 * A library is a collection songs, artists, albums and playlists.
 */
public final class Library {

    private static final Logger log = Logger.get(Library.class);

    private List<Song> songs = new ArrayList<>();
    private List<Album> albums = new ArrayList<>();
    private List<Artist> artists = new ArrayList<>();
    private List<Playlist> playlists = new ArrayList<>();

    private Path musicDirectory;

    /**
     * Ctor for importing library from music directory.
     */
    public Library(Path musicDirectory, List<Song> songs, List<Album> albums, List<Artist> artists) {
        this(musicDirectory, songs, albums, artists, List.of(
                new MostPlayedPlaylist(-2),
                new RecentlyPlayedPlaylist(-1)
        ));
    }

    /**
     * Ctor for loading library from its serialized form.
     */
    public Library(Path musicDirectory, List<Song> songs, List<Album> albums, List<Artist> artists, List<Playlist> playlists) {
        this.musicDirectory = musicDirectory;
        this.songs.addAll(songs);
        this.albums.addAll(albums);
        this.artists.addAll(artists);
        this.playlists.addAll(playlists);

        // TODO: sort others?
        this.playlists.sort((p1, p2) -> Integer.compare(p2.getId(), p1.getId()));
    }

    public Path getMusicDirectory() {
        return musicDirectory;
    }

    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }

    public List<Album> getAlbums() {
        return new ArrayList<>(albums);
    }

    public List<Artist> getArtists() {
        return new ArrayList<>(artists);
    }

    public List<Playlist> getPlaylists() {
        return new ArrayList<>(playlists);
    }

    public Optional<Song> findSongByTitle(String title) {
        return songs.stream()
                .filter(song -> title.equals(song.getTitle()))
                .findFirst();
    }

    public Optional<Album> findAlbumByTitle(String title) {
        return albums.stream()
                .filter(album -> title.equals(album.getTitle()))
                .findFirst();
    }

    public Optional<Artist> findArtistByTitle(String title) {
        return artists.stream()
                .filter(artist -> title.equals(artist.title()))
                .findFirst();
    }




    // TODO: below
    public void addPlaylist(String title) {
        int id = playlists.stream()
                .max(Comparator.comparingInt(Playlist::getId))
                .map(Playlist::getId)
                .orElse(0);

        playlists.add(new Playlist(id, title));
    }

    public void removePlaylist(Playlist playlist) {
        playlists.remove(playlist);
    }

    public Playlist getPlaylist(int id) {
        // Gets the play list size.
        int playListSize = playlists.size();
        // The +2 takes into account the two default play lists.
        // The -1 is used because size() starts at 1 but indexes start at 0.
        return playlists.get(playListSize - (id + 2) - 1);
    }

    public Playlist getPlaylist(String title) {
        return playlists.stream().filter(playlist -> title.equals(playlist.getTitle())).findFirst().get();
    }
}
