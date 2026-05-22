package app.data.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * Repository für Whitelist und Blacklist der TMDB-Crew-Jobs.
 *
 * Beim Start des Importers werden beide Listen einmalig in den Speicher geladen
 * und dann nur noch lokal abgefragt — kein DB-Hit pro Job während des Imports.
 * Neue Einträge werden sofort in die DB geschrieben und lokal nachgepflegt.
 */
public class TmdbCrewFilterRepository {

    private static final Logger log = Logger.getLogger(TmdbCrewFilterRepository.class.getName());

    private final java.util.Set<String> whitelist = new java.util.HashSet<>();
    private final java.util.Set<String> blacklist = new java.util.HashSet<>();

    /**
     * Lädt Whitelist und Blacklist einmalig aus der DB in den Speicher.
     * Muss zu Beginn des Imports aufgerufen werden.
     */
    public void load() {
        whitelist.clear();
        blacklist.clear();
        log.info("Lade CrewFilter aus DB");
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT job FROM crew_whitelist");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                whitelist.add(rs.getString("job"));
        } catch (Exception e) {
            throw new RuntimeException("CrewFilter Whitelist laden fehlgeschlagen", e);
        }
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT job FROM crew_blacklist");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                blacklist.add(rs.getString("job"));
        } catch (Exception e) {
            throw new RuntimeException("CrewFilter Blacklist laden fehlgeschlagen", e);
        }
        log.info("CrewFilter geladen: " + whitelist.size() + " Whitelist, " + blacklist.size() + " Blacklist");
    }

    public boolean isWhitelisted(String job) {
        return whitelist.contains(job);
    }

    public boolean isBlacklisted(String job) {
        return blacklist.contains(job);
    }

    /**
     * Fügt einen Job zur Whitelist hinzu — in DB und lokal.
     */
    public void addToWhitelist(String job) {
        log.info("Job zur Whitelist hinzugefügt: " + job);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "INSERT INTO crew_whitelist (job) VALUES (?)")) {
            ps.setString(1, job);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("addToWhitelist fehlgeschlagen. job: " + job, e);
        }
        whitelist.add(job);
    }

    /**
     * Fügt einen Job zur Blacklist hinzu — in DB und lokal.
     */
    public void addToBlacklist(String job) {
        log.info("Job zur Blacklist hinzugefügt: " + job);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "INSERT INTO crew_blacklist (job) VALUES (?)")) {
            ps.setString(1, job);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("addToBlacklist fehlgeschlagen. job: " + job, e);
        }
        blacklist.add(job);
    }

    /**
     * Liefert alle Jobs die noch nicht eingeordnet wurden — also in crew_pending
     * stehen aber weder in Whitelist noch Blacklist.
     */
    public java.util.List<String> getPendingJobs() {
        java.util.List<String> jobs = new java.util.ArrayList<>();
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT DISTINCT job FROM crew_pending");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                jobs.add(rs.getString("job"));
        } catch (Exception e) {
            throw new RuntimeException("getPendingJobs fehlgeschlagen", e);
        }
        return jobs;
    }
}