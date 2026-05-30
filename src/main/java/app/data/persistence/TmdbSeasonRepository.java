package app.data.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import app.tmdb.json.CastJSON;
import app.tmdb.json.CrewJSON;
import app.tmdb.json.RoleJSON;
import app.tmdb.json.SeasonJSON;

/**
* <p>Zum regular-Flag (markRegularCast/markRegularCrew) und der zugrunde
 * liegenden Invariante "regulär ⊆ aggregiert" siehe das Klassen-Javadoc von
 * {@code TmdbSeriesImporter}, Abschnitt "Season Regulars".
 */
public class TmdbSeasonRepository {

    private static final Logger log = Logger.getLogger(TmdbSeasonRepository.class.getName());

    // -------------------------------------------------------------------------
    // Insert
    // -------------------------------------------------------------------------

    public void insertSeason(SeasonJSON season, Connection conn) {
        log.fine("insertSeason, seasonId " + season.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO season (id, tv_show_id, season_number, number_of_episodes, " +
                "release_date, last_air_date, name, german_name, overview, poster_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, season.id);
            ps.setInt(2, season.tvShowID);
            ps.setInt(3, season.season_number);
            ps.setInt(4, season.episode_count == null ? 0 : season.episode_count);
            ps.setString(5, season.air_date == null ? null : season.air_date.toString());
            ps.setString(6, season.last_air_date == null ? null : season.last_air_date.toString());
            ps.setString(7, season.name);
            ps.setString(8, season.germanName);
            ps.setString(9, season.overview);
            ps.setString(10, season.poster_path);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertSeason fehlgeschlagen. seasonId: " + season.id, e);
        }
    }

    public void insertSeasonImage(SeasonJSON season, int width, int height,
            String filename, Connection conn) {
        log.fine("insertSeasonImage, seasonId " + season.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO season_image (season_id, type, width, language, " +
                "height, original_name, filename) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, season.id);
            ps.setString(2, "poster");
            ps.setInt(3, width);
            ps.setString(4, "en-US");
            ps.setInt(5, height);
            ps.setString(6, season.poster_path.substring(1));
            ps.setString(7, filename);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertSeasonImage fehlgeschlagen. seasonId: " + season.id, e);
        }
    }

    /**
     * Kopiert das Show-Image als Season-Image, wenn die Staffel kein eigenes Poster hat.
     */
    public void copyShowImageToSeason(SeasonJSON season, Connection conn) {
        log.fine("copyShowImageToSeason, seasonId " + season.id + ", tvShowId " + season.tvShowID);
        try (PreparedStatement psRead = DB.getTmdbConnection().prepareStatement(
                "SELECT type, width, height, original_name, filename FROM tv_show_image " +
                "WHERE tv_show_id = ? AND width = 154")) {
            psRead.setInt(1, season.tvShowID);
            try (ResultSet rs = psRead.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement psWrite = conn.prepareStatement(
                            "INSERT OR IGNORE INTO season_image (season_id, type, width, " +
                            "language, height, original_name, filename) " +
                            "VALUES (?, ?, ?, 'en-US', ?, ?, ?)")) {
                        psWrite.setInt(1, season.id);
                        psWrite.setString(2, rs.getString("type"));
                        psWrite.setInt(3, rs.getInt("width"));
                        psWrite.setInt(4, rs.getInt("height"));
                        psWrite.setString(5, rs.getString("original_name"));
                        psWrite.setString(6, rs.getString("filename"));
                        psWrite.execute();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("copyShowImageToSeason fehlgeschlagen. seasonId: " + season.id, e);
        }
    }

    /**
     * Fügt Cast-Einträge in season_to_person ein.
     * Aggregierte Credits: Ein Cast-Eintrag kann mehrere Rollen haben.
     */
    public void insertSeasonCast(CastJSON cast, SeasonJSON season, Connection conn) {
        log.fine("insertSeasonCast, seasonId " + season.id + ", personId " + cast.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO season_to_person (season_id, person_id, credit_id, " +
                "tv_show_id, season_number, character, \"order\", department, job, episode_count) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?)")) {
            for (RoleJSON role : cast.getRoles()) {
                ps.setInt(1, season.id);
                ps.setInt(2, cast.id);
                ps.setString(3, role.credit_id);
                ps.setInt(4, season.tvShowID);
                ps.setInt(5, season.season_number);
                ps.setString(6, role.character);
                ps.setInt(7, cast.order);
                ps.setInt(8, role.episode_count);
                ps.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException("insertSeasonCast fehlgeschlagen. seasonId: " + season.id
                    + ", personId: " + cast.id, e);
        }
    }

    /**
     * Fügt einen Crew-Eintrag in season_to_person ein.
     * Die Filterentscheidung liegt beim Aufrufer (Importer).
     */
    public void insertSeasonCrew(CrewJSON crew, SeasonJSON season, String job,
            String creditId, int episodeCount, Connection conn) {
        log.fine("insertSeasonCrew, seasonId " + season.id + ", personId " + crew.id + ", job " + job);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO season_to_person (season_id, person_id, credit_id, " +
                "tv_show_id, season_number, character, \"order\", department, job, episode_count) " +
                "VALUES (?, ?, ?, ?, ?, NULL, NULL, ?, ?, ?)")) {
            ps.setInt(1, season.id);
            ps.setInt(2, crew.id);
            ps.setString(3, creditId);
            ps.setInt(4, season.tvShowID);
            ps.setInt(5, season.season_number);
            ps.setString(6, crew.department);
            ps.setString(7, job);
            ps.setInt(8, episodeCount);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertSeasonCrew fehlgeschlagen. seasonId: " + season.id
                    + ", personId: " + crew.id + ", job: " + job, e);
        }
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public boolean seasonExists(int tvShowId, int seasonNumber) {
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT 1 FROM season WHERE tv_show_id = ? AND season_number = ?")) {
            ps.setInt(1, tvShowId);
            ps.setInt(2, seasonNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException("seasonExists fehlgeschlagen. tvShowId: " + tvShowId
                    + ", seasonNumber: " + seasonNumber, e);
        }
    }
    
    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    /**
     * Setzt regular=1 auf der season_to_person-Zeile (Match über
     * season_id, person_id, credit_id). Trifft keine Zeile: FailFast — die
     * Invariante "regulär ⊆ aggregiert" ist verletzt. Begründung und
     * Diagnose siehe TmdbSeriesImporter, Abschnitt "Season Regulars".
     */
    public void markRegularCast(CastJSON cast, int seasonId, Connection conn) {
        flipRegular(seasonId, cast.id, cast.getCredit_id(), conn);
    }

    /**
     * Setzt regular=1 auf der season_to_person-Zeile (Match über
     * season_id, person_id, credit_id). Trifft keine Zeile: FailFast — die
     * Invariante "regulär ⊆ aggregiert" ist verletzt. Begründung und
     * Diagnose siehe TmdbSeriesImporter, Abschnitt "Season Regulars".
     */
    public void markRegularCrew(CrewJSON crew, int seasonId, Connection conn) {
        flipRegular(seasonId, crew.id, crew.getCredit_id(), conn);
    }

    private void flipRegular(int seasonId, int personId, String creditId, Connection conn) {
        int updated;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE season_to_person SET regular = 1 " +
                "WHERE season_id = ? AND person_id = ? AND credit_id = ?")) {
            ps.setInt(1, seasonId);
            ps.setInt(2, personId);
            ps.setString(3, creditId);
            updated = ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("flipRegular fehlgeschlagen. seasonId: " + seasonId
                    + ", personId: " + personId, e);
        }
        if (updated == 0)
            throw new RuntimeException("Regular nicht in aggregierten Season-Credits gefunden — "
                    + "Grundannahme (regulär \u2286 aggregiert) verletzt. seasonId=" + seasonId
                    + ", personId=" + personId + ", creditId=" + creditId);
    }
}