package app.musicplayer.util;

import app.musicplayer.MusicPlayerApp;
import app.musicplayer.model.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.List;
import java.util.stream.Collectors;

public class Search {

    private static BooleanProperty hasResults = new SimpleBooleanProperty(false);
    private static SearchResult result;
    private static Thread searchThread;

    public static BooleanProperty hasResultsProperty() {
        return hasResults;
    }

    public static SearchResult getResult() {
        hasResults.set(false);
        return result;
    }

    public static void search(String searchText) {
        if (searchThread != null && searchThread.isAlive()) {
            searchThread.interrupt();
        }

        String text = searchText.toUpperCase();

        searchThread = new Thread(() -> {
            try {
                hasResults.set(false);

                List<Song> songResults = MusicPlayerApp.getLibrary()
                        .getSongs()
                        .stream()
                        .filter(song -> song.getTitle().toUpperCase().contains(text))
                        .sorted((x, y) -> {
                            return compareSearchString(x.getTitle().toUpperCase(), y.getTitle().toUpperCase(), text);
                        })
                        .collect(Collectors.toList());

                if (searchThread.isInterrupted()) {
                    throw new InterruptedException();
                }

                List<Album> albumResults = MusicPlayerApp.getLibrary()
                        .getAlbums()
                        .stream()
                        .filter(album -> album.getTitle().toUpperCase().contains(text))
                        .sorted((x, y) -> {
                            return compareSearchString(x.getTitle().toUpperCase(), y.getTitle().toUpperCase(), text);
                        })
                        .collect(Collectors.toList());

                if (searchThread.isInterrupted()) {
                    throw new InterruptedException();
                }

                List<Artist> artistResults = MusicPlayerApp.getLibrary()
                        .getArtists()
                        .stream()
                        .filter(artist -> artist.getTitle().toUpperCase().contains(text))
                        .sorted((x, y) -> {
                            return compareSearchString(x.getTitle().toUpperCase(), y.getTitle().toUpperCase(), text);
                        })
                        .collect(Collectors.toList());

                if (searchThread.isInterrupted()) {
                    throw new InterruptedException();
                }

                if (songResults.size() > 3)
                    songResults = songResults.subList(0, 3);

                if (albumResults.size() > 3)
                    albumResults = albumResults.subList(0, 3);

                if (artistResults.size() > 3)
                    artistResults = artistResults.subList(0, 3);

                result = new SearchResult(songResults, albumResults, artistResults);

                hasResults.set(true);

            } catch (InterruptedException ex) {
                // terminate thread
            }
        });
        searchThread.start();
    }

    /**
     * All arguments must be uppercase.
     *
     * @return Comparator compareTo() int
     */
    private static int compareSearchString(String s1, String s2, String text) {
        boolean xMatch = s1.equals(text);
        boolean yMatch = s2.equals(text);
        if (xMatch && yMatch)
            return 0;
        if (xMatch)
            return -1;
        if (yMatch)
            return 1;

        boolean xStartWith = s1.startsWith(text);
        boolean yStartWith = s2.startsWith(text);
        if (xStartWith && yStartWith)
            return 0;
        if (xStartWith)
            return -1;
        if (yStartWith)
            return 1;

        boolean xContains = s1.contains(" " + text);
        boolean yContains = s2.contains(" " + text);
        if (xContains && yContains)
            return 0;
        if (xContains)
            return -1;
        if (yContains)
            return 1;

        return 0;
    }

    public record SearchResult(
            List<Song> songResults,
            List<Album> albumResults,
            List<Artist> artistResults) {
    }
}