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
 * <p>Statisches OAuth2-Modell (anders als Fitbit): Das Refresh-Token ist dauerhaft und
 * rotiert NICHT. Der Client refresht beim Erzeugen einmal ein Access-Token; das genügt,
 * weil der Import einmal täglich läuft. Es wird nichts persistiert.</p>
 *
 * <p>Credentials liegen statisch in der Config: {@code healthClientId},
 * {@code healthClientSecret}, {@code healthRefreshToken}.</p>
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

        JsonNode root = MAPPER_readTree(postJson(STEPS_BASE + ":dailyRollUp", body));

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

            JsonNode root = MAPPER_readTree(get(url));
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

        JsonNode root = MAPPER_readTree(postForm(TOKEN_ENDPOINT, body));
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

    private static JsonNode MAPPER_readTree(String json) {
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