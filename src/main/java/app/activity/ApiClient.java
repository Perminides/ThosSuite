package app.activity;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.activity.model.Exercise;
import app.shared.Config;

/**
 * Kommunikation mit der Google-Health-API für den Aktivitäts-/Fitness-Bereich.
 *
 * <h3>Die Schrittzahl hier ist nicht die Zahl auf der Uhr</h3>
 *
 * <p>Die API liefert systematisch weniger Schritte, als die Uhr anzeigt, und die Differenz
 * schwankt von Tag zu Tag. Das ist kein Fehler in dieser Klasse und lässt sich von hier aus
 * auch nicht beheben — <b>bitte nicht erneut danach suchen.</b></p>
 *
 * <p>Die Ursache ist eine Filterung: Die Consumer-Oberflächen zählen Schritte mit, die das Gerät
 * als "off-wrist" erkannt hat (in der Tasche, in der Hand, Vibration von außen); die API-Pipeline
 * verlangt dagegen per Default "on-wrist"-Gültigkeit und wirft genau diese weg. Weil der Anteil
 * off-wrist täglich schwankt, schwankt auch die Differenz. Google beziffert sie mit 0,3 % bis
 * 15 %, hier gemessen wurden 0,1 % bis 1,8 %.</p>
 *
 * <p><b>Alle Lesemethoden liefern dieselbe gefilterte Zahl</b> — {@code list},
 * {@code dailyRollUp} und {@code reconcile} greifen auf denselben rekonziliierten Speicher zu,
 * in dem der Filter bereits angewendet ist. Die Methode zu wechseln bringt deshalb nichts; das
 * ist gemessen. Ein Schalter, der die off-wrist-Schritte einschließt, existiert nicht. Google
 * hat einen angekündigt, aber ohne Termin.</p>
 *
 * <p><b>Wonach sich die Suite richtet:</b> nach der Health-App. Deren Anzeige und diese API
 * stimmen seit dem 03.09.2026 überein. Die Uhr weicht weiterhin ab — und weil App und Uhr
 * selbst auseinanderliegen, ist "mit beidem synchron" gar nicht erreichbar. Die App zu treffen
 * ist das Beste, was von hier aus geht.</p>
 *
 * <h3>Methodenwahl</h3>
 *
 * <p><b>Schritte über {@code dailyRollUp}:</b> Google zieht die Tagesgrenze selbst, zeitzonen-
 * und DST-fest, und näht den lokalen Tag über Zeitzonensprünge zusammen — im Urlaub also ohne
 * eigenes Zutun korrekt. Keine eigene Grenzberechnung, keine Paginierung, und an sauberen Tagen
 * ohnehin derselbe Wert wie {@code reconcile}.</p>
 *
 * <p><b>Aktivitäten über {@code list}:</b> die einzige Methode, die {@code exerciseType} und
 * Distanz je Aktivität einzeln liefert — {@code reconcile} fasst zusammen. Der Preis ist, dass
 * {@code list} pro Quelle liest und nicht dedupliziert: Zeichnen zwei Geräte dieselbe Aktivität
 * gleichzeitig auf, steht sie zweimal da. Das setzt voraus, dass nie zwei Geräte parallel
 * schreiben.</p>
 *
 * <h3>OAuth</h3>
 *
 * <p>Statisches Modell: Das Refresh-Token ist dauerhaft und rotiert NICHT. Der Client refresht
 * beim Erzeugen einmal ein Access-Token; das genügt, weil der Import einmal täglich läuft. Es
 * wird nichts persistiert.</p>
 *
 * <p>Credentials liegen statisch in der Config: {@code healthClientId},
 * {@code healthClientSecret}, {@code healthRefreshToken}. Neu erteilt wird die Einwilligung mit
 * {@code scripts.fitbit.GoogleHealthConsent}.</p>
 */
public class ApiClient {

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String STEPS_BASE =
            "https://health.googleapis.com/v4/users/me/dataTypes/steps/dataPoints";
    private static final String EXERCISE_BASE =
            "https://health.googleapis.com/v4/users/me/dataTypes/exercise/dataPoints";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private final String accessToken;

    public ApiClient() {
        this.accessToken = refresh();
    }

    /**
     * Tagesschritte je lokalem Kalendertag über {@code dailyRollUp} (Google zieht die
     * Tagesgrenze selbst, zeitzonen- und DST-fest). Ein Tag, der FEHLT, bedeutet
     * "nicht getragen / nicht synchronisiert" — er taucht schlicht nicht in der Map auf
     * und darf NICHT als 0 behandelt werden.
     */
    public Map<LocalDate, Integer> fetchDailySteps(LocalDate from, LocalDate to) {
        String body = """
                {
                  "range": {
                    "start": { "date": { "year": %d, "month": %d, "day": %d }, "time": { "hours": 0, "minutes": 0, "seconds": 0 } },
                    "end":   { "date": { "year": %d, "month": %d, "day": %d }, "time": { "hours": 23, "minutes": 59, "seconds": 59 } }
                  },
                  "windowSizeDays": 1
                }
                """.formatted(
                        from.getYear(), from.getMonthValue(), from.getDayOfMonth(),
                        to.getYear(),   to.getMonthValue(),   to.getDayOfMonth());

        JsonNode root = parse(postJson(STEPS_BASE + ":dailyRollUp", body));

        Map<LocalDate, Integer> byDay = new HashMap<>();
        for (JsonNode point : root.path("rollupDataPoints")) {
            LocalDate day = civilDate(point.path("civilStartTime").path("date"));
            int countSum = Integer.parseInt(point.path("steps").path("countSum").asText());
            byDay.put(day, countSum);
        }
        return byDay;
    }

    /**
     * Alle aufgezeichneten Aktivitäten im lokalen Kalenderbereich [from, to] über die
     * paginierte {@code exercise}-{@code list}. Der Filter läuft über die zivile (lokale)
     * Startzeit, das erspart eigenes UTC-Rechnen beim Abholen.
     */
    public List<Exercise> fetchActivities(LocalDate from, LocalDate to) {
        String filter = "exercise.interval.civil_start_time >= \"" + from + "T00:00:00\""
                + " AND exercise.interval.civil_start_time < \"" + to.plusDays(1) + "T00:00:00\"";

        List<Exercise> activities = new ArrayList<>();
        String pageToken = null;
        do {
            String url = EXERCISE_BASE + "?filter=" + enc(filter) + "&page_size=1000";
            if (pageToken != null) {
                url += "&pageToken=" + enc(pageToken);
            }

            JsonNode root = parse(get(url));
            for (JsonNode point : root.path("dataPoints")) {
                activities.add(toExercise(point.path("exercise")));
            }
            pageToken = root.path("nextPageToken").asText(null);
        } while (pageToken != null && !pageToken.isBlank());

        return activities;
    }

    private static Exercise toExercise(JsonNode ex) {
        String type = ex.path("exerciseType").asText(null);
        if (type == null) {
            throw new RuntimeException("exercise ohne exerciseType: " + ex);
        }

        JsonNode metrics = ex.path("metricsSummary");
        Long distanceMm = metrics.has("distanceMillimeters") ? metrics.get("distanceMillimeters").asLong() : null;
        Integer steps   = metrics.has("steps") ? metrics.get("steps").asInt() : null;

        JsonNode interval = ex.path("interval");
        Instant startTime = Instant.parse(interval.path("startTime").asText());
        int offsetSeconds = parseOffsetSeconds(interval.path("startUtcOffset").asText());

        return new Exercise(type, distanceMm, steps, startTime, offsetSeconds);
    }

    private static String refresh() {
        String body = "client_id="  + enc(Config.get("healthClientId"))
                + "&client_secret=" + enc(Config.get("healthClientSecret"))
                + "&refresh_token=" + enc(Config.get("healthRefreshToken"))
                + "&grant_type=refresh_token";

        JsonNode root = parse(postForm(TOKEN_ENDPOINT, body));
        return root.get("access_token").asText();
    }

    // --- HTTP ---

    private String get(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        return send(request);
    }

    private String postJson(String url, String jsonBody) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return send(request);
    }

    private static String postForm(String url, String formBody) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
        return send(request);
    }

    private static String send(HttpRequest request) {
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Health-API Fehler (HTTP " + response.statusCode()
                        + "): " + response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Fehler bei der Kommunikation mit der Health-API", e);
        }
    }

    // --- Hilfsmittel ---

    private static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Parsen der Health-Antwort", e);
        }
    }

    private static LocalDate civilDate(JsonNode dateNode) {
        return LocalDate.of(
                dateNode.path("year").asInt(),
                dateNode.path("month").asInt(),
                dateNode.path("day").asInt());
    }

    /** "3600s" -> 3600. */
    private static int parseOffsetSeconds(String offset) {
        return Integer.parseInt(offset.replace("s", ""));
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}