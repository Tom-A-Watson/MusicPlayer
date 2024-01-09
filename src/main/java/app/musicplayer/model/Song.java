package app.musicplayer.model;

import javafx.beans.property.*;
import javafx.scene.image.Image;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;

public final class Song implements Comparable<Song> {

    private int id;
    private SimpleStringProperty title;
    private SimpleStringProperty artist;
    private SimpleStringProperty album;
    private SimpleStringProperty length;
    private long lengthInSeconds;
    private int trackNumber;
    private int discNumber;
    private SimpleIntegerProperty playCount;

    // TODO:
    public LocalDateTime playDate;
    private String location;
    private SimpleBooleanProperty playing;
    private SimpleBooleanProperty selected;

    public Song(int id, String title, String artist, String album, Duration length,
                int trackNumber, int discNumber, int playCount, LocalDateTime playDate, String location) {

        if (title == null) {
            Path path = Paths.get(location);
            String fileName = path.getFileName().toString();
            title = fileName.substring(0, fileName.lastIndexOf('.'));
        }

        if (album == null) {
            album = "Unknown Album";
        }

        if (artist == null) {
            artist = "Unknown Artist";
        }

        this.id = id;
        this.title = new SimpleStringProperty(title);
        this.artist = new SimpleStringProperty(artist);
        this.album = new SimpleStringProperty(album);
        this.lengthInSeconds = length.getSeconds();
        long seconds = length.getSeconds() % 60;
        this.length = new SimpleStringProperty(length.toMinutes() + ":" + (seconds < 10 ? "0" + seconds : seconds));
        this.trackNumber = trackNumber;
        this.discNumber = discNumber;
        this.playCount = new SimpleIntegerProperty(playCount);
        this.playDate = playDate;
        this.location = location;
        this.playing = new SimpleBooleanProperty(false);
        this.selected = new SimpleBooleanProperty(false);
    }

    public int getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title.get();
    }

    public StringProperty titleProperty() {
        return this.title;
    }

    public String getArtist() {
        return this.artist.get();
    }

    public StringProperty artistProperty() {
        return this.artist;
    }

    public String getAlbum() {
        return this.album.get();
    }

    public Image getArtwork() {
        return Library.getAlbum(this.album.get()).artwork();
    }

    public StringProperty albumProperty() {
        return this.album;
    }

    public String getLength() {
        return this.length.get();
    }

    public StringProperty lengthProperty() {
        return this.length;
    }

    public long getLengthInSeconds() {
        return this.lengthInSeconds;
    }

    public int getTrackNumber() {
        return this.trackNumber;
    }

    public int getDiscNumber() {
        return this.discNumber;
    }

    public int getPlayCount() {
        return this.playCount.get();
    }

    public IntegerProperty playCountProperty() {
        return this.playCount;
    }

    public LocalDateTime getPlayDate() {
        return this.playDate;
    }

    public String getLocation() {
        return this.location;
    }

    public BooleanProperty playingProperty() {
        return this.playing;
    }

    public boolean getPlaying() {
        return this.playing.get();
    }

    public void setPlaying(boolean playing) {
        this.playing.set(playing);
    }

    public BooleanProperty selectedProperty() {
        return this.selected;
    }

    public boolean getSelected() {
        return this.selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    @Override
    public int compareTo(Song other) throws NullPointerException {
        int discComparison = Integer.compare(this.discNumber, other.discNumber);

        if (discComparison != 0) {
            return discComparison;
        } else {
            return Integer.compare(this.trackNumber, other.trackNumber);
        }
    }
}