package app.tmdb;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable DTO für eine Kachel im Viewer.
 * Typ-agnostisch: Filme, Serien und Episoden nutzen dasselbe Record.
 *
 * Der Skin rendert uniform:
 * - headerLines werden bold dargestellt (per String.join("\n",...) in ein Label)
 * - detailLines werden normal dargestellt (zwischen Header und "Rated:")
 * - Dann Rated, Directors (Links), Actors (Links), Overview, Comment
 *
 * Die Factory-Methoden formatieren die Header- und Detail-Zeilen
 * typ-spezifisch, damit der Skin keine Typ-Logik braucht.
 *
 * sortDate wird nicht angezeigt, sondern nur zum Sortieren der Ergebnisliste
 * verwendet, wenn Filme, Serien und Episoden gemischt dargestellt werden.
 */
public record CardData(
        int id,
        List<String> headerLines,
        List<String> detailLines,
        LocalDate sortDate,
        int rating,
        LocalDate ratedAt,
        List<String> directors,
        List<String> actors,
        String overview,
        String comment,
        String imageFilename
) {

    // -------------------------------------------------------------------------
    // Film
    // -------------------------------------------------------------------------

    /**
     * Factory für Filme.
     *
     * Header (bold):
     *   "Title (Year)"
     *   "German Title" — nur wenn abweichend vom Title
     *
     * Detail: keine
     * sortDate: release_date
     */
    public static CardData forMovie(
            int id,
            String title,
            String germanTitle,
            String originalTitle,
            LocalDate releaseDate,
            int rating,
            LocalDate ratedAt,
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

        return new CardData(
                id,
                Collections.unmodifiableList(header),
                List.of(),
                releaseDate,
                rating,
                ratedAt,
                Collections.unmodifiableList(directors),
                Collections.unmodifiableList(actors),
                overview,
                comment,
                imageFilename);
    }

    // -------------------------------------------------------------------------
    // Serie (TvShow)
    // -------------------------------------------------------------------------

    /**
     * Factory für bewertete Serien.
     *
     * Header (bold):
     *   "Name (FirstAirYear - LastAirYear)"
     *   "German Name" — nur wenn abweichend vom Name
     *
     * Detail:
     *   "Seasons: 12"
     *   "Episodes: 120"
     *
     * sortDate: first_air_date
     */
    public static CardData forTvShow(
            int id,
            String name,
            String germanName,
            LocalDate firstAirDate,
            LocalDate lastAirDate,
            int numberOfSeasons,
            int numberOfEpisodes,
            int rating,
            LocalDate ratedAt,
            List<String> directors,
            List<String> actors,
            String overview,
            String comment,
            String imageFilename) {

        List<String> header = new ArrayList<>();
        String firstYear = firstAirDate != null ? String.valueOf(firstAirDate.getYear()) : "?";
        String lastYear = lastAirDate != null ? String.valueOf(lastAirDate.getYear()) : "?";
        header.add(name + " (" + firstYear + " - " + lastYear + ")");

        if (germanName != null && !germanName.equals(name)) {
            header.add(germanName);
        }

        List<String> detail = new ArrayList<>();
        detail.add("Seasons: " + numberOfSeasons);
        detail.add("Episodes: " + numberOfEpisodes);

        return new CardData(
                id,
                Collections.unmodifiableList(header),
                Collections.unmodifiableList(detail),
                firstAirDate,
                rating,
                ratedAt,
                Collections.unmodifiableList(directors),
                Collections.unmodifiableList(actors),
                overview,
                comment,
                imageFilename);
    }

    // -------------------------------------------------------------------------
    // Episode
    // -------------------------------------------------------------------------

    /**
     * Factory für bewertete Episoden.
     *
     * Das Format hängt von den Flags ab:
     *
     * rated_season=1 (Staffelbewertung):
     *   Header: "Show Name / Show German Name"
     *           "Season Name (SeasonFirstAirYear)"
     *   Detail: "Season: X"
     *   sortDate: season release_date
     *
     * Einzelepisode (rated_season=0):
     *   Header: "Show Name / Show German Name"
     *           "Episode Name / Episode German Name (EpisodeAirYear)"
     *   Detail: "Episode: S01E05"
     *   sortDate: episode_release_date
     *
     * Wenn Show-Name = Show-German-Name, kein " / German Name" im Header.
     * Analog für Episode-Name.
     */
    public static CardData forEpisode(
            int id,
            String showName,
            String showGermanName,
            String seasonName,
            String seasonGermanName,
            String episodeName,
            String episodeGermanName,
            int seasonNumber,
            int episodeNumber,
            LocalDate seasonFirstAirDate,
            LocalDate episodeAirDate,
            boolean ratedSeason,
            int rating,
            LocalDate ratedAt,
            List<String> directors,
            List<String> actors,
            String overview,
            String comment,
            String imageFilename) {

        List<String> header = new ArrayList<>();
        List<String> detail = new ArrayList<>();

        // Zeile 1: Show-Name, ggf. mit deutschem Namen
        String showLine = showName;
        if (showGermanName != null && !showGermanName.equals(showName)) {
            showLine = showName + " / " + showGermanName;
        }
        header.add(showLine);

        if (ratedSeason) {
            // Staffelbewertung
            String seasonLine = seasonName != null ? seasonName : "Season " + seasonNumber;
            if (seasonGermanName != null && !seasonGermanName.equals(seasonName)) {
                seasonLine = seasonLine + " / " + seasonGermanName;
            }
            String seasonYear = seasonFirstAirDate != null
                    ? " (" + seasonFirstAirDate.getYear() + ")"
                    : "";
            header.add(seasonLine + seasonYear);
            detail.add("Season: " + seasonNumber);
        } else {
            // Einzelepisode
            String episodeLine = episodeName != null ? episodeName : "Episode " + episodeNumber;
            if (episodeGermanName != null && !episodeGermanName.equals(episodeName)) {
                episodeLine = episodeLine + " / " + episodeGermanName;
            }
            String episodeYear = episodeAirDate != null
                    ? " (" + episodeAirDate.getYear() + ")"
                    : "";
            header.add(episodeLine + episodeYear);
            detail.add("Episode: S" + String.format("%02d", seasonNumber)
                    + "E" + String.format("%02d", episodeNumber));
        }

        return new CardData(
                id,
                Collections.unmodifiableList(header),
                Collections.unmodifiableList(detail),
                ratedSeason ? seasonFirstAirDate : episodeAirDate,
                rating,
                ratedAt,
                Collections.unmodifiableList(directors),
                Collections.unmodifiableList(actors),
                overview,
                comment,
                imageFilename);
    }
}