package app.data.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import app.tmdb.json.CastJSON;
import app.tmdb.json.CreditListJSON;
import app.tmdb.json.CrewJSON;
import app.tmdb.json.GenreJSON;
import app.tmdb.json.MovieJSON;
import app.tmdb.json.MovieRatingJSON;
import app.tmdb.json.PersonJSON;
import app.tmdb.json.ProductionCountryJSON;
import app.tmdb.json.SpokenLanguageJSON;

/**
 * Repository für alle Datenbankoperationen rund um Filme in der TMDB-Datenbank.
 * Kein Netzwerkzugriff, kein Dateisystemzugriff — nur DB.
 */
public class TmdbMovieRepository {

    private static final Logger log = Logger.getLogger(TmdbMovieRepository.class.getName());
    
    /**
     * Fügt einen neuen Film in die DB ein. Ignoriert den Insert wenn der Film
     * bereits existiert (z.B. weil er schon über eine Episode oder Serie bekannt ist).
     *
     * @param movie     Vollständiges MovieJSON mit german_title
     * @param conn      Transaktions-Connection
     */
    public void insertMovie(MovieJSON movie, Connection conn) {
        log.fine("insertMovie, movieId " + movie.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO movie (id, title, original_title, german_title, release_date, " +
                "tagline, overview, backdrop_path, collection_name, budget, homepage, imdb_id, " +
                "original_language, popularity, poster_path, runtime, vote_average, vote_count) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, movie.id);
            ps.setString(2, movie.title);
            ps.setString(3, movie.original_title);
            ps.setString(4, movie.german_title);
            ps.setString(5, movie.release_date == null ? null : movie.release_date.toString());
            ps.setString(6, movie.tagline);
            ps.setString(7, movie.overview);
            ps.setString(8, movie.backdrop_path);
            ps.setString(9, movie.belongs_to_collection == null ? null : movie.belongs_to_collection.name);
            ps.setInt(10, movie.budget == null ? 0 : movie.budget);
            ps.setString(11, movie.homepage);
            ps.setString(12, movie.imdb_id);
            ps.setString(13, movie.original_language);
            ps.setDouble(14, movie.popularity == null ? 0 : movie.popularity);
            ps.setString(15, movie.poster_path);
            ps.setInt(16, movie.runtime == null ? 0 : movie.runtime);
            ps.setDouble(17, movie.vote_average == null ? 0 : movie.vote_average);
            ps.setInt(18, movie.vote_count == null ? 0 : movie.vote_count);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertMovie fehlgeschlagen. movieId: " + movie.id, e);
        }
    }
    
    /**
     * Fügt eine neue Filmbewertung in die DB ein.
     * Kommentar wird als "." gesetzt wenn keiner angegeben — wird später in
     * TmdbImportReview nachgepflegt.
     *
     * @param rating    MovieRatingJSON mit Bewertung und Metadaten
     * @param comment   Kommentar, oder "." wenn noch keiner eingegeben wurde
     * @param conn      Transaktions-Connection
     */
    public void insertMovieRating(MovieRatingJSON rating, String comment, Connection conn) {
        log.fine("insertMovieRating, movieId " + rating.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO movie_rating (movie_id, ar_value, comment, first_rated_at) " +
                "VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, rating.id);
            ps.setInt(2, rating.account_rating.value);
            ps.setString(3, comment == null || comment.isEmpty() ? "." : comment);
            ps.setString(4, rating.account_rating.getCreated_at().toString());
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertMovieRating fehlgeschlagen. movieId: " + rating.id, e);
        }
    }
    
    /**
     * Speichert die Bild-Metadaten eines Films in der DB.
     * Das eigentliche Bild-Byte-Array wird nicht hier gespeichert — das landet
     * im Dateisystem, gehandhabt durch den TmdbImporter.
     *
     * @param movie     MovieJSON mit poster_path
     * @param width     Breite des Bildes in Pixeln, z.B. 92 oder 154
     * @param height    Höhe des Bildes in Pixeln
     * @param filename  Dateiname wie er im Dateisystem gespeichert wird
     * @param conn      Transaktions-Connection
     */
    public void insertMovieImage(MovieJSON movie, int width, int height, String filename, Connection conn) {
        log.fine("insertMovieImage, movieId " + movie.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO movie_image (movie_id, type, width, height, language, original_name, filename) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, movie.id);
            ps.setString(2, "poster");
            ps.setInt(3, width);
            ps.setInt(4, height);
            ps.setString(5, "en-US");
            ps.setString(6, movie.poster_path.substring(1)); // führendes "/" entfernen
            ps.setString(7, filename);
            ps.execute();
        } catch (Exception e) {
            throw new RuntimeException("insertMovieImage fehlgeschlagen. movieId: " + movie.id, e);
        }
    }
    
    /**
     * Fügt eine Person in die DB ein, wenn sie noch nicht vorhanden ist.
     * Bereits vorhandene Personen werden nicht aktualisiert — das ist eine
     * bewusste Designentscheidung, siehe Architektur-Dokumentation.
     *
     * @param person    PersonJSON mit allen Detaildaten
     * @param conn      Transaktions-Connection
     */
    public void insertPersonIfNotExists(PersonJSON person, Connection conn) {
        log.fine("insertPersonIfNotExists, personId " + person.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO person (id, gender, name, known_for_department, birthday, " +
                "deathday, biography, homepage, imdb_id, place_of_birth, popularity, profile_path) " +
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
            throw new RuntimeException("insertPersonIfNotExists fehlgeschlagen. personId: " + person.id, e);
        }
    }
    
    /**
     * Fügt Cast und Crew eines Films in movie_to_person ein.
     * Bereits vorhandene Einträge werden ignoriert (INSERT OR IGNORE).
     * Personen müssen vorher bereits in der person-Tabelle vorhanden sein.
     *
     * @param credits   CreditListJSON mit Cast und Crew
     * @param movieId   TMDB-ID des Films
     * @param conn      Transaktions-Connection
     */
    public void insertMovieCredits(CreditListJSON credits, int movieId, Connection conn) {
        log.fine("insertMovieCredits, movieId " + movieId);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO movie_to_person (movie_id, person_id, credit_id, character, " +
                "\"order\", department, job) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            for (CastJSON cast : credits.cast) {
                ps.setInt(1, movieId);
                ps.setInt(2, cast.id);
                ps.setString(3, cast.getCredit_id());
                ps.setString(4, cast.getCharacter());
                ps.setObject(5, cast.order);
                ps.setNull(6, java.sql.Types.VARCHAR);
                ps.setNull(7, java.sql.Types.VARCHAR);
                ps.execute();
            }
            for (CrewJSON crew : credits.crew) {
                ps.setInt(1, movieId);
                ps.setInt(2, crew.id);
                ps.setString(3, crew.getCredit_id());
                ps.setNull(4, java.sql.Types.VARCHAR);
                ps.setNull(5, java.sql.Types.INTEGER);
                ps.setString(6, crew.department);
                ps.setString(7, crew.getJob());
                ps.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException("insertMovieCredits fehlgeschlagen. movieId: " + movieId, e);
        }
    }
    
    /**
     * Fügt die Genre-Zuordnungen eines Films ein.
     * Genres werden bei Bedarf ebenfalls angelegt (INSERT OR IGNORE).
     *
     * @param movie     MovieJSON mit genres-Liste
     * @param conn      Transaktions-Connection
     */
    public void insertMovieGenres(MovieJSON movie, Connection conn) {
        log.fine("insertMovieGenres, movieId " + movie.id);
        try (PreparedStatement psGenre = conn.prepareStatement(
                    "INSERT OR IGNORE INTO genre (id, name) VALUES (?, ?)");
             PreparedStatement psMovieGenre = conn.prepareStatement(
                    "INSERT OR IGNORE INTO movie_to_genre (movie_id, genre_id) VALUES (?, ?)")) {
            for (GenreJSON genre : movie.genres) {
                psGenre.setInt(1, genre.id);
                psGenre.setString(2, genre.name);
                psGenre.execute();
                psMovieGenre.setInt(1, movie.id);
                psMovieGenre.setInt(2, genre.id);
                psMovieGenre.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException("insertMovieGenres fehlgeschlagen. movieId: " + movie.id, e);
        }
    }

    /**
     * Fügt die Länder-Zuordnungen eines Films ein.
     *
     * @param movie     MovieJSON mit production_countries-Liste
     * @param conn      Transaktions-Connection
     */
    public void insertMovieCountries(MovieJSON movie, Connection conn) {
        log.fine("insertMovieCountries, movieId " + movie.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO movie_to_country (movie_id, iso_3166_1) VALUES (?, ?)")) {
            for (ProductionCountryJSON country : movie.production_countries) {
                ps.setInt(1, movie.id);
                ps.setString(2, country.iso_3166_1);
                ps.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException("insertMovieCountries fehlgeschlagen. movieId: " + movie.id, e);
        }
    }

    /**
     * Fügt die Sprach-Zuordnungen eines Films ein.
     *
     * @param movie     MovieJSON mit spoken_languages-Liste
     * @param conn      Transaktions-Connection
     */
    public void insertMovieLanguages(MovieJSON movie, Connection conn) {
        log.fine("insertMovieLanguages, movieId " + movie.id);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO movie_to_language (movie_id, iso_639_1) VALUES (?, ?)")) {
            for (SpokenLanguageJSON language : movie.spoken_languages) {
                ps.setInt(1, movie.id);
                ps.setString(2, language.iso_639_1);
                ps.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException("insertMovieLanguages fehlgeschlagen. movieId: " + movie.id, e);
        }
    }
    
    /**
     * Aktualisiert die Bewertung eines bereits vorhandenen Films.
     * first_rated_at wird nicht angefasst — nur der neue Wert, Kommentar und
     * das Update-Datum werden gesetzt.
     *
     * @param rating    MovieRatingJSON mit neuer Bewertung
     * @param comment   Kommentar. Der alte wird hiermit ersetzt!
     * @param conn      Transaktions-Connection
     */
	public void updateMovieRating(MovieRatingJSON rating, String comment) {
		{
			log.fine("updateMovieRating, movieId " + rating.id);
			try (PreparedStatement ps = DB.getTmdbConnection()
					.prepareStatement("UPDATE movie_rating SET ar_value = ?, comment = ?, last_updated_at = ? " + "WHERE movie_id = ?")) {
				ps.setInt(1, rating.account_rating.value);
				ps.setString(2, comment == null || comment.isEmpty() ? "." : comment);
				ps.setString(3, java.time.LocalDateTime.now().toString());
				ps.setInt(4, rating.id);
				ps.execute();
			} catch (Exception e) {
				throw new RuntimeException("updateMovieRating fehlgeschlagen. movieId: " + rating.id, e);
			}
		}
	}
    
    /**
     * Liefert den aktuellen Kommentar einer Filmbewertung aus der DB.
     * Gibt null zurück wenn keine Bewertung existiert.
     *
     * @param movieId   TMDB-ID des Films
     * @return          Aktueller Kommentar, oder null
     */
    public String getMovieComment(int movieId) {
        log.fine("getMovieComment, movieId " + movieId);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT comment FROM movie_rating WHERE movie_id = ?")) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString("comment");
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("getMovieComment fehlgeschlagen. movieId: " + movieId, e);
        }
    }

    /**
     * Liefert die aktuelle Bewertung eines Films aus der DB.
     * Gibt null zurück wenn keine Bewertung existiert.
     *
     * @param movieId   TMDB-ID des Films
     * @return          Aktuelle Bewertung als Integer, oder null
     */
    public Integer getMovieRating(int movieId) {
        log.fine("getMovieRating, movieId " + movieId);
        try (PreparedStatement ps = DB.getTmdbConnection().prepareStatement(
                "SELECT ar_value FROM movie_rating WHERE movie_id = ?")) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("ar_value");
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("getMovieRating fehlgeschlagen. movieId: " + movieId, e);
        }
    }
}