package app.data.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Logger;

import app.tmdb.json.PersonJSON;

/**
 * Repository für die Pending-Tabellen tmdb_person_pending und tmdb_crew_pending.
 *
 * Personen und unbekannte Crew-Einträge landen hier während des Imports.
 * TmdbImportReview entscheidet dann was damit passiert:
 * - Person in person übertragen und crew_pending-Eintrag in movie_to_person
 * - oder beides verwerfen
 */
public class TmdbPendingRepository {

    private static final Logger log = Logger.getLogger(TmdbPendingRepository.class.getName());

    /**
     * Schreibt eine Person in tmdb_person_pending.
     * Ignoriert wenn bereits vorhanden.
     *
     * @param person    PersonJSON mit allen Detaildaten
     * @param conn      Transaktions-Connection
     */
    public void insertPersonPending(PersonJSON person, Connection conn) {
        log.fine("insertPersonPending, personId " + person.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO tmdb_person_pending (id, gender, name, known_for_department, " +
                "birthday, deathday, biography, homepage, imdb_id, place_of_birth, popularity, profile_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, person.id);
            ps.setInt(2, person.gender == null ? 0 : person.gender);
            ps.setString(3, person.name);
            ps.setString(4, person.known_for_department);
            ps.setString(5, person.birthday == null ? null : person.birthday.toString());
            ps.setString(6, person.deathday == null ? null : person.deathday.toString());
            ps.setString(7, person.biography);
            ps.setString(8, person.homepage);
            ps.setString(9, person.imdb_id);
            ps.setString(10, person.place_of_birth);
            ps.setDouble(11, person.popularity == null ? 0 : person.popularity);
            ps.setString(12, person.profile_path);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertPersonPending fehlgeschlagen. personId: " + person.id, e);
        }
    }

    /**
     * Schreibt einen unbekannten Crew-Eintrag in tmdb_crew_pending.
     *
     * @param movieId       TMDB-ID des Films
     * @param personId      TMDB-ID der Person
     * @param personName    Name der Person
     * @param job           Unbekannter Job
     * @param department    Department
     * @param creditId      Credit-ID
     * @param conn          Transaktions-Connection
     */
    public void insertCrewPending(int movieId, int personId, String personName, String job,
            String department, String creditId, Connection conn) {
        log.fine("insertCrewPending, personId " + personId + ", job " + job);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tmdb_crew_pending (movie_id, person_id, person_name, job, department, credit_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, movieId);
            ps.setInt(2, personId);
            ps.setString(3, personName);
            ps.setString(4, job);
            ps.setString(5, department);
            ps.setString(6, creditId);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertCrewPending fehlgeschlagen. personId: " + personId + ", job: " + job, e);
        }
    }

    /**
     * Überträgt eine Person von tmdb_person_pending in die echte person-Tabelle.
     * Löscht den Pending-Eintrag danach.
     *
     * @param personId  TMDB-ID der Person
     */
    public void transferPersonToMain(int personId) {
        log.info("transferPersonToMain, personId " + personId);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "INSERT OR IGNORE INTO person SELECT * FROM tmdb_person_pending WHERE id = ?")) {
            ps.setInt(1, personId);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("transferPersonToMain fehlgeschlagen. personId: " + personId, e);
        }
        deletePersonPending(personId);
    }

    /**
     * Löscht eine Person aus tmdb_person_pending.
     *
     * @param personId  TMDB-ID der Person
     */
    public void deletePersonPending(int personId) {
        log.fine("deletePersonPending, personId " + personId);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "DELETE FROM tmdb_person_pending WHERE id = ?")) {
            ps.setInt(1, personId);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("deletePersonPending fehlgeschlagen. personId: " + personId, e);
        }
    }

    /**
     * Löscht einen Crew-Pending-Eintrag nach der Entscheidung in TmdbImportReview.
     *
     * @param movieId   TMDB-ID des Films
     * @param personId  TMDB-ID der Person
     * @param job       Job
     */
    public void deleteCrewPending(int movieId, int personId, String job) {
        log.fine("deleteCrewPending, personId " + personId + ", job " + job);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "DELETE FROM tmdb_crew_pending WHERE movie_id = ? AND person_id = ? AND job = ?")) {
            ps.setInt(1, movieId);
            ps.setInt(2, personId);
            ps.setString(3, job);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("deleteCrewPending fehlgeschlagen. personId: " + personId + ", job: " + job, e);
        }
    }
}