package app.movie.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import app.shared.Config;
import app.shared.DB;
import app.shared.model.CardData;

/**
 * Repository für Leseoperationen des Viewers.
 *
 * Liefert CardData für Filme, Serien und Episoden über die bestehenden
 * Detail-Views (movie_details, tv_show_details, episode_details) und
 * die Actor/Director-Views.
 *
 * Nutzt die SWYT-Views (all_directors, all_actors, all_titles) für
 * die Vorschlagslisten.
 *
 * Nur Leseoperationen über die Singleton-Connection.
 */
public class MovieViewerRepository {

    private final int actorsToShow = Config.getInt("tmdb.actorsToShow", 10);
    private final int directorsToShow = Config.getInt("tmdb.directorsToShow", 5);

    // -------------------------------------------------------------------------
    // Suche nach Director
    // -------------------------------------------------------------------------

    public List<CardData> loadByDirector(String directorName) {
        List<CardData> result = new ArrayList<>();
        result.addAll(loadMoviesByDirector(directorName));
        result.addAll(loadTvShowsByDirector(directorName));
        result.addAll(loadEpisodesByDirector(directorName));
        result.sort(Comparator.comparing(CardData::sortDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private List<CardData> loadMoviesByDirector(String name) {
        String sql = "SELECT distinct mdet.* FROM movie_details mdet "
                + "JOIN movie_directors mdir ON mdet.id = mdir.id "
                + "WHERE mdir.name = ? "
                + "ORDER BY mdet.release_date DESC";
        return loadMovies(sql, name);
    }

    private List<CardData> loadTvShowsByDirector(String name) {
        String sql = "SELECT distinct sdet.* FROM tv_show_details sdet "
                + "JOIN tv_show_directors sdir ON sdet.id = sdir.id "
                + "WHERE sdir.name = ? "
                + "ORDER BY sdet.first_air_date DESC";
        return loadTvShows(sql, name);
    }

    private List<CardData> loadEpisodesByDirector(String name) {
        String sql = "SELECT distinct edet.* FROM episode_details edet "
                + "JOIN episode_directors edir ON edet.episode_id = edir.id "
                + "WHERE edir.name = ? "
                + "ORDER BY edet.episode_release_date DESC";
        return loadEpisodes(sql, name);
    }

    // -------------------------------------------------------------------------
    // Suche nach Actor
    // -------------------------------------------------------------------------

    public List<CardData> loadByActor(String actorName) {
        List<CardData> result = new ArrayList<>();
        result.addAll(loadMoviesByActor(actorName));
        result.addAll(loadTvShowsByActor(actorName));
        result.addAll(loadEpisodesByActor(actorName));
        result.sort(Comparator.comparing(CardData::sortDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private List<CardData> loadMoviesByActor(String name) {
        String sql = "SELECT distinct mdet.* FROM movie_details mdet "
                + "JOIN movie_actors mact ON mdet.id = mact.id "
                + "WHERE mact.name = ? "
                + "ORDER BY mdet.release_date DESC";
        return loadMovies(sql, name);
    }

    private List<CardData> loadTvShowsByActor(String name) {
        String sql = "SELECT distinct sdet.* FROM tv_show_details sdet "
                + "JOIN tv_show_actors sact ON sdet.id = sact.id "
                + "WHERE sact.name = ? "
                + "ORDER BY sdet.first_air_date DESC";
        return loadTvShows(sql, name);
    }

    private List<CardData> loadEpisodesByActor(String name) {
        String sql = "SELECT distinct edet.* FROM episode_details edet "
                + "JOIN episode_actors eact ON edet.episode_id = eact.id "
                + "WHERE eact.name = ? "
                + "ORDER BY edet.episode_release_date DESC";
        return loadEpisodes(sql, name);
    }

    // -------------------------------------------------------------------------
    // Suche nach Titel
    // -------------------------------------------------------------------------

    public List<CardData> loadByTitle(String title) {
        List<CardData> result = new ArrayList<>();
        result.addAll(loadMoviesByTitle(title));
        result.addAll(loadTvShowsByTitle(title));
        result.addAll(loadEpisodesByTitle(title));
        result.sort(Comparator.comparing(CardData::sortDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private List<CardData> loadMoviesByTitle(String title) {
        String sql = "SELECT mdet.* FROM movie_details mdet "
                + "WHERE mdet.title = ? OR mdet.german_title = ? OR mdet.original_title = ? "
                + "ORDER BY mdet.release_date DESC";
        Connection conn = DB.getTmdbConnection();
        List<CardData> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, title);
            ps.setString(3, title);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(buildMovieCard(rs, conn));
            }
        } catch (Exception e) {
            throw new RuntimeException("loadMoviesByTitle fehlgeschlagen. title: " + title, e);
        }
        return result;
    }

    private List<CardData> loadTvShowsByTitle(String title) {
        String sql = "SELECT sdet.* FROM tv_show_details sdet "
                + "WHERE sdet.name = ? OR sdet.german_name = ? "
                + "ORDER BY sdet.first_air_date DESC";
        Connection conn = DB.getTmdbConnection();
        List<CardData> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, title);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(buildTvShowCard(rs, conn));
            }
        } catch (Exception e) {
            throw new RuntimeException("loadTvShowsByTitle fehlgeschlagen. title: " + title, e);
        }
        return result;
    }

    private List<CardData> loadEpisodesByTitle(String title) {
        String sql = "SELECT edet.* FROM episode_details edet "
                + "WHERE edet.show_name = ? OR edet.show_german_name = ? "
                + "OR edet.episode_name = ? OR edet.episode_german_name = ? "
                + "ORDER BY edet.episode_release_date DESC";
        Connection conn = DB.getTmdbConnection();
        List<CardData> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, title);
            ps.setString(3, title);
            ps.setString(4, title);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(buildEpisodeCard(rs, conn));
            }
        } catch (Exception e) {
            throw new RuntimeException("loadEpisodesByTitle fehlgeschlagen. title: " + title, e);
        }
        return result;
    }
    
    // -------------------------------------------------------------------------
    // Alle Episoden
    // -------------------------------------------------------------------------
    
    public List<CardData> loadAllEpisodes() {
        String sql = "SELECT edet.* FROM episode_details edet "
                + "ORDER BY edet.episode_release_date DESC";
        return loadEpisodes(sql);
    }

    // -------------------------------------------------------------------------
    // SWYT-Vorschlagslisten
    // -------------------------------------------------------------------------

    public List<String> loadAllDirectorNames() {
        return loadNameList("SELECT name FROM all_directors ORDER BY cnt DESC, name ASC");
    }

    public List<String> loadAllActorNames() {
        return loadNameList("SELECT name FROM all_actors ORDER BY cnt DESC, name ASC");
    }

    public List<String> loadAllTitles() {
        return loadNameList("SELECT title FROM all_titles ORDER BY rating DESC, title ASC");
    }

    // -------------------------------------------------------------------------
    // CardData-Builder
    // -------------------------------------------------------------------------

    private CardData buildMovieCard(ResultSet rs, Connection conn) {
        try {
            int movieId = rs.getInt("id");

            List<String> actors = loadPersonNames(conn,
                    "SELECT name FROM movie_actors WHERE id = ? ORDER BY position ASC",
                    movieId, actorsToShow);

            List<String> directors = loadPersonNames(conn,
                    "SELECT name FROM movie_directors WHERE id = ? ORDER BY name ASC",
                    movieId, directorsToShow);

            String releaseDateStr = rs.getString("release_date");
            LocalDate releaseDate = releaseDateStr != null ? LocalDate.parse(releaseDateStr) : null;

            return CardDataFactory.forMovie(
                    movieId,
                    rs.getString("title"),
                    rs.getString("german_title"),
                    rs.getString("original_title"),
                    releaseDate,
                    rs.getInt("rating"),
                    LocalDate.parse(rs.getString("rated").substring(0, 10)),
                    directors,
                    actors,
                    rs.getString("overview"),
                    rs.getString("comment"),
                    rs.getString("image_filename"));
        } catch (Exception e) {
            throw new RuntimeException("buildMovieCard fehlgeschlagen", e);
        }
    }

    private CardData buildTvShowCard(ResultSet rs, Connection conn) {
        try {
            int showId = rs.getInt("id");

            List<String> actors = loadPersonNames(conn,
                    "SELECT name FROM tv_show_actors WHERE id = ? ORDER BY position ASC",
                    showId, actorsToShow);

            List<String> directors = loadPersonNames(conn,
                    "SELECT name FROM tv_show_directors WHERE id = ? ORDER BY name ASC",
                    showId, directorsToShow);

            String firstAirDateStr = rs.getString("first_air_date");
            LocalDate firstAirDate = firstAirDateStr != null ? LocalDate.parse(firstAirDateStr) : null;
            String lastAirDateStr = rs.getString("last_air_date");
            LocalDate lastAirDate = lastAirDateStr != null ? LocalDate.parse(lastAirDateStr) : null;

            return CardDataFactory.forTvShow(
                    showId,
                    rs.getString("name"),
                    rs.getString("german_name"),
                    firstAirDate,
                    lastAirDate,
                    rs.getInt("number_of_seasons"),
                    rs.getInt("number_of_episodes"),
                    rs.getInt("rating"),
                    LocalDate.parse(rs.getString("rated").substring(0, 10)),
                    directors,
                    actors,
                    rs.getString("overview"),
                    rs.getString("comment"),
                    rs.getString("image_filename"));
        } catch (Exception e) {
            throw new RuntimeException("buildTvShowCard fehlgeschlagen", e);
        }
    }

    private CardData buildEpisodeCard(ResultSet rs, Connection conn) {
        try {
            int episodeId = rs.getInt("episode_id");
            boolean ratedSeason = rs.getInt("rated_season") == 1;

            List<String> actors = loadPersonNames(conn,
                    "SELECT name FROM episode_actors WHERE id = ? ORDER BY position ASC",
                    episodeId, actorsToShow);

            List<String> directors = loadPersonNames(conn,
                    "SELECT name FROM episode_directors WHERE id = ? ORDER BY position DESC",
                    episodeId, directorsToShow);

            String seasonReleaseDateStr = rs.getString("release_date");
            LocalDate seasonFirstAirDate = seasonReleaseDateStr != null
                    ? LocalDate.parse(seasonReleaseDateStr) : null;
            String episodeReleaseDateStr = rs.getString("episode_release_date");
            LocalDate episodeAirDate = episodeReleaseDateStr != null
                    ? LocalDate.parse(episodeReleaseDateStr) : null;

            return CardDataFactory.forEpisode(
                    episodeId,
                    rs.getString("show_name"),
                    rs.getString("show_german_name"),
                    rs.getString("season_name"),
                    rs.getString("season_german_name"),
                    rs.getString("episode_name"),
                    rs.getString("episode_german_name"),
                    rs.getInt("season_number"),
                    rs.getInt("episode_number"),
                    seasonFirstAirDate,
                    episodeAirDate,
                    ratedSeason,
                    rs.getInt("rating"),
                    LocalDate.parse(rs.getString("ratingInsertDate").substring(0, 10)),
                    directors,
                    actors,
                    rs.getString("overview"),
                    rs.getString("season_overview"),
                    rs.getString("show_overview"),
                    rs.getString("comment"),
                    rs.getString("season_image_filename"),
                    rs.getString("show_image_filename"));
        } catch (Exception e) {
            throw new RuntimeException("buildEpisodeCard fehlgeschlagen", e);
        }
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    private List<CardData> loadMovies(String sql, String paramName) {
        Connection conn = DB.getTmdbConnection();
        List<CardData> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paramName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(buildMovieCard(rs, conn));
            }
        } catch (Exception e) {
            throw new RuntimeException("loadMovies fehlgeschlagen. sql: " + sql + ", param: " + paramName, e);
        }
        return result;
    }

    private List<CardData> loadTvShows(String sql, String paramName) {
        Connection conn = DB.getTmdbConnection();
        List<CardData> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paramName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(buildTvShowCard(rs, conn));
            }
        } catch (Exception e) {
            throw new RuntimeException("loadTvShows fehlgeschlagen. sql: " + sql + ", param: " + paramName, e);
        }
        return result;
    }
    
    private List<CardData> loadEpisodes(String sql, String paramName) {
        Connection conn = DB.getTmdbConnection();
        List<CardData> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paramName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(buildEpisodeCard(rs, conn));
            }
        } catch (Exception e) {
            throw new RuntimeException("loadEpisodes fehlgeschlagen. sql: " + sql + ", param: " + paramName, e);
        }
        return result;
    }
    
    private List<CardData> loadEpisodes(String sql) {
        Connection conn = DB.getTmdbConnection();
        List<CardData> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                result.add(buildEpisodeCard(rs, conn));
        } catch (Exception e) {
            throw new RuntimeException("loadEpisodes fehlgeschlagen. sql: " + sql, e);
        }
        return result;
    }

    private List<String> loadPersonNames(Connection conn, String sql, int id, int limit) {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next() && count < limit) {
                    names.add(rs.getString("name"));
                    count++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("loadPersonNames fehlgeschlagen. id: " + id, e);
        }
        return names;
    }

    private List<String> loadNameList(String sql) {
        Connection conn = DB.getTmdbConnection();
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                names.add(rs.getString(1));
        } catch (Exception e) {
            throw new RuntimeException("loadNameList fehlgeschlagen. sql: " + sql, e);
        }
        return names;
    }
}