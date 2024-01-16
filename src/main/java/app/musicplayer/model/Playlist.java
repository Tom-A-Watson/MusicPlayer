/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

// TODO: sort songs
public class Playlist implements Comparable<Playlist> {

    private int id;
    private String title;
    private ObservableList<Song> songs = FXCollections.observableArrayList();

    public Playlist(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public ObservableList<Song> getSongs() {
        return songs;
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
        return Integer.compare(getId(), other.getId());
    }

    @Override
    public String toString() {
        return title;
    }
}