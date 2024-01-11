/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * An album has a collection of songs.
 */
public final class Album implements Comparable<Album> {

    private IntegerProperty id;
    private StringProperty title;
    private StringProperty artistTitle;
    private ObjectProperty<Image> artwork;
    private ObservableList<Song> songs;

    private ObjectProperty<Artist> artist;

    public Album(
            int id,
            String title,
            String artistTitle,
            Image artwork,
            List<Song> songs
    ) {
        this.id = new SimpleIntegerProperty(id);
        this.title = new SimpleStringProperty(title);
        this.artistTitle = new SimpleStringProperty(artistTitle);
        this.artwork = new SimpleObjectProperty<>(artwork);
        this.songs = FXCollections.observableList(new ArrayList<>(songs));

        this.artist = new SimpleObjectProperty<>(null);
    }

    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(int id) {
        this.id.set(id);
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

    public String getArtistTitle() {
        return artistTitle.get();
    }

    public StringProperty artistTitleProperty() {
        return artistTitle;
    }

    public void setArtistTitle(String artistTitle) {
        this.artistTitle.set(artistTitle);
    }

    public Image getArtwork() {
        return artwork.get();
    }

    public ObjectProperty<Image> artworkProperty() {
        return artwork;
    }

    public void setArtwork(Image artwork) {
        this.artwork.set(artwork);
    }

    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }

    public Artist getArtist() {
        return artist.get();
    }

    public ObjectProperty<Artist> artistProperty() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist.set(artist);
    }

    @Override
    public int compareTo(Album other) {
        return getTitle().compareTo(other.getTitle());
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
