package app.movie.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import app.movie.model.TvShowComparisonData;
import app.movie.model.json.CastJSON;
import app.movie.model.json.CrewJSON;
import app.movie.model.json.GenreJSON;
import app.movie.model.json.ProductionCountryJSON;
import app.movie.model.json.RoleJSON;
import app.movie.model.json.SpokenLanguageJSON;
import app.movie.model.json.TvShowJSON;
import app.movie.model.json.TvShowRatingJSON;
import app.shared.DB;

/**
 * Repository für alle Datenbankoperationen rund um Serien in der TMDB-Datenbank.
 * Kein Netzwerkzugriff, kein Dateisystemzugriff — nur DB.
 */
public class TvShowRepository {

    private static final Logger log = Logger.getLogger(TvShowRepository.class.getName());

        

    // -------------------------------------------------------------------------
    // Insert
    // -------------------------------------------------------------------------

    public void insertTvShow(TvShowJSON show, Connection conn) {
        log.fine("insertTvShow, tvShowId " + show.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tv_show (id, name, original_name, german_name, first_air_date, " +
                "last_air_date, overview, backdrop_path, original_language, poster_path, " +
                "number_of_episodes, episode_run_time, number_of_seasons, type, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, show.id);
            ps.setString(2, show.name);
            ps.setString(3, show.original_name);
            ps.setString(4, show.german_name);
            ps.setString(5, show.first_air_date == null ? null : show.first_air_date.toString());
            ps.setString(6, show.last_air_date == null ? null : show.last_air_date.toString());
            ps.setString(7, show.overview);
            ps.setString(8, show.backdrop_path);
            ps.setString(9, show.original_language);
            ps.setString(10, show.poster_path);
            ps.setInt(11, show.number_of_episodes == null ? 0 : show.number_of_episodes);
            String episodeRuntime = show.episode_run_time == null ? null : show.episode_run_time.toString();
            if (episodeRuntime != null && episodeRuntime.length() > 2)
                episodeRuntime = episodeRuntime.substring(1, episodeRuntime.length() - 1);
            ps.setString(12, episodeRuntime);
            ps.setInt(13, show.number_of_seasons == null ? 0 : show.number_of_seasons);
            ps.setString(14, show.type);
            ps.setString(15, show.status);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertTvShow fehlgeschlagen. tvShowId: " + show.id, e);
        }
    }

    public void insertTvShowRating(TvShowRatingJSON rating, String comment, Connection conn) {
        log.fine("insertTvShowRating, tvShowId " + rating.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tv_show_rating (tv_show_id, ar_value, comment, first_rated_at) " +
                "VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, rating.id);
            ps.setInt(2, rating.account_rating.value);
            ps.setString(3, comment);
            ps.setString(4, rating.account_rating.getCreated_at().toString());
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertTvShowRating fehlgeschlagen. tvShowId: " + rating.id, e);
        }
    }

    public void insertTvShowImage(TvShowJSON show, int width, int height,
            String filename, Connection conn) {
        log.fine("insertTvShowImage, tvShowId " + show.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tv_show_image (tv_show_id, type, width, height, language, " +
                "original_name, filename) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, show.id);
            ps.setString(2, "poster");
            ps.setInt(3, width);
            ps.setInt(4, height);
            ps.setString(5, "en-US");
            ps.setString(6, show.poster_path.substring(1));
            ps.setString(7, filename);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertTvShowImage fehlgeschlagen. tvShowId: " + show.id, e);
        }
    }

    /**
     * Fügt Cast-Einträge in tv_show_to_person ein.
     * Aggregierte Credits: Ein Cast-Eintrag kann mehrere Rollen haben.
     */
    public void insertTvShowCast(CastJSON cast, int tvShowId, Connection conn) {
        log.fine("insertTvShowCast, tvShowId " + tvShowId + ", personId " + cast.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO tv_show_to_person (tv_show_id, person_id, credit_id, " +
                "character, \"order\", department, job, episode_count) " +
                "VALUES (?, ?, ?, ?, ?, NULL, NULL, ?)")) {
            for (RoleJSON role : cast.getRoles()) {
                ps.setInt(1, tvShowId);
                ps.setInt(2, cast.id);
                ps.setString(3, role.credit_id);
                ps.setString(4, role.character);
                ps.setInt(5, cast.order);
                ps.setInt(6, role.episode_count);
                ps.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException("insertTvShowCast fehlgeschlagen. tvShowId: " + tvShowId
                    + ", personId: " + cast.id, e);
        }
    }

    /**
     * Fügt einen Crew-Eintrag in tv_show_to_person ein.
     * Aggregierte Credits: Ein Crew-Eintrag kann mehrere Jobs haben.
     * Die Filterentscheidung liegt beim Aufrufer (Importer).
     */
    public void insertTvShowCrew(CrewJSON crew, int tvShowId, String job,
            String creditId, int episodeCount, Connection conn) {
        log.fine("insertTvShowCrew, tvShowId " + tvShowId + ", personId " + crew.id + ", job " + job);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO tv_show_to_person (tv_show_id, person_id, credit_id, " +
                "character, \"order\", department, job, episode_count) " +
                "VALUES (?, ?, ?, NULL, NULL, ?, ?, ?)")) {
            ps.setInt(1, tvShowId);
            ps.setInt(2, crew.id);
            ps.setString(3, creditId);
            ps.setString(4, crew.department);
            ps.setString(5, job);
            ps.setInt(6, episodeCount);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertTvShowCrew fehlgeschlagen. tvShowId: " + tvShowId
                    + ", personId: " + crew.id + ", job: " + job, e);
        }
    }

    public void insertTvShowGenres(TvShowJSON show, Connection conn) {
        log.fine("insertTvShowGenres, tvShowId " + show.id);
        try (PreparedStatement psGenre = conn.prepareStatement(
                    "INSERT OR IGNORE INTO genre (id, name) VALUES (?, ?)");
             PreparedStatement psLink = conn.prepareStatement(
                    "INSERT OR IGNORE INTO tv_show_to_genre (tv_show_id, genre_id) VALUES (?, ?)")) {
            for (GenreJSON genre : show.genres) {
                psGenre.setInt(1, genre.id);
                psGenre.setString(2, genre.name);
                psGenre.execute();
                psLink.setInt(1, show.id);
                psLink.setInt(2, genre.id);
                psLink.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException("insertTvShowGenres fehlgeschlagen. tvShowId: " + show.id, e);
        }
    }

    public void insertTvShowCountries(TvShowJSON show, Connection conn) {
        log.fine("insertTvShowCountries, tvShowId " + show.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO tv_show_to_country (tv_show_id, iso_3166_1) VALUES (?, ?)")) {
            for (ProductionCountryJSON country : show.production_countries) {
                ps.setInt(1, show.id);
                ps.setString(2, country.iso_3166_1);
                ps.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException("insertTvShowCountries fehlgeschlagen. tvShowId: " + show.id, e);
        }
    }

    public void insertTvShowLanguages(TvShowJSON show, Connection conn) {
        log.fine("insertTvShowLanguages, tvShowId " + show.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO tv_show_to_language (tv_show_id, iso_639_1) VALUES (?, ?)")) {
            for (SpokenLanguageJSON language : show.spoken_languages) {
                ps.setInt(1, show.id);
                ps.setString(2, language.iso_639_1);
                ps.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException("insertTvShowLanguages fehlgeschlagen. tvShowId: " + show.id, e);
        }
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public Integer getTvShowRating(int tvShowId) {
        log.fine("getTvShowRating, tvShowId " + tvShowId);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT ar_value FROM tv_show_rating WHERE tv_show_id = ?")) {
            ps.setInt(1, tvShowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("ar_value");
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("getTvShowRating fehlgeschlagen. tvShowId: " + tvShowId, e);
        }
    }

    public String getTvShowComment(int tvShowId) {
        log.fine("getTvShowComment, tvShowId " + tvShowId);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT comment FROM tv_show_rating WHERE tv_show_id = ?")) {
            ps.setInt(1, tvShowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString("comment");
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("getTvShowComment fehlgeschlagen. tvShowId: " + tvShowId, e);
        }
    }

    public boolean tvShowExists(int tvShowId) {
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT 1 FROM tv_show WHERE id = ?")) {
            ps.setInt(1, tvShowId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException("tvShowExists fehlgeschlagen. tvShowId: " + tvShowId, e);
        }
    }

    /**
     * Lädt die Vergleichsdaten einer Serie für den Daten-Check.
     * Gibt null zurück wenn die Serie nicht in der DB ist.
     */
    public TvShowComparisonData loadComparisonData(int tvShowId) {
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT number_of_seasons, number_of_episodes, last_air_date, status " +
                "FROM tv_show WHERE id = ?")) {
            ps.setInt(1, tvShowId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return null;
                return new TvShowComparisonData(
                        rs.getInt("number_of_seasons"),
                        rs.getInt("number_of_episodes"),
                        rs.getString("last_air_date"),
                        rs.getString("status"));
            }
        } catch (Exception e) {
            throw new RuntimeException("loadComparisonData fehlgeschlagen. tvShowId: " + tvShowId, e);
        }
    }
    
    public List<String> loadDotCommentTitles() {
        List<String> titles = new ArrayList<>();
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT tv.name, tv.german_name, tr.first_rated_at " +
                "FROM tv_show_rating tr " +
                "JOIN tv_show tv ON tv.id = tr.tv_show_id " +
                "WHERE tr.comment = '.' " +
                "ORDER BY tr.first_rated_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                titles.add(rs.getString("name") + " (" + rs.getString("german_name") + ") — " + rs.getString("first_rated_at"));
        } catch (Exception e) {
            throw new RuntimeException("loadDotCommentTitles fehlgeschlagen", e);
        }
        return titles;
    }
    
    public List<Integer> getShowsWithoutOverview() {
    	String sql =
                "SELECT ts.id FROM tv_show ts "
                + "JOIN tv_show_rating tsr ON ts.id = tsr.tv_show_id "
                + "WHERE ts.overview IS NULL OR ts.overview = ''";
        List<Integer> result = new ArrayList<>();
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                result.add(rs.getInt(1));
        } catch (Exception e) {
            throw new RuntimeException("getShowsWithoutOverview fehlgeschlagen. sql: " + sql, e);
        }
        return result;
    }
    
    public List<Integer> getShowsWithoutPoster() {
        String sql =
                "SELECT ts.id FROM tv_show ts "
                + "JOIN tv_show_rating tsr ON ts.id = tsr.tv_show_id "
                + "WHERE NOT EXISTS (SELECT 1 FROM tv_show_image tsi WHERE tsi.tv_show_id = ts.id)";
        List<Integer> result = new ArrayList<>();
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                result.add(rs.getInt(1));
        } catch (Exception e) {
            throw new RuntimeException("getShowsWithoutPoster fehlgeschlagen. sql: " + sql, e);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    public void updateTvShowRating(TvShowRatingJSON rating, String comment) {
        log.fine("updateTvShowRating, tvShowId " + rating.id);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "UPDATE tv_show_rating SET ar_value = ?, comment = ?, last_updated_at = ? " +
                "WHERE tv_show_id = ?")) {
            ps.setInt(1, rating.account_rating.value);
            ps.setString(2, comment == null || comment.isEmpty() ? "." : comment);
            ps.setString(3, java.time.LocalDateTime.now().toString());
            ps.setInt(4, rating.id);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("updateTvShowRating fehlgeschlagen. tvShowId: " + rating.id, e);
        }
    }

    /**
     * Aktualisiert die sich ändernden Felder einer Serie.
     */
    public void updateTvShowData(TvShowJSON show) {
        log.info("updateTvShowData, tvShowId " + show.id);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "UPDATE tv_show SET number_of_seasons = ?, number_of_episodes = ?, " +
                "last_air_date = ?, status = ? WHERE id = ?")) {
            ps.setInt(1, show.number_of_seasons == null ? 0 : show.number_of_seasons);
            ps.setInt(2, show.number_of_episodes == null ? 0 : show.number_of_episodes);
            ps.setString(3, show.last_air_date == null ? null : show.last_air_date.toString());
            ps.setString(4, show.status);
            ps.setInt(5, show.id);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("updateTvShowData fehlgeschlagen. tvShowId: " + show.id, e);
        }
    }
    
    public void updateOverview(int tvShowId, String overview) {
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "UPDATE tv_show SET overview = ? WHERE id = ?")) {
            ps.setString(1, overview);
            ps.setInt(2, tvShowId);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("updateOverview fehlgeschlagen. tvShowId: " + tvShowId, e);
        }
    }

    public void updatePosterPath(int tvShowId, String posterPath) {
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "UPDATE tv_show SET poster_path = ? WHERE id = ?")) {
            ps.setString(1, posterPath);
            ps.setInt(2, tvShowId);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("updatePosterPath fehlgeschlagen. tvShowId: " + tvShowId, e);
        }
    }

    public void insertTvShowImage(int tvShowId, String posterPath, int width, int height,
            String language, String filename) {
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "INSERT INTO tv_show_image (tv_show_id, type, width, height, language, " +
                "original_name, filename) VALUES (?, 'poster', ?, ?, ?, ?, ?)")) {
            ps.setInt(1, tvShowId);
            ps.setInt(2, width);
            ps.setInt(3, height);
            ps.setString(4, language);
            ps.setString(5, posterPath.startsWith("/") ? posterPath.substring(1) : posterPath);
            ps.setString(6, filename);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertTvShowImage fehlgeschlagen. tvShowId: " + tvShowId, e);
        }
    }
}