package app.tmdb;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable DTO für eine Filmkachel im MovieViewer.
 * 
 * Die headerLines werden vom Konstruktor fertig formatiert, damit der Skin
 * keine Typ-spezifische Logik braucht. Erste Zeile wird bold dargestellt.
 * 
 * Perspektivisch kann dieses Record durch ein Interface ersetzt werden,
 * das auch TvShowCardData, SeasonCardData und EpisodeCardData abdeckt.
 */
public record MovieCardData(
        int id,
        List<String> headerLines,
        int rating,
        String ratedAt,
        List<String> directors,
        List<String> actors,
        String overview,
        String comment,
        String imageFilename
) {

    /**
     * Factory-Methode für Filme. Formatiert die Header-Zeilen nach dem
     * bewährten Geosuite-Format:
     * 
     * Zeile 1 (bold): "Title (Year)"
     * Zeile 2 (bold): "German Title" — nur wenn abweichend vom Title
     */
    public static MovieCardData forMovie(
            int id,
            String title,
            String germanTitle,
            String originalTitle,
            LocalDate releaseDate,
            int rating,
            String ratedAt,
            List<String> directors,
            List<String> actors,
            String overview,
            String comment,
            String imageFilename) {

        List<String> header = new ArrayList<>();
        String year = releaseDate != null ? String.valueOf(releaseDate.getYear()) : "?";
        header.add(title + " (" + year + ")");

        if (germanTitle != null && !germanTitle.equals(title)) {
            header.add(germanTitle);
        }

        return new MovieCardData(
                id,
                Collections.unmodifiableList(header),
                rating,
                ratedAt,
                Collections.unmodifiableList(directors),
                Collections.unmodifiableList(actors),
                overview,
                comment,
                imageFilename);
    }
}