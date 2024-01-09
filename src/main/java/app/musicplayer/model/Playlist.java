/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model;

import app.musicplayer.util.XMLUtil;

import java.util.ArrayList;
import java.util.List;

public class Playlist {

    private int id;
    private String title;
    private List<Song> songs = new ArrayList<>();

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

    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }

    public void addSongsNoXML(List<Song> songs) {
        this.songs.addAll(songs);
    }
    
    public void addSong(Song song) {
    	if (!songs.contains(song)) {
    		songs.add(song);

            XMLUtil.addSongToPlaylist(this, song);
    	}
    }
    
    public void removeSong(int songId) {
        songs.removeIf(s -> s.getId() == songId);
    }

    @Override
    public String toString() {
        return title;
    }
}