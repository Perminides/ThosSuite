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
 * Wird über einen Menübutton ausgelöst. Läuft komplett auf dem FX-Thread.
 * Interaktive Dialoge (Kommentar, Flags, Crew-Whitelist) erscheinen inline.
 *
 * Ablauf:
 * 1. Step-Alert → Serien-Check (neue, Umbewertungen, Daten-Update)
 * 2. Step-Alert → Episoden-Check (neue mit Kaskade, Flags, Kommentar, Umbewertungen)
 * 3. Step-Alert → Lücken-Check (fehlende Poster/Overviews nachholen)
 * 4. Abschluss-Alert mit Zusammenfassung
 *
 * Alle Fehler sind fatal — kein stiller Fallback.
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
                        null, null, null, conn);
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
        showFlagDialog(episode.id);
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
    // Flag-Dialog
    // =========================================================================

    /**
     * Zeigt vier Vorschau-Karten für die Flag-Auswahl.
     * Baut jede Karte mit den passenden Credits aus den jeweiligen Views.
     */
    private void showFlagDialog(int episodeId) {
        // Vier Flag-Kombinationen
        String[][] flagCombos = {
            // {label, ratedSeason, actorsFromShow, directorsFromShow}
            {"1 - Staffel bewertet", "1", "null", "null"},
            {"2 - Nur diese Episode", "0", "0", "0"},
            {"3 - Episode, Schauspieler von der Show", "0", "1", "0"},
            {"4 - Abgebrochene Serie", "0", "1", "1"},
        };

        // Karten bauen
        List<CardData> cards = new ArrayList<>();
        for (String[] combo : flagCombos) {
            boolean ratedSeason = "1".equals(combo[1]);
            boolean actorsFromShow = "1".equals(combo[2]);
            boolean directorsFromShow = "1".equals(combo[3]);
            cards.add(buildPreviewCard(episodeId, ratedSeason, actorsFromShow, directorsFromShow));
        }

        // Dialog zusammenbauen
        VBox cardBox = new VBox(10);
        for (int i = 0; i < cards.size(); i++) {
            Label label = new Label(flagCombos[i][0]);
            label.setStyle("-fx-font-weight: bold;");
            Pane card = SkinService.get().createCard(cards.get(i), _ -> {}, _ -> {});
            cardBox.getChildren().addAll(label, card);
        }

        ScrollPane scrollPane = new ScrollPane(cardBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefHeight(800);
        scrollPane.getStyleClass().add("my-dialog-scrollpane");

        ButtonType btn1 = new ButtonType("1 - Staffel", ButtonBar.ButtonData.OTHER);
        ButtonType btn2 = new ButtonType("2 - Episode", ButtonBar.ButtonData.OTHER);
        ButtonType btn3 = new ButtonType("3 - Ep+ShowCast", ButtonBar.ButtonData.OTHER);
        ButtonType btn4 = new ButtonType("4 - Abgebrochen", ButtonBar.ButtonData.OTHER);

        Alert alert = SkinService.get().createAlert(null,
                "Wähle die Art der Bewertung",
                "", btn1, btn2, btn3, btn4);
        alert.getDialogPane().setContent(scrollPane);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty())
            throw new RuntimeException("Flag-Dialog wurde ohne Auswahl geschlossen. episodeId=" + episodeId);

        ButtonType chosen = result.get();
        if (chosen == btn1)
            episodeRepo.updateEpisodeFlags(episodeId, true, null, null);
        else if (chosen == btn2)
            episodeRepo.updateEpisodeFlags(episodeId, false, false, false);
        else if (chosen == btn3)
            episodeRepo.updateEpisodeFlags(episodeId, false, true, false);
        else if (chosen == btn4)
            episodeRepo.updateEpisodeFlags(episodeId, false, true, true);
    }

    /**
     * Baut eine Vorschau-Karte für den Flag-Dialog mit den passenden Credits.
     */
    private CardData buildPreviewCard(int episodeId, boolean ratedSeason,
            boolean actorsFromShow, boolean directorsFromShow) {
        Connection conn = DB.getTmdbConnection();
        int actorsToShow = Config.getInt("tmdb.actorsToShow", 10);
        int directorsToShow = Config.getInt("tmdb.directorsToShow", 5);

        // Actor-View wählen
        String actorView;
        if (ratedSeason) actorView = "episode_actors_from_season";
        else if (actorsFromShow) actorView = "episode_actors_from_show";
        else actorView = "episode_actors_from_episode";

        // Director-View wählen
        String directorView;
        if (ratedSeason) directorView = "episode_directors_from_season";
        else if (directorsFromShow) directorView = "episode_directors_from_show";
        else directorView = "episode_directors_from_episode";

        List<String> actors = loadPreviewNames(conn,
                "SELECT name FROM " + actorView + " WHERE id = ? ORDER BY position ASC",
                episodeId, actorsToShow);
        List<String> directors = loadPreviewNames(conn,
                "SELECT name FROM " + directorView + " WHERE id = ? ORDER BY position DESC",
                episodeId, directorsToShow);

        // Episode-Details laden
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM episode_details WHERE episode_id = ?")) {
            ps.setInt(1, episodeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new RuntimeException("Episode nicht in episode_details. episodeId=" + episodeId);

                String seasonReleaseDateStr = rs.getString("release_date");
                LocalDate seasonFirstAirDate = seasonReleaseDateStr != null
                        ? LocalDate.parse(seasonReleaseDateStr) : null;
                String episodeReleaseDateStr = rs.getString("episode_release_date");
                LocalDate episodeAirDate = episodeReleaseDateStr != null
                        ? LocalDate.parse(episodeReleaseDateStr) : null;

                boolean useSeasonOverview = ratedSeason || (actorsFromShow && directorsFromShow);
                String overview = useSeasonOverview
                        ? rs.getString("season_overview")
                        : rs.getString("overview");

                String imageFilename = rs.getString("season_image_filename");
                if (imageFilename == null)
                    imageFilename = rs.getString("show_image_filename");

                return CardData.forEpisode(
                        episodeId,
                        rs.getString("show_name"),
                        rs.getString("show_german_name"),
                        rs.getString("season_name"),
                        rs.getString("season_german_name"),
                        rs.getString("episode_name"),
                        rs.getString("episode_german_name"),
                        rs.getInt("season_number"),
                        rs.getInt("episode_number"),
                        seasonFirstAirDate,
                        episodeAirDate,
                        ratedSeason,
                        rs.getInt("rating"),
                        LocalDate.parse(rs.getString("ratingInsertDate")),
                        directors,
                        actors,
                        overview,
                        rs.getString("comment"),
                        imageFilename);
            }
        } catch (Exception e) {
            throw new RuntimeException("buildPreviewCard fehlgeschlagen. episodeId=" + episodeId, e);
        }
    }

    private List<String> loadPreviewNames(Connection conn, String sql, int id, int limit) {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next() && count < limit) {
                    names.add(rs.getString("name"));
                    count++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("loadPreviewNames fehlgeschlagen. id: " + id, e);
        }
        return names;
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