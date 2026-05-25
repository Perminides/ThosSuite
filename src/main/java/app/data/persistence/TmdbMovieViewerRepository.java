package app.data.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import app.config.Config;
import app.tmdb.MovieCardData;

/**
 * Repository für Leseoperationen des MovieViewers.
 * 
 * Getrennt vom TmdbMovieRepository (Import-Operationen), weil die Zuständigkeiten
 * klar unterschiedlich sind: Import schreibt mit Transaktions-Connections,
 * der Viewer liest nur über die Singleton-Connection.
 * 
 * Nutzt die Views movie_details, movie_actors, movie_directors für saubere Queries.
 */
public class TmdbMovieViewerRepository {

    // -------------------------------------------------------------------------
    // Film-Suche
    // -------------------------------------------------------------------------

    /**
     * Lädt alle bewerteten Filme eines Regisseurs, sortiert nach Erscheinungsdatum absteigend.
     */
    public List<MovieCardData> loadMoviesByDirector(String directorName) {
        String sql = "SELECT mdet.* FROM movie_details mdet "
                + "JOIN movie_directors mdir ON mdet.id = mdir.id "
                + "WHERE mdir.name = ? "
                + "ORDER BY mdet.release_date DESC";
        return loadMovies(sql, directorName);
    }

    /**
     * Lädt alle bewerteten Filme eines Schauspielers, sortiert nach Erscheinungsdatum absteigend.
     */
    public List<MovieCardData> loadMoviesByActor(String actorName) {
        String sql = "SELECT mdet.* FROM movie_details mdet "
                + "JOIN movie_actors mact ON mdet.id = mact.id "
                + "WHERE mact.name = ? "
                + "ORDER BY mdet.release_date DESC";
        return loadMovies(sql, actorName);
    }

    /**
     * Lädt alle bewerteten Filme mit einem bestimmten Titel, sortiert nach Erscheinungsdatum absteigend.
     * Sucht in title, german_title und original_title.
     */
    public List<MovieCardData> loadMoviesByTitle(String title) {
        String sql = "SELECT mdet.* FROM movie_details mdet "
                + "WHERE mdet.title = ? OR mdet.german_title = ? OR mdet.original_title = ? "
                + "ORDER BY mdet.release_date DESC";
        Connection conn = DB.getTmdbConnection();
        List<MovieCardData> result = new ArrayList<>();
        int actorsToShow = Config.getInt("tmdb.actorsToShow", 10);
        int directorsToShow = Config.getInt("tmdb.directorsToShow", 5);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, title);
            ps.setString(3, title);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(buildMovieCardData(rs, conn, actorsToShow, directorsToShow));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("loadMoviesByTitle fehlgeschlagen. title: " + title, e);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // SWYT-Vorschlagslisten (einmal beim Öffnen des Viewers vorladen)
    // -------------------------------------------------------------------------

    /**
     * Lädt alle Regisseur-Namen aus bewerteten Filmen, sortiert nach Häufigkeit absteigend.
     * Bei gleicher Häufigkeit alphabetisch.
     */
    public List<String> loadAllDirectorNames() {
        String sql = "SELECT name, COUNT(*) AS cnt "
                + "FROM movie_directors "
                + "GROUP BY name "
                + "ORDER BY cnt DESC, name ASC";
        return loadNameList(sql);
    }

    /**
     * Lädt alle Schauspieler-Namen aus bewerteten Filmen, sortiert nach Häufigkeit absteigend.
     * Bei gleicher Häufigkeit alphabetisch.
     */
    public List<String> loadAllActorNames() {
        String sql = "SELECT name, COUNT(*) AS cnt "
                + "FROM movie_actors "
                + "GROUP BY name "
                + "ORDER BY cnt DESC, name ASC";
        return loadNameList(sql);
    }

    /**
     * Lädt alle Filmtitel (title, german_title, original_title) aus bewerteten Filmen,
     * sortiert nach Bewertung absteigend, dann nach Titel alphabetisch.
     * 
     * Liefert eine deduplizierte Liste: Jeder Titel erscheint nur einmal,
     * auch wenn er als title UND german_title vorkommt.
     */
    public List<String> loadAllTitles() {
        // Drei Varianten des Titels, alle in eine sortierte Liste,
        // dann deduplizieren. ORDER BY rating DESC sorgt dafür, dass
        // bei Duplikaten der höher bewertete Eintrag überlebt.
        String sql = "SELECT DISTINCT title_variant FROM ("
                + "SELECT title AS title_variant, rating FROM movie_details "
                + "UNION "
                + "SELECT german_title AS title_variant, rating FROM movie_details WHERE german_title IS NOT NULL "
                + "UNION "
                + "SELECT original_title AS title_variant, rating FROM movie_details WHERE original_title IS NOT NULL"
                + ") ORDER BY rating DESC, title_variant ASC";
        return loadNameList(sql);
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    private List<MovieCardData> loadMovies(String sql, String paramName) {
        Connection conn = DB.getTmdbConnection();
        List<MovieCardData> result = new ArrayList<>();
        int actorsToShow = Config.getInt("tmdb.actorsToShow", 10);
        int directorsToShow = Config.getInt("tmdb.directorsToShow", 5);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paramName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(buildMovieCardData(rs, conn, actorsToShow, directorsToShow));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("loadMovies fehlgeschlagen. sql: " + sql + ", param: " + paramName, e);
        }
        return result;
    }

    /**
     * Baut ein MovieCardData aus einer Zeile des movie_details-Views.
     * Lädt Actors und Directors in separaten Queries (wie Geosuite V2).
     */
    private MovieCardData buildMovieCardData(ResultSet detailsRow, Connection conn,
            int actorsToShow, int directorsToShow) {
        try {
            int movieId = detailsRow.getInt("id");

            List<String> actors = loadPersonNames(conn,
                    "SELECT name FROM movie_actors WHERE id = ? ORDER BY position ASC",
                    movieId, actorsToShow);

            List<String> directors = loadPersonNames(conn,
                    "SELECT name FROM movie_directors WHERE id = ? ORDER BY name ASC",
                    movieId, directorsToShow);

            String releaseDateStr = detailsRow.getString("release_date");
            java.time.LocalDate releaseDate = releaseDateStr != null
                    ? java.time.LocalDate.parse(releaseDateStr) : null;

            return MovieCardData.forMovie(
                    movieId,
                    detailsRow.getString("title"),
                    detailsRow.getString("german_title"),
                    detailsRow.getString("original_title"),
                    releaseDate,
                    detailsRow.getInt("rating"),
                    detailsRow.getString("rated"),
                    directors,
                    actors,
                    detailsRow.getString("overview"),
                    detailsRow.getString("comment"),
                    detailsRow.getString("image_filename"));

        } catch (Exception e) {
            throw new RuntimeException("buildMovieCardData fehlgeschlagen", e);
        }
    }

    private List<String> loadPersonNames(Connection conn, String sql, int movieId, int limit) {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next() && count < limit) {
                    names.add(rs.getString("name"));
                    count++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("loadPersonNames fehlgeschlagen. movieId: " + movieId, e);
        }
        return names;
    }

    private List<String> loadNameList(String sql) {
        Connection conn = DB.getTmdbConnection();
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        } catch (Exception e) {
            throw new RuntimeException("loadNameList fehlgeschlagen. sql: " + sql, e);
        }
        return names;
    }
}