package app.movie;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

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
import app.shared.KeyValueRepository;
import app.ui.skin.SkinService;

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
 * Bilder landen im Dateisystem unter Config.get("tmdb.imageFolder").
 * Bild-Metadaten landen in der DB.
 *
 * Alle Fehler sind fatal — kein stiller Fallback.
 */
public class Importer {

    private static final Logger log = Logger.getLogger(Importer.class.getName());

    private final ApiClient api;
    private final MovieRepository movieRepo;
    private final CrewFilterRepository crewFilterRepo;
    private final PendingRepository pendingRepo;

    public Importer() {
        this.api = new ApiClient();
        this.movieRepo = new MovieRepository();
        this.crewFilterRepo = new CrewFilterRepository();
        this.pendingRepo = new PendingRepository();
    }

    public void run() {
        log.info("TmdbImporter gestartet");
        importMovies();
        log.info("TmdbImporter abgeschlossen");
    }

    /**
     * Hauptablauf für den Filmimport.
     * Lädt zuerst Seite 1 für neue Bewertungen, dann die Rolling-Check-Seite
     * für Umbewertungen.
     */
    private void importMovies() {
        KeyValueRepository kvRepo = new KeyValueRepository();
        LocalDateTime lastImport = kvRepo.getTime("tmdb.lastMovieImport");
        if (lastImport.toLocalDate().equals(LocalDate.now())) {
            log.info("TMDB Filmimport heute bereits durchgeführt, überspringe.");
            return;
        }
        log.info("Letzter Filmimport: " + lastImport + ". Wir starten einen neuen Import-Lauf.");

        crewFilterRepo.load();

        // Schritt 1+2: Seite 1 laden, neue Bewertungen importieren
        MovieRatingsPageJSON firstPage = api.getRatedMovies(1);
        int totalPages = firstPage.total_pages;
        int newMoviesImported = processNewMovies(firstPage);
        log.info("Neue Filme importiert: " + newMoviesImported);

        // Schritt 3: Rolling-Check für Umbewertungen
        int lastCheckedPage = kvRepo.getInteger("tmdb.lastCheckedMoviePage");
        int nextPageToCheck = lastCheckedPage + 1;
        if (nextPageToCheck > totalPages)
            nextPageToCheck = 1;
        log.info("Rolling-Check auf Seite " + nextPageToCheck + " von " + totalPages);
        MovieRatingsPageJSON rollingPage = nextPageToCheck == 1
                ? firstPage
                : api.getRatedMovies(nextPageToCheck);
        processReratedMovies(rollingPage);

        // Schritt 4: Timestamps aktualisieren
        kvRepo.setTime("tmdb.lastMovieImport", LocalDateTime.now());
        kvRepo.setInteger("tmdb.lastCheckedMoviePage", nextPageToCheck);
        log.info("Timestamps aktualisiert");
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
                log.info("Umbewertung erkannt für Film " + rating.id + " (" + rating.title + ")");
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
        log.info("Importiere neuen Film: " + rating.title + " (id=" + rating.id + ")");
        MovieJSON movie = api.getMovieDetails(rating.id);
        CreditListJSON credits = api.getMovieCredits(rating.id);
        byte[] posterW92 = movie.poster_path != null ? api.getImage(movie.poster_path, "w92") : null;
        byte[] posterW154 = movie.poster_path != null ? api.getImage(movie.poster_path, "w154") : null;

        try (var conn = DB.getNewTmdbConnection()) {
            try {
                movieRepo.insertMovie(movie, conn);
                if (posterW92 != null) {
                    int[] dimensions = getImageDimensions(posterW92);
                    String filename = buildImageFilename(movie.poster_path, "en-US", dimensions[0], dimensions[1]);
                    saveImageToFileSystem(filename, posterW92);
                    movieRepo.insertMovieImage(movie, 92, dimensions[1], filename, conn);
                } else {
                	SkinService.get().createAlert(SkinService.getOwnerWindow(), "92er Poster fehlt", "Für " + movie.german_title + " / " + movie.title, false, false);
                }
                if (posterW154 != null) {
                    int[] dimensions = getImageDimensions(posterW154);
                    String filename = buildImageFilename(movie.poster_path, "en-US", dimensions[0], dimensions[1]);
                    saveImageToFileSystem(filename, posterW154);
                    movieRepo.insertMovieImage(movie, 154, dimensions[1], filename, conn);
                } else {
                	SkinService.get().createAlert(SkinService.getOwnerWindow(), "154er Poster fehlt", "Für " + movie.german_title + " / " + movie.title, false, false);
                }
                movieRepo.insertMovieRating(rating, null, conn);
                processCredits(credits, movie, conn);
                movieRepo.insertMovieGenres(movie, conn);
                movieRepo.insertMovieCountries(movie, conn);
                movieRepo.insertMovieLanguages(movie, conn);
                conn.commit();
                log.info("Film erfolgreich importiert: " + movie.title);
            } catch (Exception e) {
                conn.rollback();
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
                log.fine("Crew blacklisted, überspringe. personId=" + crew.id + ", job=" + job);
            } else if (crewFilterRepo.isWhitelisted(job)) {
                movieRepo.insertPersonIfNotExists(api.getPerson(crew.id), conn);
                movieRepo.insertMovieCrew(crew, movie.id, conn);
            } else {
                log.info("Crew-Job unbekannt, in pending. personId=" + crew.id + ", job=" + job + ", film=" + movie.title);
                pendingRepo.insertPersonPending(api.getPerson(crew.id), conn);
                pendingRepo.insertCrewPending(movie.id, crew.id, crew.name, job, crew.department, crew.getCredit_id(), conn);
            }
        }
    }

    /**
     * Speichert ein Bild im Dateisystem. Wirft Exception wenn bereits vorhanden —
     * das sollte nie passieren.
     */
    private static void saveImageToFileSystem(String filename, byte[] image) {
        File file = new File(Config.get("imageFolder") + "tmdb" + File.separator + filename);
        if (file.exists())
            throw new RuntimeException("Bild existiert bereits, das sollte nicht passieren: " + filename);
        try {
            file.getParentFile().mkdirs();
            java.nio.file.Files.write(file.toPath(), image);
            log.fine("Bild gespeichert: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("saveImageToFileSystem fehlgeschlagen. filename: " + filename, e);
        }
    }

    /**
     * Ermittelt Breite und Höhe eines Bildes aus den Rohdaten.
     * @return int[] mit [width, height]
     */
    private static int[] getImageDimensions(byte[] imageData) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
            return new int[]{img.getWidth(), img.getHeight()};
        } catch (Exception e) {
            throw new RuntimeException("getImageDimensions fehlgeschlagen", e);
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