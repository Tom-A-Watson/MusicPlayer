package app.musicplayer.model;

import app.musicplayer.util.Resources;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;

public final class Album implements Comparable<Album> {

    private int id;
    private String title;
    private String artist;
    private Image artwork;
    private ArrayList<Song> songs;
    private SimpleObjectProperty<Image> artworkProperty;

    /**
     * Constructor for the Album class. 
     * Creates an album object and obtains the album artwork.
     *
     * @param id
     * @param title
     * @param artist
     * @param songs
     */
    public Album(int id, String title, String artist, ArrayList<Song> songs) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.songs = songs;
        this.artworkProperty = new SimpleObjectProperty<>(getArtwork());
    }

    /**
     * Gets album ID.
     *
     * @return album ID
     */
    public int getId() {
        return this.id;
    }

    /**
     * Gets album title
     *
     * @return album title
     */
    public String getTitle() {
        return this.title;
    }

    public String getArtist() {
        return this.artist;
    }

    public ArrayList<Song> getSongs() {
        return new ArrayList<>(this.songs);
    }

    public ObjectProperty<Image> artworkProperty() {
        return this.artworkProperty;
    }

    public Image getArtwork() {
        if (this.artwork == null) {

            try {
                String location = this.songs.get(0).getLocation();
                AudioFile audioFile = AudioFileIO.read(new File(location));
                Tag tag = audioFile.getTag();
                byte[] bytes = tag.getFirstArtwork().getBinaryData();
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                this.artwork = new Image(in, 300, 300, true, true);

                if (this.artwork.isError()) {
                    this.artwork = new Image(Resources.IMG + "albumsIcon.png");
                }

            } catch (Exception ex) {
                this.artwork = new Image(Resources.IMG + "albumsIcon.png");
            }
        }
        return this.artwork;
    }

    @Override
    public int compareTo(Album other) {
        String first = removeArticle(this.title);
        String second = removeArticle(other.title);

        return first.compareTo(second);
    }

    private String removeArticle(String title) {

        String arr[] = title.split(" ", 2);

        if (arr.length < 2) {

            return title;

        } else {

            String firstWord = arr[0];
            String theRest = arr[1];

            switch (firstWord) {
                case "A":
                case "An":
                case "The":
                    return theRest;
                default:
                    return title;
            }
        }
    }
}
