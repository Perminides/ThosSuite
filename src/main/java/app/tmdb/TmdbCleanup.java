package app.tmdb;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import app.data.persistence.TmdbCrewFilterRepository;
import app.data.persistence.TmdbMovieRepository;
import app.data.persistence.TmdbMovieRepository.NullCommentEntry;
import app.data.persistence.TmdbPendingRepository;
import app.data.persistence.TmdbPendingRepository.CrewPendingEntry;
import app.ui.skin.SkinService;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

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
public class TmdbCleanup {

    private static final Logger log = Logger.getLogger(TmdbCleanup.class.getName());

    private final TmdbCrewFilterRepository crewFilterRepo;
    private final TmdbPendingRepository pendingRepo;
    private final TmdbMovieRepository movieRepo;

    public TmdbCleanup() {
        this.crewFilterRepo = new TmdbCrewFilterRepository();
        this.pendingRepo = new TmdbPendingRepository();
        this.movieRepo = new TmdbMovieRepository();
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
        ButtonType btnWhitelist = new ButtonType("Whitelist", ButtonBar.ButtonData.YES);
        ButtonType btnBlacklist = new ButtonType("Blacklist", ButtonBar.ButtonData.NO);

        Alert alert = SkinService.get().createAlert(
            null,
            "Unbekannter Crew-Job",
            "Person: " + entry.personName + "\n" +
            "Job: " + entry.job + "\n" +
            "Department: " + entry.department + "\n" +
            "Film: " + entry.movieTitle,
            btnWhitelist, btnBlacklist
        );

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty())
            throw new RuntimeException("Crew-Pending-Dialog wurde ohne Auswahl geschlossen. personId=" + entry.personId + ", job=" + entry.job);

        return result.get() == btnWhitelist;
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
            movieRepo.saveComment(entry.movieId, comment);
        }
    }

    /**
     * Zeigt eine TextArea für die Kommentareingabe zu einem Film.
     * Leere Eingabe → ".".
     */
    private String askForComment(NullCommentEntry entry) {
        ButtonType btnOk = new ButtonType("OK", ButtonBar.ButtonData.YES);
        ButtonType btnSkip = new ButtonType("Überspringen (Punkt)", ButtonBar.ButtonData.NO);

        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefRowCount(6);
        textArea.setPrefColumnCount(40);

        Alert alert = SkinService.get().createAlert(
            null,
            "Kommentar eingeben",
            "Film: " + entry.title + " (" + entry.germanTitle + ")\n" +
            "Bewertet am: " + entry.firstRatedAt + "\n" +
            "Bewertung: " + entry.rating,
            btnOk, btnSkip
        );
        alert.getDialogPane().setExpandableContent(textArea);
        alert.getDialogPane().setExpanded(true);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == btnSkip)
            return ".";

        String text = textArea.getText();
        return (text == null || text.isBlank()) ? "." : text.trim();
    }

    /**
     * Zeigt am Ende eine Liste aller Filme mit "."-Kommentar.
     */
    private void showDotCommentSummary() {
        List<String> titles = movieRepo.loadDotCommentTitles();
        if (titles.isEmpty()) {
            log.info("Keine Filme mit Punkt-Kommentar.");
            return;
        }

        String message = "Folgende Filme haben noch keinen richtigen Kommentar (\".\").\n" +
                "Bitte in der DB nachpflegen:\n\n" +
                String.join("\n", titles);

        Alert alert = SkinService.get().createAlert(
            null,
            "Offene Kommentare",
            message,
            new ButtonType("OK", ButtonBar.ButtonData.OK_DONE)
        );
        alert.showAndWait();
    }
}