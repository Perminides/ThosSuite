package app.data.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import app.tmdb.json.CastJSON;
import app.tmdb.json.CrewJSON;
import app.tmdb.json.EpisodeJSON;

/**
 * Repository für alle Datenbankoperationen rund um Episoden in der TMDB-Datenbank.
 * Kein Netzwerkzugriff, kein Dateisystemzugriff — nur DB.
 */
public class TmdbEpisodeRepository {

    private static final Logger log = Logger.getLogger(TmdbEpisodeRepository.class.getName());
    public record EpisodeForApi(int tvShowId, int seasonNumber, int episodeNumber) {} 

    // -------------------------------------------------------------------------
    // Insert
    // -------------------------------------------------------------------------

    public void insertEpisode(EpisodeJSON episode, Connection conn) {
        log.fine("insertEpisode, episodeId " + episode.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO episode (id, tv_show_id, episode_number, season_number, " +
                "release_date, name, german_name, original_name, overview, runtime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, episode.id);
            ps.setInt(2, episode.show_id);
            ps.setInt(3, episode.episode_number);
            ps.setInt(4, episode.season_number);
            ps.setString(5, episode.air_date == null ? null : episode.air_date.toString());
            ps.setString(6, episode.name);
            ps.setString(7, episode.german_name);
            ps.setString(8, null);
            ps.setString(9, episode.overview);
            ps.setInt(10, episode.runtime == null ? 0 : episode.runtime);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertEpisode fehlgeschlagen. episodeId: " + episode.id, e);
        }
    }

    /**
     * Fügt ein Episoden-Rating mit Flag und Kommentar ein.
     *
     * @param episodeId    TMDB-ID der Episode
     * @param rating       Bewertungswert
     * @param comment      Kommentar oder "."
     * @param firstRatedAt Zeitpunkt der ersten Bewertung als String
     * @param ratedSeason  Flag: Staffelbewertung?
     * @param conn         Transaktions-Connection
     */
    public void insertEpisodeRating(int episodeId, int rating, String comment,
            String firstRatedAt, Boolean ratedSeason, Connection conn) {
        log.fine("insertEpisodeRating, episodeId " + episodeId);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO episode_rating (episode_id, ar_value, comment, first_rated_at, " +
                "rated_season) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, episodeId);
            ps.setInt(2, rating);
            ps.setString(3, comment);
            ps.setString(4, firstRatedAt);
            if (ratedSeason == null) ps.setNull(5, java.sql.Types.INTEGER);
            else ps.setInt(5, ratedSeason ? 1 : 0);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertEpisodeRating fehlgeschlagen. episodeId: " + episodeId, e);
        }
    }

    /**
     * Fügt Guest-Stars (Cast) in episode_to_person ein.
     */
    public void insertEpisodeCast(CastJSON cast, int episodeId, Connection conn) {
        log.fine("insertEpisodeCast, episodeId " + episodeId + ", personId " + cast.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO episode_to_person (episode_id, person_id, credit_id, " +
                "character, \"order\", department, job) " +
                "VALUES (?, ?, ?, ?, ?, NULL, NULL)")) {
            ps.setInt(1, episodeId);
            ps.setInt(2, cast.id);
            ps.setString(3, cast.getCredit_id());
            ps.setString(4, cast.getCharacter());
            ps.setObject(5, cast.order);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertEpisodeCast fehlgeschlagen. episodeId: " + episodeId
                    + ", personId: " + cast.id, e);
        }
    }

    /**
     * Fügt einen Crew-Eintrag in episode_to_person ein.
     * Die Filterentscheidung liegt beim Aufrufer (Importer).
     */
    public void insertEpisodeCrew(CrewJSON crew, int episodeId, Connection conn) {
        log.fine("insertEpisodeCrew, episodeId " + episodeId + ", personId " + crew.id + ", job " + crew.getJob());
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO episode_to_person (episode_id, person_id, credit_id, " +
                "character, \"order\", department, job) " +
                "VALUES (?, ?, ?, NULL, NULL, ?, ?)")) {
            ps.setInt(1, episodeId);
            ps.setInt(2, crew.id);
            ps.setString(3, crew.getCredit_id());
            ps.setString(4, crew.department);
            ps.setString(5, crew.getJob());
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertEpisodeCrew fehlgeschlagen. episodeId: " + episodeId
                    + ", personId: " + crew.id + ", job: " + crew.getJob(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public Integer getEpisodeRating(int episodeId) {
        log.fine("getEpisodeRating, episodeId " + episodeId);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT ar_value FROM episode_rating WHERE episode_id = ?")) {
            ps.setInt(1, episodeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("ar_value");
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("getEpisodeRating fehlgeschlagen. episodeId: " + episodeId, e);
        }
    }

    public String getEpisodeComment(int episodeId) {
        log.fine("getEpisodeComment, episodeId " + episodeId);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT comment FROM episode_rating WHERE episode_id = ?")) {
            ps.setInt(1, episodeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString("comment");
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("getEpisodeComment fehlgeschlagen. episodeId: " + episodeId, e);
        }
    }
    
    public List<EpisodeForApi> getEpisodesWithoutOverview() {
    	String sql =
                "SELECT tv_show_id, season_number, episode_number FROM episode "
                + "WHERE overview IS NULL OR overview = ''";
        List<EpisodeForApi> result = new ArrayList<>();
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
            	result.add(new EpisodeForApi(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
        } catch (Exception e) {
            throw new RuntimeException("getEpisodesWithoutOverview fehlgeschlagen. sql: " + sql, e);
        }
        return result;
    }
    
    public List<String> loadDotCommentTitles() {
        List<String> titles = new ArrayList<>();
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT ep.name, ep.german_name, er.first_rated_at " +
                "FROM episode_rating er " +
                "JOIN episode ep ON ep.id = er.episode_id " +
                "WHERE er.comment = '.' " +
                "ORDER BY er.first_rated_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                titles.add(rs.getString("name") + " (" + rs.getString("german_name") + ") — " + rs.getString("first_rated_at"));
        } catch (Exception e) {
            throw new RuntimeException("loadDotCommentTitles fehlgeschlagen", e);
        }
        return titles;
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------
    
    public void updateEpisodeRating(int episodeId, int rating, String comment) {
        log.fine("updateEpisodeRating, episodeId " + episodeId);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "UPDATE episode_rating SET ar_value = ?, comment = ?, last_updated_at = ? " +
                "WHERE episode_id = ?")) {
            ps.setInt(1, rating);
            ps.setString(2, comment == null || comment.isEmpty() ? "." : comment);
            ps.setString(3, java.time.LocalDateTime.now().toString());
            ps.setInt(4, episodeId);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("updateEpisodeRating fehlgeschlagen. episodeId: " + episodeId, e);
        }
    }

    /**
     * Aktualisiert das rated_season-Flag einer Episodenbewertung.
     */
    public void updateEpisodeFlags(int episodeId, Boolean ratedSeason) {
        log.fine("updateEpisodeFlags, episodeId " + episodeId);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "UPDATE episode_rating SET rated_season = ? WHERE episode_id = ?")) {
            if (ratedSeason == null) ps.setNull(1, java.sql.Types.INTEGER);
            else ps.setInt(1, ratedSeason ? 1 : 0);
            ps.setInt(2, episodeId);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("updateEpisodeFlags fehlgeschlagen. episodeId: " + episodeId, e);
        }
    }

    /**
     * Aktualisiert die Flags einer Episodenbewertung.
     */
    public void updateEpisodeFlags(int episodeId, Boolean ratedSeason,
            Boolean actorsFromShow, Boolean directorsFromShow) {
        log.fine("updateEpisodeFlags, episodeId " + episodeId);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "UPDATE episode_rating SET rated_season = ?, actors_from_show = ?, " +
                "directors_from_show = ? WHERE episode_id = ?")) {
            if (ratedSeason == null) ps.setNull(1, java.sql.Types.INTEGER);
            else ps.setInt(1, ratedSeason ? 1 : 0);
            if (actorsFromShow == null) ps.setNull(2, java.sql.Types.INTEGER);
            else ps.setInt(2, actorsFromShow ? 1 : 0);
            if (directorsFromShow == null) ps.setNull(3, java.sql.Types.INTEGER);
            else ps.setInt(3, directorsFromShow ? 1 : 0);
            ps.setInt(4, episodeId);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("updateEpisodeFlags fehlgeschlagen. episodeId: " + episodeId, e);
        }
    }
    
    public void updateOverview(int episodeId, String overview) {
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "UPDATE episode SET overview = ? WHERE id = ?")) {
            ps.setString(1, overview);
            ps.setInt(2, episodeId);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("updateOverview fehlgeschlagen. episodeId: " + episodeId, e);
        }
    }
}