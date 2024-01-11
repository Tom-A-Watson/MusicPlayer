/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.model;

import javafx.beans.property.*;

import java.nio.file.Path;
import java.time.LocalDateTime;

public final class Song implements Comparable<Song> {

    private IntegerProperty id;
    private StringProperty title;
    private StringProperty artistTitle;
    private StringProperty albumTitle;
    private IntegerProperty lengthInSeconds;
    private IntegerProperty trackNumber;
    private IntegerProperty discNumber;
    private IntegerProperty playCount;
    private ObjectProperty<LocalDateTime> playDate;
    private ObjectProperty<Path> file;

    private BooleanProperty isPlaying;
    private BooleanProperty isSelected;
    private ObjectProperty<Album> album;
    private ObjectProperty<Artist> artist;

    public Song(
            int id,
            String title,
            String artistTitle,
            String albumTitle,
            int lengthInSeconds,
            int trackNumber,
            int discNumber,
            int playCount,
            LocalDateTime playDate,
            Path file
    ) {
        this.id = new SimpleIntegerProperty(id);
        this.title = new SimpleStringProperty(title);
        this.artistTitle = new SimpleStringProperty(artistTitle);
        this.albumTitle = new SimpleStringProperty(albumTitle);
        this.lengthInSeconds = new SimpleIntegerProperty(lengthInSeconds);
        this.trackNumber = new SimpleIntegerProperty(trackNumber);
        this.discNumber = new SimpleIntegerProperty(discNumber);
        this.playCount = new SimpleIntegerProperty(playCount);
        this.playDate = new SimpleObjectProperty<>(playDate);
        this.file = new SimpleObjectProperty<>(file);

        this.isPlaying = new SimpleBooleanProperty(false);
        this.isSelected = new SimpleBooleanProperty(false);
        this.album = new SimpleObjectProperty<>(null);
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

    public String getAlbumTitle() {
        return albumTitle.get();
    }

    public StringProperty albumTitleProperty() {
        return albumTitle;
    }

    public void setAlbumTitle(String albumTitle) {
        this.albumTitle.set(albumTitle);
    }

    public int getLengthInSeconds() {
        return lengthInSeconds.get();
    }

    public IntegerProperty lengthInSecondsProperty() {
        return lengthInSeconds;
    }

    public void setLengthInSeconds(int lengthInSeconds) {
        this.lengthInSeconds.set(lengthInSeconds);
    }

    public int getTrackNumber() {
        return trackNumber.get();
    }

    public IntegerProperty trackNumberProperty() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber.set(trackNumber);
    }

    public int getDiscNumber() {
        return discNumber.get();
    }

    public IntegerProperty discNumberProperty() {
        return discNumber;
    }

    public void setDiscNumber(int discNumber) {
        this.discNumber.set(discNumber);
    }

    public int getPlayCount() {
        return playCount.get();
    }

    public IntegerProperty playCountProperty() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount.set(playCount);
    }

    public LocalDateTime getPlayDate() {
        return playDate.get();
    }

    public ObjectProperty<LocalDateTime> playDateProperty() {
        return playDate;
    }

    public void setPlayDate(LocalDateTime playDate) {
        this.playDate.set(playDate);
    }

    public Path getFile() {
        return file.get();
    }

    public ObjectProperty<Path> fileProperty() {
        return file;
    }

    public void setFile(Path file) {
        this.file.set(file);
    }

    public boolean isPlaying() {
        return isPlaying.get();
    }

    public BooleanProperty playingProperty() {
        return isPlaying;
    }

    public void setPlaying(boolean isPlaying) {
        this.isPlaying.set(isPlaying);
    }

    public boolean isSelected() {
        return isSelected.get();
    }

    public BooleanProperty selectedProperty() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected.set(isSelected);
    }

    public Album getAlbum() {
        return album.get();
    }

    public ObjectProperty<Album> albumProperty() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album.set(album);
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

    //        long seconds = length.getSeconds() % 60;
//        this.length = new SimpleStringProperty(length.toMinutes() + ":" + (seconds < 10 ? "0" + seconds : seconds));
//    public Image getArtwork() {
//        return MusicPlayerApp.getLibrary().getAlbum(this.album.get()).artwork();
//    }

    @Override
    public int compareTo(Song other) {
        int discComparison = Integer.compare(this.getDiscNumber(), other.getDiscNumber());

        if (discComparison != 0) {
            return discComparison;
        }

        return Integer.compare(this.getTrackNumber(), other.getTrackNumber());
    }
}