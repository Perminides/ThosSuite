package app.tmdb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import app.config.Config;
import app.data.persistence.DB;
import app.data.persistence.KeyValueRepository;
import app.data.persistence.TmdbCrewFilterRepository;
import app.data.persistence.TmdbEpisodeRepository;
import app.data.persistence.TmdbMovieRepository;
import app.data.persistence.TmdbSeasonRepository;
import app.data.persistence.TmdbTvShowRepository;
import app.data.persistence.TmdbTvShowRepository.TvShowComparisonData;
import app.tmdb.json.CastJSON;
import app.tmdb.json.CreditListJSON;
import app.tmdb.json.CrewJSON;
import app.tmdb.json.EpisodeJSON;
import app.tmdb.json.EpisodeRatingJSON;
import app.tmdb.json.EpisodeRatingsPageJSON;
import app.tmdb.json.JobJSON;
import app.tmdb.json.SeasonJSON;
import app.tmdb.json.TvShowJSON;
import app.tmdb.json.TvShowRatingJSON;
import app.tmdb.json.TvShowRatingsPageJSON;
import app.ui.skin.SkinService;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Orchestriert den manuellen Serien-/Episoden-Import.
 *
 * <p>Wird über einen Menübutton ausgelöst. Läuft komplett auf dem FX-Thread.
 * Interaktive Dialoge (Kommentar, Flag, Crew-Whitelist) erscheinen inline.
 *
 * <h2>Ablauf</h2>
 * <ol>
 *   <li>Serien-Check: neue Serien importieren, Umbewertungen erkennen,
 *       geänderte Seriendaten (Seasons/Episodes/Status/Last-Air-Date) auf
 *       Rückfrage aktualisieren.</li>
 *   <li>Episoden-Check: neue Episoden mit Kaskade (Show → Season → Episode
 *       sicherstellen), Kommentar- und Flag-Abfrage; Umbewertungen erkennen.</li>
 *   <li>Lücken-Check: fehlende Poster und Overviews für Filme, Serien und
 *       Episoden nachholen.</li>
 *   <li>Abschluss-Zusammenfassung.</li>
 * </ol>
 *
 * <h2>Kaskade beim Episoden-Import</h2>
 * Eine bewertete Episode kann auf eine Show/Season verweisen, die noch nicht in
 * der DB ist (z.B. wenn nie zuvor etwas aus dieser Serie bewertet wurde).
 * {@code ensureShowExists} und {@code ensureSeasonExists} importieren diese bei
 * Bedarf nach — die Show ggf. ohne eigenes Rating (nur über den Episodenpfad
 * reingekommen).
 *
 * <h2>Episoden-Bewertungsart (rated_season)</h2>
 * Pro neuer Episode wird genau eine Frage gestellt: bezieht sich die Bewertung
 * auf diese eine Episode oder auf mehr aus der Staffel (entweder abgebrochen
 * oder letzte Episode bewertet, dann bezieht es sich auf die ganze Staffel)?
 * Daraus ergibt sich das Flag {@code rated_season}, das die Credit- und
 * Overview-Auswahl im Viewer steuert (siehe {@code CardData.forEpisode} und die
 * episode_*-Views). Es gibt keine Vorschau — die fertige Karte wird nachträglich
 * im Viewer kontrolliert, das Flag kann bei Bedarf direkt in der DB angepasst
 * werden.
 *
 * <h2>Season Regulars und die aggregated-Invariante</h2>
 * Direkt nach dem Import einer Season ({@code /aggregate_credits}) werden die
 * regulären Season-Credits ({@code /credits}) geholt und die zugehörigen Zeilen
 * in {@code season_to_person} auf {@code regular = 1} geflippt — in derselben
 * Transaktion wie der Season-Import.
 *
 * <p><b>Wie TMDB Credits führt:</b> Eine Staffel hat zweierlei Cast/Crew. Zum
 * einen die Credits der einzelnen Episoden (Guest Stars, Episodenregie usw.).
 * Zum anderen die "Season Regulars" — durchgehende Stammbesetzung/-crew, die auf
 * TMDB direkt an der Staffel gepflegt wird ("Add Season Regular" auf
 * Staffelebene), nicht über einzelne Episoden. Der reguläre Endpunkt
 * ({@code /credits}) liefert genau diese Regulars; der aggregierte
 * ({@code /aggregate_credits}) liefert die Vereinigung aus beidem.
 *
 * <p><b>Invariante:</b> {@code aggregated == (Vereinigung aller Episoden-Credits)
 * ∪ (reguläre Season-Credits)}. Insbesondere ist jeder reguläre Credit auch in
 * aggregated enthalten ("regulär ⊆ aggregiert"). Empirisch bestätigt an Akte X
 * Staffel 7 (22 Episoden, akribisch gepflegte Fan-Daten) und einer Miniserie —
 * in beide Richtungen keine Abweichung.
 *
 * <p><b>Warum wir flippen statt einfügen:</b> Weil {@code season_to_person}
 * bereits alle aggregierten Credits enthält und die Regulars eine Teilmenge
 * davon sind, müssen wir nichts Neues einfügen — es genügt, die bereits
 * vorhandenen Zeilen als regular zu markieren. Der Flip matcht auf
 * {@code (season_id, person_id, credit_id)}; {@code credit_id} ist pro Eintrag
 * eindeutig (ebenfalls bestätigt — keine (person_id, credit_id) mit mehreren
 * Jobs/Characters), daher braucht der Match weder job noch character.
 *
 * <p><b>FailFast:</b> Findet der Flip keine Zeile, ist die Invariante verletzt
 * (ein regulärer Credit, der nicht in aggregated steht). Das darf nach unserer
 * Prüfung nicht vorkommen; tritt es doch auf, wirft
 * {@code TmdbSeasonRepository.markRegularCast/markRegularCrew} eine
 * RuntimeException statt eines stillen Inserts. Der umschließende
 * Transaktions-Rollback verwirft dann die ganze Season. Zur Diagnose eines
 * solchen Falls dient die Wegwerfklasse {@code AggregatedCreditsProbe}
 * (vergleicht aggregated gegen Episoden ∪ Regulars und prüft die
 * credit_id-Eindeutigkeit).
 *
 * <h2>Transaktionen und Fehler</h2>
 * Jeder Entitäts-Import (Show, Season, Episode) läuft jeweils in einer eigenen
 * Transaktion über {@code DB.getNewTmdbConnection()}. Alle Fehler sind fatal —
 * kein stiller Fallback. Schlägt etwas innerhalb einer Transaktion fehl (auch
 * der Regular-Flip), wird die gesamte Entität zurückgerollt, statt halb
 * importiert zu werden.
 *
 * <h2>Crew-Filter</h2>
 * Crew-Jobs werden gegen eine Whitelist/Blacklist geprüft. Unbekannte Jobs
 * lösen eine interaktive Whitelist/Blacklist-Abfrage aus; die Entscheidung wird
 * persistiert und gilt für den weiteren Lauf.
 */
public class TmdbSeriesImporter {

    private static final Logger log = Logger.getLogger(TmdbSeriesImporter.class.getName());

    private final TmdbApiClient api;
    private final TmdbTvShowRepository tvShowRepo;
    private final TmdbSeasonRepository seasonRepo;
    private final TmdbEpisodeRepository episodeRepo;
    private final TmdbMovieRepository movieRepo;
    private final TmdbCrewFilterRepository crewFilterRepo;
    private final KeyValueRepository kvRepo;

    // Zähler für die Zusammenfassung
    private int newShows;
    private int reRatedShows;
    private int updatedShowData;
    private int newEpisodes;
    private int reRatedEpisodes;
    private int postersFound;
    private int overviewsFound;

    public TmdbSeriesImporter() {
        this.api = new TmdbApiClient();
        this.tvShowRepo = new TmdbTvShowRepository();
        this.seasonRepo = new TmdbSeasonRepository();
        this.episodeRepo = new TmdbEpisodeRepository();
        this.movieRepo = new TmdbMovieRepository();
        this.crewFilterRepo = new TmdbCrewFilterRepository();
        this.kvRepo = new KeyValueRepository();
    }

    public void run() {
        log.info("TmdbSeriesImporter gestartet");
        crewFilterRepo.load();
        newShows = 0; reRatedShows = 0; updatedShowData = 0;
        newEpisodes = 0; reRatedEpisodes = 0;
        postersFound = 0; overviewsFound = 0;

        // Step 1: Serien
        showStepAlert("Wir schauen mal, ob sich bei den Serien was getan hat.");
        processRatedTvShows();

        // Step 2: Episoden
        showStepAlert("Wir schauen mal, ob sich bei den Episoden was getan hat.");
        processRatedEpisodes();

        // Step 3: Lücken-Check
        // TODO: Unbedingt SOFORT: auf serien und episoden erweitern, sollte easy as a pie sein. Meinte Claude zumindest
        showStepAlert("Wir schauen mal, ob fehlende Poster oder Zusammenfassungen nachgeholt werden können.");
        processGaps();

        // Abschluss
        kvRepo.setTime("tmdb.lastAdditionalImportRun", LocalDateTime.now());
        showSummary();
        log.info("TmdbSeriesImporter abgeschlossen");
    }

    // =========================================================================
    // Step 1: Serien
    // =========================================================================

    private void processRatedTvShows() {
        int pageNo = 0;
        TvShowRatingsPageJSON page;
        do {
            pageNo++;
            page = api.getRatedTvShows(pageNo);
            for (TvShowRatingJSON rating : page.results) {
                Integer dbRating = tvShowRepo.getTvShowRating(rating.id);

                if (dbRating == null) {
                    // Neue Serie
                    importNewTvShow(rating);
                    newShows++;
                } else if (!dbRating.equals(rating.account_rating.value)) {
                    // Umbewertung
                    log.info("Serien-Umbewertung erkannt: " + rating.name + " (id=" + rating.id + ")");
                    String existingComment = tvShowRepo.getTvShowComment(rating.id);
                    String newComment = askForComment(
                            "Serien-Umbewertung: " + rating.name,
                            rating.name + (rating.original_name != null
                                    && !rating.original_name.equals(rating.name)
                                    ? " / " + rating.original_name : ""),
                            existingComment);
                    tvShowRepo.updateTvShowRating(rating, newComment);
                    reRatedShows++;
                }

                // Daten-Check für alle bewerteten Serien
                checkTvShowDataChanged(rating.id, rating.name);
            }
        } while (pageNo < page.total_pages);
    }

    private void importNewTvShow(TvShowRatingJSON rating) {
        log.info("Importiere neue Serie: " + rating.name + " (id=" + rating.id + ")");
        TvShowJSON show = api.getTvShowDetails(rating.id);
        CreditListJSON credits = api.getAggregatedTvShowCredits(rating.id);

        String comment = askForComment(
                "Neue Serie bewertet: " + show.name,
                show.name + (show.german_name != null && !show.german_name.equals(show.name)
                        ? " / " + show.german_name : ""),
                null);

        // TODO: tmdb.posterWidths=92,154
        byte[] posterW92 = show.poster_path != null ? api.getImage(show.poster_path, "w92") : null;
        byte[] posterW154 = show.poster_path != null ? api.getImage(show.poster_path, "w154") : null;

        try (Connection conn = DB.getNewTmdbConnection()) {
            try {
                tvShowRepo.insertTvShow(show, conn);
                savePoster(show.poster_path, show.id, posterW92, 92,
                        filename -> tvShowRepo.insertTvShowImage(show, 92,
                                getImageDimensions(posterW92)[1], filename, conn),
                        "Serie " + show.name);
                savePoster(show.poster_path, show.id, posterW154, 154,
                        filename -> tvShowRepo.insertTvShowImage(show, 154,
                                getImageDimensions(posterW154)[1], filename, conn),
                        "Serie " + show.name);
                tvShowRepo.insertTvShowRating(rating, comment, conn);
                processAggregatedCredits(credits, show.id, conn,
                        (cast, c) -> tvShowRepo.insertTvShowCast(cast, show.id, c),
                        (crew, job, creditId, episodeCount, c) ->
                                tvShowRepo.insertTvShowCrew(crew, show.id, job, creditId, episodeCount, c),
                        show.name);
                tvShowRepo.insertTvShowGenres(show, conn);
                tvShowRepo.insertTvShowCountries(show, conn);
                tvShowRepo.insertTvShowLanguages(show, conn);
                conn.commit();
                log.info("Serie erfolgreich importiert: " + show.name);
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Import fehlgeschlagen für Serie: " + rating.name
                        + " (id=" + rating.id + ")", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("TMDB-DB-Verbindung fehlgeschlagen", e);
        }
    }

    private void checkTvShowDataChanged(int tvShowId, String showName) {
        TvShowComparisonData dbData = tvShowRepo.loadComparisonData(tvShowId);
        if (dbData == null)
            return;
        TvShowJSON webData = api.getTvShowDetails(tvShowId);
        if (dbData.differs(webData)) {
            log.info("Seriendaten haben sich geändert: " + showName);
            Alert alert = SkinService.get().createAlert(null,
                    "Seriendaten geändert",
                    "Die Daten der Serie \"" + showName + "\" haben sich geändert.\n\n"
                    + "Seasons: " + dbData.numberOfSeasons + " → " + webData.number_of_seasons + "\n"
                    + "Episodes: " + dbData.numberOfEpisodes + " → " + webData.number_of_episodes + "\n"
                    + "Status: " + dbData.status + " → " + webData.status + "\n"
                    + "Last Air Date: " + dbData.lastAirDate + " → " + webData.last_air_date + "\n\n"
                    + "Sollen die Daten aktualisiert werden?",
                    new ButtonType("Ja", ButtonBar.ButtonData.YES),
                    new ButtonType("Nein", ButtonBar.ButtonData.NO));
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.YES) {
                tvShowRepo.updateTvShowData(webData);
                updatedShowData++;
            }
        }
    }

    // =========================================================================
    // Step 2: Episoden
    // =========================================================================

    private void processRatedEpisodes() {
        int pageNo = 0;
        EpisodeRatingsPageJSON page;
        do {
            pageNo++;
            page = api.getRatedEpisodes(pageNo);
            for (EpisodeRatingJSON rating : page.results) {
                Integer dbRating = episodeRepo.getEpisodeRating(rating.id);

                if (dbRating == null) {
                    // Neue Episode
                    ensureShowExists(rating.show_id);
                    ensureSeasonExists(rating.show_id, rating.season_number);
                    importNewEpisode(rating);
                    newEpisodes++;
                } else if (!dbRating.equals(rating.rating)) {
                    // Umbewertung
                    log.info("Episoden-Umbewertung erkannt: " + rating.name + " (id=" + rating.id + ")");
                    String existingComment = episodeRepo.getEpisodeComment(rating.id);
                    String newComment = askForComment(
                            "Episoden-Umbewertung: " + rating.name,
                            rating.name + (rating.original_name != null
                                    && !rating.original_name.equals(rating.name)
                                    ? " / " + rating.original_name : ""),
                            existingComment);
                    episodeRepo.updateEpisodeRating(rating.id, rating.rating, newComment);
                    reRatedEpisodes++;
                }
            }
        } while (pageNo < page.total_pages);
    }

    /**
     * Stellt sicher, dass die Show in der DB existiert.
     * Falls nicht: Importieren ohne Rating (über den Episodenpfad reingekommen).
     */
    private void ensureShowExists(int tvShowId) {
        if (tvShowRepo.tvShowExists(tvShowId))
            return;
        log.info("Show noch nicht in DB, importiere ohne Rating: tvShowId=" + tvShowId);
        TvShowJSON show = api.getTvShowDetails(tvShowId);
        CreditListJSON credits = api.getAggregatedTvShowCredits(tvShowId);
        byte[] posterW92 = show.poster_path != null ? api.getImage(show.poster_path, "w92") : null;
        byte[] posterW154 = show.poster_path != null ? api.getImage(show.poster_path, "w154") : null;

        try (Connection conn = DB.getNewTmdbConnection()) {
            try {
                tvShowRepo.insertTvShow(show, conn);
                savePoster(show.poster_path, show.id, posterW92, 92,
                        filename -> tvShowRepo.insertTvShowImage(show, 92,
                                getImageDimensions(posterW92)[1], filename, conn),
                        "Serie " + show.name);
                savePoster(show.poster_path, show.id, posterW154, 154,
                        filename -> tvShowRepo.insertTvShowImage(show, 154,
                                getImageDimensions(posterW154)[1], filename, conn),
                        "Serie " + show.name);
                processAggregatedCredits(credits, show.id, conn,
                        (cast, c) -> tvShowRepo.insertTvShowCast(cast, show.id, c),
                        (crew, job, creditId, episodeCount, c) ->
                                tvShowRepo.insertTvShowCrew(crew, show.id, job, creditId, episodeCount, c),
                        show.name);
                tvShowRepo.insertTvShowGenres(show, conn);
                tvShowRepo.insertTvShowCountries(show, conn);
                tvShowRepo.insertTvShowLanguages(show, conn);
                conn.commit();
                log.info("Show ohne Rating importiert: " + show.name);
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Show-Import fehlgeschlagen: " + show.name
                        + " (id=" + tvShowId + ")", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("TMDB-DB-Verbindung fehlgeschlagen", e);
        }
    }

    /**
     * Stellt sicher, dass die Season in der DB existiert.
     */
    private void ensureSeasonExists(int tvShowId, int seasonNumber) {
        if (seasonRepo.seasonExists(tvShowId, seasonNumber))
            return;
        log.info("Season noch nicht in DB, importiere: tvShowId=" + tvShowId
                + ", seasonNumber=" + seasonNumber);
        SeasonJSON season = api.getSeasonDetails(tvShowId, seasonNumber);
        CreditListJSON credits = api.getAggregatedSeasonCredits(tvShowId, seasonNumber);
        byte[] posterW92 = season.poster_path != null ? api.getImage(season.poster_path, "w92") : null;
        byte[] posterW154 = season.poster_path != null ? api.getImage(season.poster_path, "w154") : null;

        try (Connection conn = DB.getNewTmdbConnection()) {
            try {
                seasonRepo.insertSeason(season, conn);
                if (season.poster_path != null && posterW92 != null) {
                    savePoster(season.poster_path, season.id, posterW92, 92,
                            filename -> seasonRepo.insertSeasonImage(season, 92,
                                    getImageDimensions(posterW92)[1], filename, conn),
                            "Season " + season.name);
                } else {
                    seasonRepo.copyShowImageToSeason(season, conn);
                }
                if (season.poster_path != null && posterW154 != null) {
                    savePoster(season.poster_path, season.id, posterW154, 154,
                            filename -> seasonRepo.insertSeasonImage(season, 154,
                                    getImageDimensions(posterW154)[1], filename, conn),
                            "Season " + season.name);
                } else {
                    seasonRepo.copyShowImageToSeason(season, conn);
                }
                processAggregatedCredits(credits, tvShowId, conn,
                        (cast, c) -> seasonRepo.insertSeasonCast(cast, season, c),
                        (crew, job, creditId, episodeCount, c) ->
                                seasonRepo.insertSeasonCrew(crew, season, job, creditId, episodeCount, c),
                        season.name);
                // Regular-Credits holen und flippen.
                // aggregated == Episoden-Credits ∪ reguläre Season-Credits, also sind
                // alle Regulars bereits in season_to_person — wir markieren nur.
                // Findet markRegular* keine Zeile, ist die Invariante verletzt → FailFast.
                // Details: Klassen-Javadoc dieser Klasse, Abschnitt "Season Regulars".
                CreditListJSON regularCredits =
                        api.getRegularSeasonCredits(tvShowId, seasonNumber);
                for (CastJSON cast : regularCredits.cast)
                    seasonRepo.markRegularCast(cast, season.id, conn);
                for (CrewJSON crew : regularCredits.crew)
                    seasonRepo.markRegularCrew(crew, season.id, conn);
                conn.commit();
                log.info("Season importiert: " + season.name);
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Season-Import fehlgeschlagen: tvShowId=" + tvShowId
                        + ", seasonNumber=" + seasonNumber, e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("TMDB-DB-Verbindung fehlgeschlagen", e);
        }
    }

    private void importNewEpisode(EpisodeRatingJSON rating) {
        log.info("Importiere neue Episode: " + rating.name + " (id=" + rating.id + ")");
        EpisodeJSON episode = api.getEpisodeDetails(rating.show_id,
                rating.season_number, rating.episode_number);

        // Kommentar abfragen
        String title = episode.name;
        if (episode.german_name != null && !episode.german_name.equals(episode.name))
            title = episode.name + " / " + episode.german_name;
        String comment = askForComment("Neue Episode bewertet", title, null);

        try (Connection conn = DB.getNewTmdbConnection()) {
            try {
                episodeRepo.insertEpisode(episode, conn);
                processEpisodeCredits(episode, conn);
                episodeRepo.insertEpisodeRating(episode.id, rating.rating, comment,
                        rating.first_rated_at != null ? rating.first_rated_at.toString()
                                : LocalDate.now().toString(),
                        null, conn);
                conn.commit();
                log.info("Episode importiert: " + episode.name);
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Episode-Import fehlgeschlagen: " + rating.name
                        + " (id=" + rating.id + ")", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("TMDB-DB-Verbindung fehlgeschlagen", e);
        }

        // Flag-Dialog — nach dem Commit, weil die Views die gespeicherten Daten brauchen
        showFlagDialog(episode.id, title);
    }

    /**
     * Verarbeitet die Credits einer Episode: Guest-Stars und Crew.
     * Crew-Jobs werden direkt gegen Whitelist/Blacklist geprüft.
     */
    private void processEpisodeCredits(EpisodeJSON episode, Connection conn) {
        // Guest Stars
        if (episode.guest_stars != null) {
            for (CastJSON cast : episode.guest_stars) {
                movieRepo.insertPersonIfNotExists(api.getPerson(cast.id), conn);
                episodeRepo.insertEpisodeCast(cast, episode.id, conn);
            }
        }
        // Crew
        if (episode.crew != null) {
            for (CrewJSON crew : episode.crew) {
                String job = crew.getJob();
                if (crewFilterRepo.isBlacklisted(job)) {
                    log.fine("Crew blacklisted: " + crew.name + ", job=" + job);
                } else if (crewFilterRepo.isWhitelisted(job)) {
                    movieRepo.insertPersonIfNotExists(api.getPerson(crew.id), conn);
                    episodeRepo.insertEpisodeCrew(crew, episode.id, conn);
                } else {
                    boolean whitelist = askWhitelistOrBlacklist(crew.name, job, crew.department,
                            episode.name);
                    if (whitelist) {
                        crewFilterRepo.addToWhitelist(job, conn);
                        movieRepo.insertPersonIfNotExists(api.getPerson(crew.id), conn);
                        episodeRepo.insertEpisodeCrew(crew, episode.id, conn);
                    } else {
                        crewFilterRepo.addToBlacklist(job, conn);
                    }
                }
            }
        }
    }

    // =========================================================================
    // Step 3: Lücken-Check
    // =========================================================================

    private void processGaps() {
        // Filme ohne Overview
        processMoviesWithoutOverview();
        // Filme ohne Poster
        processMoviesWithoutPoster();
        // Serien ohne Overview
        processShowsWithoutOverview();
        // Serien ohne Poster
        processShowsWithoutPoster();
        // Episoden ohne Overview
        processEpisodesWithoutOverview();
    }

    private void processMoviesWithoutOverview() {
        List<Integer> movies = movieRepo.getMoviesWithoutOverview();
        for (Integer id : movies) {
            try {
                var movieDetails = api.getMovieDetails(id);
                if (movieDetails.overview != null && !movieDetails.overview.isEmpty()) {
                    movieRepo.updateMovieOverview(id, movieDetails.overview);
                    overviewsFound++;
                    log.info("Overview nachgeholt für Film id=" + id);
                }
            } catch (Exception e) {
                log.warning("Overview-Check fehlgeschlagen für Film id=" + id + ": " + e.getMessage());
            }
        }
    }

    private void processMoviesWithoutPoster() {
    	List<Integer> movies = movieRepo.getMoviesWithoutPoster();
        for (Integer id : movies) {
            try {
                var movieDetails = api.getMovieDetails(id);
                if (movieDetails.poster_path != null) {
                    byte[] posterW92 = api.getImage(movieDetails.poster_path, "w92");
                    byte[] posterW154 = api.getImage(movieDetails.poster_path, "w154");
                    if (posterW92 != null) {
                        int[] dim = getImageDimensions(posterW92);
                        String filename = buildImageFilename(movieDetails.poster_path, "en-US",
                                dim[0], dim[1]);
                        saveImageToFileSystem(filename, posterW92);
                        movieRepo.updateMoviePoster(id, dim[0], dim[1], "en-US", movieDetails.poster_path.substring(1), filename);
                    }
                    if (posterW154 != null) {
                        int[] dim = getImageDimensions(posterW154);
                        String filename = buildImageFilename(movieDetails.poster_path, "en-US",
                                dim[0], dim[1]);
                        saveImageToFileSystem(filename, posterW154);
                        movieRepo.updateMoviePoster(id, dim[0], dim[1], "en-US", movieDetails.poster_path.substring(1), filename);
                    }
                    postersFound++;
                    movieRepo.updateMoviePosterPath(id, movieDetails.poster_path);
                    log.info("Poster nachgeholt für Film id=" + id);
                }
            } catch (Exception e) {
                log.warning("Poster-Check fehlgeschlagen für Film id=" + id + ": " + e.getMessage());
            }
        }
    }
    
    private void processShowsWithoutOverview() {
        List<Integer> shows = tvShowRepo.getShowsWithoutOverview();
        for (Integer id : shows) {
            try {
                var showDetails = api.getTvShowDetails(id);
                if (showDetails.overview != null && !showDetails.overview.isEmpty()) {
                    tvShowRepo.updateOverview(id, showDetails.overview);
                    overviewsFound++;
                    log.info("Overview nachgeholt für Serie id=" + id);
                }
            } catch (Exception e) {
                log.warning("Overview-Check fehlgeschlagen für Serie id=" + id + ": " + e.getMessage());
            }
        }
    }

    private void processShowsWithoutPoster() {
        List<Integer> shows = tvShowRepo.getShowsWithoutPoster();
        for (Integer id : shows) {
            try {
                var showDetails = api.getTvShowDetails(id);
                if (showDetails.poster_path != null) {
                    byte[] posterW92 = api.getImage(showDetails.poster_path, "w92");
                    byte[] posterW154 = api.getImage(showDetails.poster_path, "w154");
                    if (posterW92 != null) {
                        int[] dim = getImageDimensions(posterW92);
                        String filename = buildImageFilename(showDetails.poster_path, "en-US", dim[0], dim[1]);
                        saveImageToFileSystem(filename, posterW92);
                        tvShowRepo.insertTvShowImage(id, showDetails.poster_path, dim[0], dim[1], "en-US", filename);
                    }
                    if (posterW154 != null) {
                        int[] dim = getImageDimensions(posterW154);
                        String filename = buildImageFilename(showDetails.poster_path, "en-US", dim[0], dim[1]);
                        saveImageToFileSystem(filename, posterW154);
                        tvShowRepo.insertTvShowImage(id, showDetails.poster_path, dim[0], dim[1], "en-US", filename);
                    }
                    postersFound++;
                    tvShowRepo.updatePosterPath(id, showDetails.poster_path);
                    log.info("Poster nachgeholt für Serie id=" + id);
                }
            } catch (Exception e) {
                log.warning("Poster-Check fehlgeschlagen für Serie id=" + id + ": " + e.getMessage());
            }
        }
    }

    private void processEpisodesWithoutOverview() {
        List<TmdbEpisodeRepository.EpisodeForApi> episodes = episodeRepo.getEpisodesWithoutOverview();
        for (var ep : episodes) {
            try {
                var episodeDetails = api.getEpisodeDetails(ep.tvShowId(), ep.seasonNumber(), ep.episodeNumber());
                if (episodeDetails.overview != null && !episodeDetails.overview.isEmpty()) {
                    episodeRepo.updateOverview(episodeDetails.id, episodeDetails.overview);
                    overviewsFound++;
                    log.info("Overview nachgeholt für Episode showId=" + ep.tvShowId()
                            + " S" + ep.seasonNumber() + "E" + ep.episodeNumber());
                }
            } catch (Exception e) {
                log.warning("Overview-Check fehlgeschlagen für Episode showId=" + ep.tvShowId()
                        + " S" + ep.seasonNumber() + "E" + ep.episodeNumber() + ": " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // Aggregierte Credits verarbeiten (Shows und Seasons)
    // =========================================================================

    @FunctionalInterface
    private interface CastInsert {
        void insert(CastJSON cast, Connection conn);
    }

    @FunctionalInterface
    private interface CrewInsert {
        void insert(CrewJSON crew, String job, String creditId, int episodeCount, Connection conn);
    }

    /**
     * Verarbeitet aggregierte Credits (Serien oder Staffeln).
     * Cast: Personen laden, einfügen. Crew: Whitelist/Blacklist prüfen, ggf. fragen.
     */
    private void processAggregatedCredits(CreditListJSON credits, int parentId,
            Connection conn, CastInsert castInsert, CrewInsert crewInsert, String contextName) {
        // Cast
        for (CastJSON cast : credits.cast) {
            movieRepo.insertPersonIfNotExists(api.getPerson(cast.id), conn);
            castInsert.insert(cast, conn);
        }
        // Crew
        for (CrewJSON crew : credits.crew) {
            for (JobJSON jobEntry : crew.jobs) {
                String job = jobEntry.job;
                if (crewFilterRepo.isBlacklisted(job)) {
                    log.fine("Crew blacklisted: " + crew.name + ", job=" + job);
                } else if (crewFilterRepo.isWhitelisted(job)) {
                    movieRepo.insertPersonIfNotExists(api.getPerson(crew.id), conn);
                    crewInsert.insert(crew, job, jobEntry.credit_id, jobEntry.episode_count, conn);
                } else {
                    boolean whitelist = askWhitelistOrBlacklist(crew.name, job, crew.department,
                            contextName);
                    if (whitelist) {
                        crewFilterRepo.addToWhitelist(job, conn);
                        movieRepo.insertPersonIfNotExists(api.getPerson(crew.id), conn);
                        crewInsert.insert(crew, job, jobEntry.credit_id, jobEntry.episode_count, conn);
                    } else {
                        crewFilterRepo.addToBlacklist(job, conn);
                    }
                }
            }
        }
    }

    // =========================================================================
    // Flag-Alert
    // =========================================================================

    /**
     * Fragt nach der Art der Bewertung: ganze Staffel oder nur diese Episode.
     * Keine Vorschau — die fertige Karte wird anschließend im Viewer
     * kontrolliert. Bei Bedarf kann das Flag direkt in der DB angepasst werden.
     */
    private void showFlagDialog(int episodeId, String title) {
        ButtonType btnSeason = new ButtonType("Ganze Staffel", ButtonBar.ButtonData.OTHER);
        ButtonType btnEpisode = new ButtonType("Nur diese Episode", ButtonBar.ButtonData.OTHER);
 
        Alert alert = SkinService.get().createAlert(null,
                "Art der Bewertung",
                "Bezieht sich die Bewertung auf die ganze Staffel oder nur auf diese Episode?\n\n"
                + title + "\n\n"
                + "Hinweis: Die Karte anschließend im Viewer kontrollieren. Stimmt das Flag "
                + "nicht, kann rated_season direkt in der DB angepasst werden.",
                btnSeason, btnEpisode);
 
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty())
            throw new RuntimeException("Flag-Dialog wurde ohne Auswahl geschlossen. episodeId=" + episodeId);
 
        episodeRepo.updateEpisodeFlags(episodeId, result.get() == btnSeason);
    }

    // =========================================================================
    // Dialoge
    // =========================================================================

    private void showStepAlert(String message) {
        Alert alert = SkinService.get().createAlert(null, "TMDB Import", message,
                new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
        alert.showAndWait();
    }

    private void showSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Import abgeschlossen.\n\n");
        if (newShows > 0) sb.append("Neue Serien: ").append(newShows).append("\n");
        if (reRatedShows > 0) sb.append("Umbewertete Serien: ").append(reRatedShows).append("\n");
        if (updatedShowData > 0) sb.append("Aktualisierte Seriendaten: ").append(updatedShowData).append("\n");
        if (newEpisodes > 0) sb.append("Neue Episoden: ").append(newEpisodes).append("\n");
        if (reRatedEpisodes > 0) sb.append("Umbewertete Episoden: ").append(reRatedEpisodes).append("\n");
        if (postersFound > 0) sb.append("Nachgeholte Poster: ").append(postersFound).append("\n");
        if (overviewsFound > 0) sb.append("Nachgeholte Zusammenfassungen: ").append(overviewsFound).append("\n");
        if (newShows + reRatedShows + updatedShowData + newEpisodes + reRatedEpisodes
                + postersFound + overviewsFound == 0)
            sb.append("Nichts Neues gefunden.");

        log.info("Zusammenfassung: " + sb.toString());
        Alert alert = SkinService.get().createAlert(null, "TMDB Import — Zusammenfassung",
                sb.toString().trim(),
                new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
        alert.showAndWait();
    }

    private String askForComment(String dialogTitle, String description, String existingComment) {
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefRowCount(6);
        textArea.setPrefColumnCount(40);
        if (existingComment != null && !".".equals(existingComment))
            textArea.setText(existingComment);

        Dialog<?> dialog = SkinService.get().createDialog(null, dialogTitle);
        VBox content = SkinService.get().createDialogContent();
        content.getChildren().add(new Label(description));
        content.getChildren().add(textArea);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<?> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().equals(ButtonType.CANCEL))
            return ".";

        String text = textArea.getText();
        return (text == null || text.isBlank()) ? "." : text.trim();
    }

    private boolean askWhitelistOrBlacklist(String personName, String job,
            String department, String contextName) {
        ButtonType btnWhitelist = new ButtonType("Whitelist", ButtonBar.ButtonData.YES);
        ButtonType btnBlacklist = new ButtonType("Blacklist", ButtonBar.ButtonData.NO);

        Alert alert = SkinService.get().createAlert(null,
                "Unbekannter Crew-Job",
                "Person: " + personName + "\n"
                + "Job: " + job + "\n"
                + "Department: " + department + "\n"
                + "Kontext: " + contextName,
                btnWhitelist, btnBlacklist);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty())
            throw new RuntimeException("Crew-Dialog ohne Auswahl geschlossen. job=" + job);

        return result.get() == btnWhitelist;
    }

    // =========================================================================
    // Bild-Hilfsmethoden
    // =========================================================================

    @FunctionalInterface
    private interface ImageDbInsert {
        void insert(String filename) throws Exception;
    }

    /**
     * Speichert ein Poster im Dateisystem und ruft den DB-Insert-Callback auf.
     * Zeigt einen Alert wenn kein Poster vorhanden.
     */
    private void savePoster(String posterPath, int entityId, byte[] imageData,
            int targetWidth, ImageDbInsert dbInsert, String contextName) {
        if (imageData == null || posterPath == null) {
            SkinService.get().createAlert(SkinService.getOwnerWindow(),
                    targetWidth + "er Poster fehlt",
                    "Für " + contextName, false, false);
            return;
        }
        try {
            int[] dim = getImageDimensions(imageData);
            String filename = buildImageFilename(posterPath, "en-US", dim[0], dim[1]);
            saveImageToFileSystem(filename, imageData);
            dbInsert.insert(filename);
        } catch (Exception e) {
            throw new RuntimeException("savePoster fehlgeschlagen. contextName=" + contextName, e);
        }
    }

    private static void saveImageToFileSystem(String filename, byte[] image) {
        File file = new File(Config.get("imageFolder") + "tmdb" + File.separator + filename);
        if (file.exists())
            return; // Bei Seasons kann dasselbe Bild schon durch die Show existieren
        try {
            file.getParentFile().mkdirs();
            java.nio.file.Files.write(file.toPath(), image);
            log.fine("Bild gespeichert: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("saveImageToFileSystem fehlgeschlagen. filename: " + filename, e);
        }
    }

    private static int[] getImageDimensions(byte[] imageData) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
            return new int[]{img.getWidth(), img.getHeight()};
        } catch (Exception e) {
            throw new RuntimeException("getImageDimensions fehlgeschlagen", e);
        }
    }

    private static String buildImageFilename(String posterPath, String language,
            int width, int height) {
        String base = posterPath.startsWith("/") ? posterPath.substring(1) : posterPath;
        base = base.substring(0, base.lastIndexOf('.'));
        return base + "_" + language + "_" + width + "_" + height + ".jpg";
    }
}