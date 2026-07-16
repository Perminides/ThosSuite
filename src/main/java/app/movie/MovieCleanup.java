package app.movie;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import app.movie.model.CrewPendingEntry;
import app.movie.model.NullCommentEntry;
import app.movie.repository.CrewFilterRepository;
import app.movie.repository.EpisodeRepository;
import app.movie.repository.MovieRepository;
import app.movie.repository.PendingRepository;
import app.movie.repository.TvShowRepository;
import app.shared.model.DialogButton;
import app.shared.skin.SkinService;
import app.shared.ui.dialog.TextPromptDialog;

/**
 * PostTask für den TMDB-Import.
 *
 * Ablauf:
 * 1. Offene Jobs in crew_pending abarbeiten — pro Job entscheiden: whitelist oder blacklist.
 *    Danach Person von person_pending in person übertragen und movie_to_person-Eintrag schreiben.
 * 2. Filme mit null-Kommentar der letzten 100 Tage einzeln per TextArea abfragen.
 *    Kein Kommentar eingegeben → ".".
 * 3. Am Ende Liste aller Filme mit "."-Kommentar anzeigen als Erinnerung.
 *
 * Kein Netzwerkzugriff. Kein getNewTmdbConnection — jede Operation ist atomar für sich.
 */
public class MovieCleanup {

    private static final Logger log = Logger.getLogger(MovieCleanup.class.getName());

    private final CrewFilterRepository crewFilterRepo;
    private final PendingRepository pendingRepo;
    private final MovieRepository movieRepo;
    private final TvShowRepository tvShowRepo;
    private final EpisodeRepository episodeRepo;

    public MovieCleanup() {
        this.crewFilterRepo = new CrewFilterRepository();
        this.pendingRepo = new PendingRepository();
        this.movieRepo = new MovieRepository();
        this.tvShowRepo = new TvShowRepository();
        this.episodeRepo = new EpisodeRepository();
    }

    public void run() {
        log.info("TmdbImportReview gestartet");
        crewFilterRepo.load();
        processPendingCrew();
        processNullComments();
        showDotCommentSummary();
        log.info("TmdbImportReview abgeschlossen");
    }

    /**
     * Iteriert alle Einträge in crew_pending.
     * Prüft vor jeder Frage ob der Job mittlerweile bereits eingeordnet wurde.
     * Fragt nur nach wenn der Job noch unbekannt ist.
     */
    private void processPendingCrew() {
        List<CrewPendingEntry> entries = pendingRepo.loadCrewPendingEntries();
        if (entries.isEmpty()) {
            log.info("Keine offenen Crew-Pending-Einträge.");
            return;
        }
        log.info("Offene Crew-Pending-Einträge: " + entries.size());

        for (CrewPendingEntry entry : entries) {
            if (crewFilterRepo.isWhitelisted(entry.job)) {
                log.info("Job mittlerweile whitelisted, verarbeite direkt: " + entry.job);
                transferAndInsert(entry);
            } else if (crewFilterRepo.isBlacklisted(entry.job)) {
                log.info("Job mittlerweile blacklisted, überspringe: " + entry.job);
                cleanupPending(entry);
            } else {
                boolean whitelist = askWhitelistOrBlacklist(entry);
                if (whitelist) {
                    crewFilterRepo.addToWhitelist(entry.job);
                    transferAndInsert(entry);
                } else {
                    crewFilterRepo.addToBlacklist(entry.job);
                    cleanupPending(entry);
                }
            }
        }
    }

    /**
     * Überträgt Person von person_pending in person (falls noch nicht geschehen)
     * und schreibt den movie_to_person-Eintrag.
     */
    private void transferAndInsert(CrewPendingEntry entry) {
        log.info("transferAndInsert, personId=" + entry.personId + ", job=" + entry.job + ", movieId=" + entry.movieId);
        pendingRepo.transferPersonToMain(entry.personId);
        movieRepo.insertMovieCrew(entry.movieId, entry.personId, entry.creditId, entry.department, entry.job);
        pendingRepo.deleteCrewPending(entry.movieId, entry.personId, entry.job);
    }

    /**
     * Räumt pending-Einträge auf ohne etwas zu übertragen (blacklist-Fall).
     */
    private void cleanupPending(CrewPendingEntry entry) {
        log.info("cleanupPending, personId=" + entry.personId + ", job=" + entry.job);
        pendingRepo.deleteCrewPending(entry.movieId, entry.personId, entry.job);
        if (!pendingRepo.hasMoreCrewPendingForPerson(entry.personId))
            pendingRepo.deletePersonPending(entry.personId);
    }

    /**
     * Zeigt einen Alert mit Whitelist/Blacklist-Auswahl für einen unbekannten Job.
     * @return true = whitelist, false = blacklist
     */
    private boolean askWhitelistOrBlacklist(CrewPendingEntry entry) {
        DialogButton result = SkinService.get().showAlert(
            "Unbekannter Crew-Job",
            "Person: " + entry.personName + "\n" +
            "Job: " + entry.job + "\n" +
            "Department: " + entry.department + "\n" +
            "Film: " + entry.movieTitle,
            DialogButton.WHITELIST, DialogButton.BLACKLIST
        );
        if (result != DialogButton.WHITELIST && result != DialogButton.BLACKLIST)
            throw new RuntimeException("Crew-Pending-Dialog wurde ohne Auswahl geschlossen. personId=" + entry.personId + ", job=" + entry.job);

        return result == DialogButton.WHITELIST;
    }

    /**
     * Fragt für alle Filme mit null-Kommentar der letzten 100 Tage nach einem Kommentar.
     */
    private void processNullComments() {
        List<NullCommentEntry> entries = movieRepo.loadNullCommentEntries();
        if (entries.isEmpty()) {
            log.info("Keine Filme mit null-Kommentar in den letzten 100 Tagen.");
            return;
        }
        log.info("Filme mit null-Kommentar: " + entries.size());

        for (NullCommentEntry entry : entries) {
            String comment = askForComment(entry);
            movieRepo.saveComment(entry.movieId, (comment == null || comment.isBlank()) ? "." : comment.trim());
        }
    }

    /**
     * Zeigt eine TextArea für die Kommentareingabe zu einem Film.
     */
    private String askForComment(NullCommentEntry entry) {
        String header = "Film: " + entry.title + " (" + entry.germanTitle + ")\n" +
                        "Bewertet am: " + entry.firstRatedAt + "\n" +
                        "Bewertung: " + entry.rating;
        Optional<String> result = TextPromptDialog.show("Kommentar eingeben", header, null);
        if (result.isEmpty())
            return ".";
        String text = result.get();
        return text.isBlank() ? "." : text.trim();
    }

    /**
     * Zeigt am Ende eine Liste aller Filme mit "."-Kommentar.
     */
    private void showDotCommentSummary() {
        List<String> movieTitles = movieRepo.loadDotCommentTitles();
        List<String> showTitles = tvShowRepo.loadDotCommentTitles();
        List<String> episodeTitles = episodeRepo.loadDotCommentTitles();

        if (movieTitles.isEmpty() && showTitles.isEmpty() && episodeTitles.isEmpty()) {
            log.info("Keine Einträge mit Punkt-Kommentar.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Folgende Einträge haben noch keinen richtigen Kommentar (\".\"):\n\n");
        if (!movieTitles.isEmpty()) {
            sb.append("Filme:\n");
            sb.append(String.join("\n", movieTitles));
            sb.append("\n\n");
        }
        if (!showTitles.isEmpty()) {
            sb.append("Serien:\n");
            sb.append(String.join("\n", showTitles));
            sb.append("\n\n");
        }
        if (!episodeTitles.isEmpty()) {
            sb.append("Episoden:\n");
            sb.append(String.join("\n", episodeTitles));
        }

        SkinService.get().showAlert(
            "Offene Kommentare",
            sb.toString().trim(),
            DialogButton.OK
        );
    }
}