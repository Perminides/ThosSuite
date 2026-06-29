package scripts.tmdb;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import app.movie.ApiClient;
import app.movie.model.json.CastJSON;
import app.movie.model.json.CreditListJSON;
import app.movie.model.json.CrewJSON;
import app.movie.model.json.EpisodeJSON;
import app.movie.model.json.JobJSON;
import app.movie.model.json.RoleJSON;
import app.shared.Config;

/**
 * Wegwerf-Analyse: Vergleicht die aggregierten Season-Credits mit der Vereinigung
 * aller Episoden-Credits dieser Staffel.
 *
 * Prüft die Annahme "aggregated == Vereinigung aller Episode-Credits" in BEIDE
 * Richtungen:
 *   A) in aggregated, aber in keiner Episode  (die ominöse Quelle)
 *   B) in einer Episode/Regular, aber nicht in aggregated  (der Flip-Crash-Fall)

 * Die Vergleichsmenge umfasst Episoden-Credits UND die regulaeren Season-Credits,
 * weil aggregated die Vereinigung aus beidem ist (Regulars haengen nicht an
 * einzelnen Episoden). A/C sollte daher leer sein — ist es das nicht, gibt es
 * eine echte dritte Quelle.
 *
 * Prüft zusätzlich die credit_id-Eindeutigkeit: Gibt es eine (person_id, credit_id),
 * die mit mehr als einem Job bzw. Character vorkommt? Wenn ja, ist credit_id NICHT
 * pro Job/Rolle eindeutig — relevant für flipRegular, das nur auf credit_id matcht.
 *
 * Schreibt einen Report auf den Desktop. Keine DB-Schreibzugriffe.
 */
public class AggregatedCreditsProbe {

    private static final Logger log = Logger.getLogger(AggregatedCreditsProbe.class.getName());

    private static final int SERIES_ID = 90705;           // <-- ausfüllen
    private static final int SEASON_NUMBER = 1;       // <-- ausfüllen
    private static final int NUMBER_OF_EPISODES = 5;  // <-- ausfüllen

    private final ApiClient api = new ApiClient();

    // credit_id-Eindeutigkeit: (person_id|credit_id) -> Menge der Jobs/Characters
    private final Map<String, Set<String>> creditIdToJobs = new LinkedHashMap<>();
    private final Map<String, Set<String>> creditIdToChars = new LinkedHashMap<>();

    public static void main(String[] args) {
        new AggregatedCreditsProbe().run();
    }

    public void run() {
    	Config.init("C:/Users/permi/Documents/Gedächtnis Lernen und so/ThosSuite/");
        log.info("AggregatedCreditsProbe: seriesId=" + SERIES_ID
                + ", season=" + SEASON_NUMBER + ", episodes=" + NUMBER_OF_EPISODES);

        // 1) Aggregierte Season-Credits, aufgefaltet
        CreditListJSON agg = api.getAggregatedSeasonCredits(SERIES_ID, SEASON_NUMBER);
        Set<String> aggCast = new LinkedHashSet<>();
        Set<String> aggCrew = new LinkedHashSet<>();
        for (CastJSON c : agg.cast)
            for (RoleJSON r : c.getRoles()) {
                aggCast.add(castKey(c.id, c.name, r.credit_id, r.character));
                trackChar(c.id, r.credit_id, r.character);
            }
        for (CrewJSON c : agg.crew)
            for (JobJSON j : c.jobs) {
                aggCrew.add(crewKey(c.id, c.name, j.credit_id, j.job));
                trackJob(c.id, j.credit_id, j.job);
            }

        // 2) Vereinigung aller Episoden-Credits, flach
        Set<String> epCast = new LinkedHashSet<>();
        Set<String> epCrew = new LinkedHashSet<>();
        for (int ep = 1; ep <= NUMBER_OF_EPISODES; ep++) {
            EpisodeJSON episode = api.getEpisodeDetails(SERIES_ID, SEASON_NUMBER, ep);
            if (episode.guest_stars != null)
                for (CastJSON c : episode.guest_stars) {
                    epCast.add(castKey(c.id, c.name, c.getCredit_id(), c.getCharacter()));
                    trackChar(c.id, c.getCredit_id(), c.getCharacter());
                }
            if (episode.crew != null)
                for (CrewJSON c : episode.crew) {
                    epCrew.add(crewKey(c.id, c.name, c.getCredit_id(), c.getJob()));
                    trackJob(c.id, c.getCredit_id(), c.getJob());
                }
        }

        // 2b) Regulaere Season-Credits, flach, in dieselben Mengen
        CreditListJSON reg = api.getRegularSeasonCredits(SERIES_ID, SEASON_NUMBER);
        if (reg.cast != null)
            for (CastJSON c : reg.cast) {
                epCast.add(castKey(c.id, c.name, c.getCredit_id(), c.getCharacter()));
                trackChar(c.id, c.getCredit_id(), c.getCharacter());
            }
        if (reg.crew != null)
            for (CrewJSON c : reg.crew) {
                epCrew.add(crewKey(c.id, c.name, c.getCredit_id(), c.getJob()));
                trackJob(c.id, c.getCredit_id(), c.getJob());
            }

        // 3) Differenzen
        Set<String> castOnlyAgg = minus(aggCast, epCast);
        Set<String> castOnlyEp  = minus(epCast, aggCast);
        Set<String> crewOnlyAgg = minus(aggCrew, epCrew);
        Set<String> crewOnlyEp  = minus(epCrew, aggCrew);

        // 4) credit_id-Eindeutigkeit auswerten
        Set<String> ambiguousJobs = ambiguous(creditIdToJobs);
        Set<String> ambiguousChars = ambiguous(creditIdToChars);

        writeReport(castOnlyAgg, castOnlyEp, crewOnlyAgg, crewOnlyEp,
                ambiguousJobs, ambiguousChars,
                aggCast.size(), aggCrew.size(), epCast.size(), epCrew.size());
    }

    private void trackJob(Integer personId, String creditId, String job) {
        creditIdToJobs.computeIfAbsent(personId + "|" + creditId, k -> new TreeSet<>()).add(String.valueOf(job));
    }

    private void trackChar(Integer personId, String creditId, String character) {
        creditIdToChars.computeIfAbsent(personId + "|" + creditId, k -> new TreeSet<>()).add(String.valueOf(character));
    }

    private Set<String> ambiguous(Map<String, Set<String>> map) {
        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> e : map.entrySet())
            if (e.getValue().size() > 1)
                result.add(e.getKey() + "  ->  " + e.getValue());
        return result;
    }

    private Set<String> minus(Set<String> a, Set<String> b) {
        Set<String> result = new LinkedHashSet<>(a);
        result.removeAll(b);
        return result;
    }

    private String castKey(Integer personId, String name, String creditId, String character) {
        return personId + " | " + name + " | credit=" + creditId + " | char=" + character;
    }

    private String crewKey(Integer personId, String name, String creditId, String job) {
        return personId + " | " + name + " | credit=" + creditId + " | job=" + job;
    }

    private void writeReport(Set<String> castOnlyAgg, Set<String> castOnlyEp,
            Set<String> crewOnlyAgg, Set<String> crewOnlyEp,
            Set<String> ambiguousJobs, Set<String> ambiguousChars,
            int aggCastN, int aggCrewN, int epCastN, int epCrewN) {
        String home = System.getProperty("user.home");
        String path = home + java.io.File.separator + "Desktop"
                + java.io.File.separator + "aggregated_credits_probe.txt";
        try (PrintWriter w = new PrintWriter(new FileWriter(path))) {
            w.println("AggregatedCreditsProbe");
            w.println("seriesId=" + SERIES_ID + ", season=" + SEASON_NUMBER
                    + ", episodes=" + NUMBER_OF_EPISODES);
            w.println();
            w.println("Zaehlung: aggCast=" + aggCastN + ", aggCrew=" + aggCrewN
                    + ", epCast=" + epCastN + ", epCrew=" + epCrewN);
            w.println();

            w.println("########## credit_id-Eindeutigkeit ##########");
            if (ambiguousJobs.isEmpty() && ambiguousChars.isEmpty()) {
                w.println("credit_id scheint eindeutig (keine (person,credit_id) mit mehreren Jobs/Characters).");
            } else {
                w.println("PROBLEM: credit_id ist NICHT eindeutig — schau mal hier:");
                section(w, "Mehrere JOBS pro (person_id|credit_id) (" + ambiguousJobs.size() + ")", ambiguousJobs);
                section(w, "Mehrere CHARACTERS pro (person_id|credit_id) (" + ambiguousChars.size() + ")", ambiguousChars);
            }
            w.println();

            section(w, "A) CAST in aggregated, aber in KEINER Episode (" + castOnlyAgg.size() + ")", castOnlyAgg);
            section(w, "B) CAST in Episode/Regular, aber NICHT in aggregated  [FLIP-CRASH] (" + castOnlyEp.size() + ")", castOnlyEp);
            section(w, "C) CREW in aggregated, aber in KEINER Episode (" + crewOnlyAgg.size() + ")", crewOnlyAgg);
            section(w, "D) CREW in Episode/Regular, aber NICHT in aggregated  [FLIP-CRASH] (" + crewOnlyEp.size() + ")", crewOnlyEp);

            log.info("Report geschrieben: " + path);
        } catch (Exception e) {
            throw new RuntimeException("Report schreiben fehlgeschlagen: " + path, e);
        }
    }

    private void section(PrintWriter w, String title, Set<String> entries) {
        w.println("=== " + title + " ===");
        if (entries.isEmpty())
            w.println("(keine)");
        else
            for (String e : entries)
                w.println("  " + e);
        w.println();
    }
}