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

import java.util.*;

/**
 * A playlist is a collection of songs.
 * The order (in user created playlists) is defined by the user.
 */
public final class Playlist implements Comparable<Playlist> {

    public enum PlaylistType {
        ALL_SONGS, MOST_PLAYED, RECENTLY_PLAYED, USER_CREATED
    }

    private List<Integer> originalOrder = new ArrayList<>();
    private ObservableList<Song> songs = FXCollections.observableArrayList();

    /**
     * Tells us if we can trust the view in [songs].
     */
    private boolean isViewDirty = false;

    private PlaylistType type;
    private StringProperty title;
    private Song lastSelectedSong = null;

    public Playlist(PlaylistType type, String title) {
        this.type = type;
        this.title = new SimpleStringProperty(title);
    }

    public void shuffle() {
        if (isViewDirty)
            return;

        originalOrder.clear();
        originalOrder.addAll(
                songs.stream()
                        .map(Song::getId)
                        .toList()
        );

        // do not shuffle potentially live scene graph
        var tmp = new ArrayList<>(songs);
        Collections.shuffle(tmp);
        songs.setAll(tmp);

        isViewDirty = true;
    }

    public void restoreFromShuffle() {
        if (!isViewDirty)
            return;

        var idsToRemove = originalOrder.stream()
                .filter(id -> songs.stream().noneMatch(s -> s.getId() == id))
                .toList();

        originalOrder.removeAll(idsToRemove);

        songs.forEach(s -> {
            if (!originalOrder.contains(s.getId())) {
                originalOrder.add(s.getId());
            }
        });

        // do not sort potentially live scene graph
        var tmp = new ArrayList<>(songs);
        tmp.sort(Comparator.comparingInt(song -> originalOrder.indexOf(song.getId())));
        songs.setAll(tmp);

        isViewDirty = false;
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

    public Optional<Song> lastSelectedSong() {
        return Optional.ofNullable(lastSelectedSong);
    }

    public void setLastSelectedSong(Song lastSelectedSong) {
        this.lastSelectedSong = lastSelectedSong;
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