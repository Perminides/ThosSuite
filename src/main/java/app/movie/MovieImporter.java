package app.movie;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import app.movie.model.json.CastJSON;
import app.movie.model.json.CreditListJSON;
import app.movie.model.json.CrewJSON;
import app.movie.model.json.MovieJSON;
import app.movie.model.json.MovieRatingJSON;
import app.movie.model.json.MovieRatingsPageJSON;
import app.movie.repository.CrewFilterRepository;
import app.movie.repository.MovieRepository;
import app.movie.repository.PendingRepository;
import app.shared.Config;
import app.shared.DB;
import app.shared.ImageUtils;
import app.shared.Log;
import app.shared.model.ButtonEnum;
import app.shared.ui.Alerts;

/**
 * Orchestriert den täglichen TMDB-Import.
 *
 * Ablauf:
 * 1. Seite 1 der bewerteten Filme laden
 * 2. Neue Bewertungen importieren (solange created_at neuer als lastMovieImport)
 * 3. Rolling-Check: eine weitere Seite auf Umbewertungen prüfen
 * 4. Timestamps und Seitenzähler in key_values aktualisieren
 *
 * Serien und Episoden werden nicht hier behandelt — die laufen über einen
 * separaten manuellen Menüpunkt.
 *
 * Bilder landen im Dateisystem unter Config.getPath("tmdb.imageFolder").
 * Bild-Metadaten landen in der DB.
 *
 * Alle Fehler sind fatal — kein stiller Fallback.
 */
public class MovieImporter {


    private final ApiClient api;
    private final MovieRepository movieRepo;
    private final CrewFilterRepository crewFilterRepo;
    private final PendingRepository pendingRepo;

    public MovieImporter() {
        this.api = new ApiClient();
        this.movieRepo = new MovieRepository();
        this.crewFilterRepo = new CrewFilterRepository();
        this.pendingRepo = new PendingRepository();
    }

    public void run() {
        Log.info(MovieImporter.class, "TmdbImporter gestartet");
        importMovies();
        Log.info(MovieImporter.class, "TmdbImporter abgeschlossen");
    }

    /**
     * Hauptablauf für den Filmimport.
     * Lädt zuerst Seite 1 für neue Bewertungen, dann die Rolling-Check-Seite
     * für Umbewertungen.
     */
    private void importMovies() {
        LocalDateTime lastImport = Config.getTime("tmdb.lastMovieImport");
        if (lastImport.toLocalDate().equals(LocalDate.now())) {
            Log.info(MovieImporter.class, "TMDB Filmimport heute bereits durchgeführt, überspringe.");
            return;
        }
        Log.info(MovieImporter.class, "Letzter Filmimport: " + lastImport + ". Wir starten einen neuen Import-Lauf.");

        crewFilterRepo.load();

        // Schritt 1+2: Seite 1 laden, neue Bewertungen importieren
        MovieRatingsPageJSON firstPage = api.getRatedMovies(1);
        int totalPages = firstPage.total_pages;
        int newMoviesImported = processNewMovies(firstPage);
        Log.info(MovieImporter.class, "Neue Filme importiert: " + newMoviesImported);

        // Schritt 3: Rolling-Check für Umbewertungen
        int lastCheckedPage = Config.getInt("tmdb.lastCheckedMoviePage");
        int nextPageToCheck = lastCheckedPage + 1;
        if (nextPageToCheck > totalPages)
            nextPageToCheck = 1;
        Log.info(MovieImporter.class, "Rolling-Check auf Seite " + nextPageToCheck + " von " + totalPages);
        MovieRatingsPageJSON rollingPage = nextPageToCheck == 1
                ? firstPage
                : api.getRatedMovies(nextPageToCheck);
        processReratedMovies(rollingPage);

        // Schritt 4: Timestamps aktualisieren
        Config.setTime("tmdb.lastMovieImport", LocalDateTime.now());
        Config.setInt("tmdb.lastCheckedMoviePage", nextPageToCheck);
        Log.info(MovieImporter.class, "Timestamps aktualisiert");
    }

    /**
     * Verarbeitet neue Bewertungen auf einer Seite.
     * Bricht ab sobald ein Film gefunden wird, der bereits in der DB existiert.
     *
     * @return Anzahl neu importierter Filme
     */
    private int processNewMovies(MovieRatingsPageJSON firstPage) {
        int count = 0;
        int pageNo = 1;
        MovieRatingsPageJSON page = firstPage;
        outer:
        while (true) {
            for (MovieRatingJSON rating : page.results) {
                if (movieRepo.getMovieRating(rating.id) != null)
                    break outer;
                importNewMovie(rating);
                count++;
            }
            if (pageNo >= page.total_pages)
                break;
            pageNo++;
            page = api.getRatedMovies(pageNo);
        }
        return count;
    }

    /**
     * Prüft alle Einträge einer Seite auf Umbewertungen.
     * Vergleicht account_rating.value mit dem gespeicherten Wert in der DB.
     */
    private void processReratedMovies(MovieRatingsPageJSON page) {
        for (MovieRatingJSON rating : page.results) {
            Integer dbRating = movieRepo.getMovieRating(rating.id);
            if (dbRating == null)
                throw new RuntimeException("Film auf der Umbewertungs-Prüfseite nicht in DB gefunden. movieId: " + rating.id + " (" + rating.title + ")");
            if (!dbRating.equals(rating.account_rating.value)) {
                Log.info(MovieImporter.class, "Umbewertung erkannt für Film " + rating.id + " (" + rating.title + ")");
                String existingComment = movieRepo.getMovieComment(rating.id);
                movieRepo.updateMovieRating(rating, existingComment);
            }
        }
    }

    /**
     * Importiert einen einzelnen neuen Film vollständig:
     * Movie, Credits, Personen, Bilder, Rating — alles in einer Transaktion.
     */
    private void importNewMovie(MovieRatingJSON rating) {
        Log.info(MovieImporter.class, "Importiere neuen Film: " + rating.title + " (id=" + rating.id + ")");
        MovieJSON movie = api.getMovieDetails(rating.id);
        CreditListJSON credits = api.getMovieCredits(rating.id);
        byte[] posterW92 = movie.poster_path != null ? api.getImage(movie.poster_path, "w92") : null;
        byte[] posterW154 = movie.poster_path != null ? api.getImage(movie.poster_path, "w154") : null;

        // Die Poster liegen im Dateisystem, nicht in der Transaktion — ein rollback() erwischt sie
        // nicht. Bliebe eine Datei liegen, scheiterte derselbe Film beim nächsten Start erneut, und
        // zwar an "Bild existiert bereits". Deshalb merken und im catch mit aufräumen. Die harte
        // Prüfung in saveImageToFileSystem bleibt so erhalten: sie meldet dann echte Namenskollisionen
        // und nicht mehr die Trümmer des eigenen Vorlaufs.
        List<String> writtenPosters = new ArrayList<>();

        try (var conn = DB.getNewTmdbConnection()) {
            try {
                movieRepo.insertMovie(movie, conn);
                if (posterW92 != null) {
                    int[] dimensions = ImageUtils.dimensions(posterW92);
                    String filename = buildImageFilename(movie.poster_path, "en-US", dimensions[0], dimensions[1]);
                    saveImageToFileSystem(filename, posterW92);
                    writtenPosters.add(filename);
                    movieRepo.insertMovieImage(movie, 92, dimensions[1], filename, conn);
                } else {
                	Alerts.show("92er Poster fehlt", "Für " + movie.german_title + " / " + movie.title, ButtonEnum.OK);
                }
                if (posterW154 != null) {
                    int[] dimensions = ImageUtils.dimensions(posterW154);
                    String filename = buildImageFilename(movie.poster_path, "en-US", dimensions[0], dimensions[1]);
                    saveImageToFileSystem(filename, posterW154);
                    writtenPosters.add(filename);
                    movieRepo.insertMovieImage(movie, 154, dimensions[1], filename, conn);
                } else {
                	Alerts.show("154er Poster fehlt", "Für " + movie.german_title + " / " + movie.title, ButtonEnum.OK);
                }
                movieRepo.insertMovieRating(rating, null, conn);
                processCredits(credits, movie, conn);
                movieRepo.insertMovieGenres(movie, conn);
                movieRepo.insertMovieCountries(movie, conn);
                movieRepo.insertMovieLanguages(movie, conn);
                conn.commit();
                Log.info(MovieImporter.class, "Film erfolgreich importiert: " + movie.title);
            } catch (Exception e) {
                conn.rollback();
                deletePoster(writtenPosters);
                throw new RuntimeException("Import fehlgeschlagen für Film: " + rating.title + " (id=" + rating.id + ")", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("TMDB-DB-Verbindung fehlgeschlagen", e);
        }
    }

    /**
     * Verarbeitet Credits eines Films:
     * - Cast: Person in DB, dann movie_to_person-Eintrag
     * - Crew whitelisted: wie Cast
     * - Crew blacklisted: ignorieren
     * - Crew unbekannt: Person und Crew-Eintrag in pending-Tabellen
     *
     * Alle Operationen laufen auf der übergebenen Transaktions-Connection.
     */
    private void processCredits(CreditListJSON credits, MovieJSON movie, Connection conn) {
        for (CastJSON cast : credits.cast) {
            movieRepo.insertPersonIfNotExists(api.getPerson(cast.id), conn);
            movieRepo.insertMovieCast(cast, movie.id, conn);
        }
        for (CrewJSON crew : credits.crew) {
            String job = crew.getJob();
            if (crewFilterRepo.isBlacklisted(job)) {
                Log.debug(MovieImporter.class, "Crew blacklisted, überspringe. personId=" + crew.id + ", job=" + job);
            } else if (crewFilterRepo.isWhitelisted(job)) {
                movieRepo.insertPersonIfNotExists(api.getPerson(crew.id), conn);
                movieRepo.insertMovieCrew(crew, movie.id, conn);
            } else {
                Log.info(MovieImporter.class, "Crew-Job unbekannt, in pending. personId=" + crew.id + ", job=" + job + ", film=" + movie.title);
                pendingRepo.insertPersonPending(api.getPerson(crew.id), conn);
                pendingRepo.insertCrewPending(movie.id, crew.id, crew.name, job, crew.department, crew.getCredit_id(), conn);
            }
        }
    }

    /**
     * Speichert ein Bild im Dateisystem. Wirft Exception wenn bereits vorhanden —
     * das sollte nie passieren.
     */
    /**
     * Das Dateisystem-Gegenstück zum {@code rollback()}. Wirft bewusst nicht weiter — hier wird ein
     * bereits gescheiterter Import aufgeräumt, und ein Problem beim Aufräumen darf die eigentliche
     * Ursache nicht verdecken. Es wird geloggt, mehr nicht.
     */
    private static void deletePoster(List<String> filenames) {
        for (String filename : filenames) {
            try {
                java.nio.file.Files.deleteIfExists(
                        Config.getPath("imageFolder").resolve("tmdb").resolve(filename));
                Log.info(MovieImporter.class, "Poster nach Rollback entfernt: " + filename);
            } catch (Exception e) {
                Log.warn(MovieImporter.class, "Poster konnte nach Rollback nicht entfernt werden: " + filename + " (" + e + ")");
            }
        }
    }

    private static void saveImageToFileSystem(String filename, byte[] image) {
    	File file = Config.getPath("imageFolder").resolve("tmdb").resolve(filename).toFile();
        if (file.exists())
            throw new RuntimeException("Bild existiert bereits, das sollte nicht passieren: " + filename);
        try {
            file.getParentFile().mkdirs();
            java.nio.file.Files.write(file.toPath(), image);
            Log.debug(MovieImporter.class, "Bild gespeichert: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("saveImageToFileSystem fehlgeschlagen. filename: " + filename, e);
        }
    }

    /**
     * Baut den Dateinamen für ein Bild zusammen.
     * Format: originalname_language_width_height.jpg
     */
    private static String buildImageFilename(String posterPath, String language, int width, int height) {
        String base = posterPath.startsWith("/") ? posterPath.substring(1) : posterPath;
        base = base.substring(0, base.lastIndexOf('.'));
        return base + "_" + language + "_" + width + "_" + height + ".jpg";
    }
}
