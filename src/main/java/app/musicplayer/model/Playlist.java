/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A playlist is a collection of songs.
 * The order (in user created playlists) is defined by the user.
 */
public final class Playlist implements Comparable<Playlist> {

    public enum PlaylistType {
        ALL_SONGS, MOST_PLAYED, RECENTLY_PLAYED, USER_CREATED
    }

    private ObservableList<Song> songs = FXCollections.observableArrayList();
    private PlaylistType type;
    private StringProperty title;

    public Playlist(PlaylistType type, String title) {
        this.type = type;
        this.title = new SimpleStringProperty(title);
    }

    public PlaylistType getType() {
        return type;
    }

    public boolean isModifiable() {
        return type == PlaylistType.USER_CREATED;
    }

    public ObservableList<Song> getSongs() {
        return songs;
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }
    
    public void addSong(Song song) {
    	if (!songs.contains(song)) {
    		songs.add(song);
    	}
    }
    
    public void removeSong(int songId) {
        songs.removeIf(s -> s.getId() == songId);
    }

    @Override
    public int compareTo(Playlist other) {
        if (!isModifiable() && other.isModifiable()) {
            return -1;
        }

        if (isModifiable() && !other.isModifiable()) {
            return 1;
        }

        return getTitle().compareTo(other.getTitle());
    }

    @Override
    public String toString() {
        return getTitle();
    }
}