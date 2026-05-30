package app.misc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import app.config.Config;
import app.data.persistence.DB;
import app.data.persistence.TmdbMovieRepository;
import app.tmdb.TmdbApiClient;
import app.tmdb.json.CastJSON;
import app.tmdb.json.CreditListJSON;
import app.tmdb.json.CrewJSON;

/**
 * Einmal-Lauf: füllt season_to_person.regular.
 *
 * Für jede Season in der DB werden die nicht-aggregierten (regulären)
 * Season-Credits geholt. Jede zurückgelieferte Cast- und Crew-Person wird
 * über (season_id, person_id, credit_id) auf regular=1 geflippt. Findet sich
 * keine passende Zeile, wird sie angelegt — vorher wird die Person bei Bedarf
 * über die API nachgeladen und in person eingefügt (FK-Vorbedingung).
 *
 * Ändert nichts an den regulären Klassen. Für den seltenen Insert-Fall wird
 * die reguläre insertPersonIfNotExists-Methode aufgerufen (nur genutzt, nicht
 * verändert), damit nachgeladene Personen identisch befüllt sind.
 *
 * 1 Sekunde Pause pro Season. Idempotent — erneuter Lauf schadet nicht.
 */
public class SeasonRegularBackfill {

    private static final Logger log = Logger.getLogger(SeasonRegularBackfill.class.getName());

    private record SeasonRow(int seasonId, int tvShowId, int seasonNumber) {}

    private final TmdbApiClient api = new TmdbApiClient();
    private final TmdbMovieRepository movieRepo = new TmdbMovieRepository();

    private int flipped;
    private int inserted;
    private int personsLoaded;

    public static void main(String[] args) {
    	Config.init("C:/Users/permi/Documents/Gedächtnis Lernen und so/ThosSuite/");
        new SeasonRegularBackfill().run();
    }

    public void run() {
        List<SeasonRow> seasons = loadAllSeasons();
        log.info("SeasonRegularBackfill gestartet, Seasons: " + seasons.size());

        int processed = 0;
        for (SeasonRow s : seasons) {
            CreditListJSON credits = api.getRegularSeasonCredits(s.tvShowId(), s.seasonNumber());
            for (CastJSON cast : credits.cast)
                markRegularCast(cast, s.seasonId(), s.tvShowId(), s.seasonNumber());
            for (CrewJSON crew : credits.crew)
                markRegularCrew(crew, s.seasonId(), s.tvShowId(), s.seasonNumber());
            processed++;
            log.info("Season fertig (" + processed + "/" + seasons.size()
                    + "): seasonId=" + s.seasonId() + ", tvShowId=" + s.tvShowId()
                    + ", seasonNumber=" + s.seasonNumber());
            sleepOneSecond();
        }
        log.info("SeasonRegularBackfill abgeschlossen. Geflippt: " + flipped
                + ", eingefügt: " + inserted + ", Personen nachgeladen: " + personsLoaded);
    }

    // -------------------------------------------------------------------------
    // Cast / Crew flippen, im Fehlerfall einfügen
    // -------------------------------------------------------------------------

    private void markRegularCast(CastJSON cast, int seasonId, int tvShowId, int seasonNumber) {
        if (flip(seasonId, cast.id, cast.getCredit_id())) {
            flipped++;
            return;
        }
        log.info("Regular-Cast nicht in aggregierten Daten, lege an. seasonId=" + seasonId
                + ", personId=" + cast.id + ", creditId=" + cast.getCredit_id());
        ensurePersonExists(cast.id);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "INSERT OR IGNORE INTO season_to_person (season_id, person_id, credit_id, " +
                "tv_show_id, season_number, character, \"order\", department, job, episode_count, regular) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, 1)")) {
            ps.setInt(1, seasonId);
            ps.setInt(2, cast.id);
            ps.setString(3, cast.getCredit_id());
            ps.setInt(4, tvShowId);
            ps.setInt(5, seasonNumber);
            ps.setString(6, cast.getCharacter());
            ps.setObject(7, cast.order);
            ps.execute();
            inserted++;
        } catch (Exception e) {
            throw new RuntimeException("Cast-INSERT fehlgeschlagen. seasonId: " + seasonId
                    + ", personId: " + cast.id, e);
        }
    }

    private void markRegularCrew(CrewJSON crew, int seasonId, int tvShowId, int seasonNumber) {
        if (flip(seasonId, crew.id, crew.getCredit_id())) {
            flipped++;
            return;
        }
        log.info("Regular-Crew nicht in aggregierten Daten, lege an. seasonId=" + seasonId
                + ", personId=" + crew.id + ", job=" + crew.getJob()
                + ", creditId=" + crew.getCredit_id());
        ensurePersonExists(crew.id);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "INSERT OR IGNORE INTO season_to_person (season_id, person_id, credit_id, " +
                "tv_show_id, season_number, character, \"order\", department, job, episode_count, regular) " +
                "VALUES (?, ?, ?, ?, ?, NULL, NULL, ?, ?, NULL, 1)")) {
            ps.setInt(1, seasonId);
            ps.setInt(2, crew.id);
            ps.setString(3, crew.getCredit_id());
            ps.setInt(4, tvShowId);
            ps.setInt(5, seasonNumber);
            ps.setString(6, crew.department);
            ps.setString(7, crew.getJob());
            ps.execute();
            inserted++;
        } catch (Exception e) {
            throw new RuntimeException("Crew-INSERT fehlgeschlagen. seasonId: " + seasonId
                    + ", personId: " + crew.id + ", job: " + crew.getJob(), e);
        }
    }

    /**
     * Setzt regular=1 auf der passenden Zeile. Liefert true, wenn eine Zeile
     * getroffen wurde.
     */
    private boolean flip(int seasonId, int personId, String creditId) {
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "UPDATE season_to_person SET regular = 1 " +
                "WHERE season_id = ? AND person_id = ? AND credit_id = ?")) {
            ps.setInt(1, seasonId);
            ps.setInt(2, personId);
            ps.setString(3, creditId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new RuntimeException("flip fehlgeschlagen. seasonId: " + seasonId
                    + ", personId: " + personId, e);
        }
    }

    // -------------------------------------------------------------------------
    // Person-Vorbedingung
    // -------------------------------------------------------------------------

    /**
     * Stellt sicher, dass die Person in der person-Tabelle existiert. Lädt sie
     * bei Bedarf über die API nach und nutzt die reguläre
     * insertPersonIfNotExists-Methode — kein eigener Person-Insert, damit die
     * Feldabdeckung exakt dem regulären Import entspricht.
     */
    private void ensurePersonExists(int personId) {
        if (personExists(personId))
            return;
        movieRepo.insertPersonIfNotExists(api.getPerson(personId), DB.getTmdbConnection());
        personsLoaded++;
    }

    private boolean personExists(int personId) {
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT 1 FROM person WHERE id = ?")) {
            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException("personExists fehlgeschlagen. personId: " + personId, e);
        }
    }

    // -------------------------------------------------------------------------
    // Seasons laden
    // -------------------------------------------------------------------------

    private List<SeasonRow> loadAllSeasons() {
        List<SeasonRow> result = new ArrayList<>();
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT id, tv_show_id, season_number FROM season");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                result.add(new SeasonRow(rs.getInt("id"), rs.getInt("tv_show_id"),
                        rs.getInt("season_number")));
        } catch (Exception e) {
            throw new RuntimeException("loadAllSeasons fehlgeschlagen", e);
        }
        return result;
    }

    private void sleepOneSecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Backfill unterbrochen", e);
        }
    }
}