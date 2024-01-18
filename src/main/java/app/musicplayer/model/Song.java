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
    private IntegerProperty lengthInSeconds;
    private IntegerProperty playCount;
    private ObjectProperty<LocalDateTime> playDate;
    private ObjectProperty<Path> file;

    private StringProperty displayLength;
    private BooleanProperty isPlaying;
    private BooleanProperty isSelected;

    public Song(
            int id,
            String title,
            int lengthInSeconds,
            int playCount,
            LocalDateTime playDate,
            Path file
    ) {
        this.id = new SimpleIntegerProperty(id);
        this.title = new SimpleStringProperty(title);
        this.lengthInSeconds = new SimpleIntegerProperty(lengthInSeconds);
        this.playCount = new SimpleIntegerProperty(playCount);
        this.playDate = new SimpleObjectProperty<>(playDate);
        this.file = new SimpleObjectProperty<>(file);

        int minutes = lengthInSeconds / 60;
        int seconds = lengthInSeconds % 60;
        var displayLength = minutes + ":" + (seconds < 10 ? "0" + seconds : Integer.toString(seconds));

        this.displayLength = new SimpleStringProperty(displayLength);
        this.isPlaying = new SimpleBooleanProperty(false);
        this.isSelected = new SimpleBooleanProperty(false);
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

    public int getLengthInSeconds() {
        return lengthInSeconds.get();
    }

    public IntegerProperty lengthInSecondsProperty() {
        return lengthInSeconds;
    }

    public void setLengthInSeconds(int lengthInSeconds) {
        this.lengthInSeconds.set(lengthInSeconds);
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

    public String getDisplayLength() {
        return displayLength.get();
    }

    public StringProperty displayLengthProperty() {
        return displayLength;
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

    @Override
    public int compareTo(Song other) {
        return getTitle().compareTo(other.getTitle());
    }

    @Override
    public String toString() {
        return getTitle();
    }
}