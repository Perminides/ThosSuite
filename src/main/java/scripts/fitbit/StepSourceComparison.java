package scripts.fitbit;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.fitbit.ApiClient;
import app.shared.Config;

/**
 * Wegwerf-Messinstrument. Zieht für einen hartcodierten Zeitraum die Tagesschritte
 * aus vier Quellen und gibt sie tab-getrennt in die Konsole, damit sie von Hand
 * gegen die Health-App verglichen werden können:
 *
 * <ul>
 *   <li><b>reconcile</b>   — Health-API, ungekürzter Strom, pro Tag selbst summiert</li>
 *   <li><b>dailyRollUp</b> — Health-API, mit Nicht-getragen-Kürzung, fertig aggregiert</li>
 *   <li><b>fitbitApi</b>   — Fitbit-Web-API, heutiger Summary-Wert (retroaktiv driftend)</li>
 *   <li><b>fitbitLog</b>   — totalSteps zum Import-Zeitpunkt aus fitbit_import.log</li>
 * </ul>
 *
 * <p>Die App-Zahl kommt NICHT aus der API — die trägt Thorsten manuell daneben.</p>
 *
 * <p><b>Kein Produktivcode.</b> Bewusst roh und ohne Abstraktion gehalten, damit die
 * Summierung beim Draufschauen nachvollziehbar ist. Die Tageszuordnung läuft über
 * Googles mitgeliefertes {@code civilStartTime.date}, NICHT über eine eigene Zeitzone —
 * dadurch ist das Bucketing automatisch DST-fest. Ein fester Offset kommt nur grob beim
 * UTC-Abholfenster zum Einsatz (großzügiges Übergreifen an den Rändern).</p>
 *
 * <p>Voraussetzungen für den Lauf: {@code ApiClient}-Konstruktor und das Record
 * {@code ApiClient.ApiResponse} müssen auf {@code public} stehen; {@code Config} muss
 * initialisiert sein (siehe Platzhalter in {@link #main}).</p>
 */
public class StepSourceComparison {

    // ===================== Hartcodiert: vor dem Lauf ausfüllen =====================
    private static final String HEALTH_CLIENT_ID     = "";
    private static final String HEALTH_CLIENT_SECRET = "";
    private static final String HEALTH_REFRESH_TOKEN = "";

    private static final LocalDate FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO   = LocalDate.of(2026, 6, 30);
    // ==============================================================================

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String STEPS_BASE =
            "https://health.googleapis.com/v4/users/me/dataTypes/steps/dataPoints";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient   HTTP   = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
    	
    	Config.init("C:/Users/permi/Documents/Gedächtnis Lernen und so/ThosSuite");

        // TODO: Config so initialisieren, wie die Suite es beim Start tut.
        //       Der genaue Aufruf ist mir nicht bekannt und wird hier NICHT geraten.
        //       Muss stehen, BEVOR ApiClient geladen wird (dessen static-Block liest Config).
        //       Beispiel: Config.init(...);

        String accessToken = fetchHealthAccessToken();

        System.out.println(LocalDateTime.now().toString() + " Wir starten mit Health reconcile (das dauert locker 70 Sekunden pro Monat)");
        Map<LocalDate, Long>    reconcile   = fetchReconcileByDay(accessToken);
        System.out.println(LocalDateTime.now().toString() + " Nun Health dailyRollup");
        Map<LocalDate, Long>    dailyRollUp = fetchDailyRollUpByDay(accessToken);
        System.out.println(LocalDateTime.now().toString() + " Nun das Fitbit Log");
        Map<LocalDate, Integer> fitbitLog   = readFitbitLog();

        ApiClient fitbit = new ApiClient(); // Konstruktor für den Lauf public

        System.out.println("Datum\treconcile\tdailyRollUp\tfitbitApi\tfitbitLog\tapp(manuell)");

        for (LocalDate day = FROM; !day.isAfter(TO); day = day.plusDays(1)) {
            Long    rec    = reconcile.get(day);
            Long    roll   = dailyRollUp.get(day);
            Integer fitApi = fetchFitbitSummarySteps(fitbit, day);
            Integer log    = fitbitLog.get(day);

            System.out.println(
                    day + "\t" + str(rec) + "\t" + str(roll) + "\t"
                        + str(fitApi) + "\t" + str(log) + "\t");
        }
    }

    // --- Health: Access-Token aus dem statischen Refresh-Token holen ---
    private static String fetchHealthAccessToken() throws Exception {
        String body = "client_id="      + enc(HEALTH_CLIENT_ID)
                + "&client_secret="      + enc(HEALTH_CLIENT_SECRET)
                + "&refresh_token="      + enc(HEALTH_REFRESH_TOKEN)
                + "&grant_type=refresh_token";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Token-Refresh fehlgeschlagen (HTTP "
                    + response.statusCode() + "): " + response.body());
        }
        return MAPPER.readTree(response.body()).get("access_token").asText();
    }

    // --- Health reconcile: ungekürzt, paginiert, pro ziviler Tag summiert ---
    private static Map<LocalDate, Long> fetchReconcileByDay(String accessToken) throws Exception {
        // Grob übergreifendes UTC-Fenster (je ein Tag Rand deckt den Offset locker ab).
        String filter = "steps.interval.start_time >= \"" + FROM.minusDays(1) + "T00:00:00Z\""
                + " AND steps.interval.start_time < \"" + TO.plusDays(1) + "T00:00:00Z\"";

        Map<LocalDate, Long> byDay = new HashMap<>();
        String pageToken = null;

        do {
            String url = STEPS_BASE + ":reconcile?filter=" + enc(filter);
            if (pageToken != null) {
                url += "&pageToken=" + enc(pageToken);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("reconcile fehlgeschlagen (HTTP "
                        + response.statusCode() + "): " + response.body());
            }

            JsonNode root = MAPPER.readTree(response.body());
            for (JsonNode point : root.path("dataPoints")) {
                JsonNode steps = point.path("steps");
                long count     = Long.parseLong(steps.path("count").asText());
                LocalDate day  = civilDate(steps.path("interval").path("civilStartTime").path("date"));
                byDay.merge(day, count, Long::sum);
            }

            pageToken = root.path("nextPageToken").asText(null);
        } while (pageToken != null && !pageToken.isBlank());

        return byDay;
    }

    // --- Health dailyRollUp: gekürzt, ein Aufruf, ein Bucket pro Tag ---
    // Ausgelegt auf einen überschaubaren Zeitraum; kein Paginieren.
    private static Map<LocalDate, Long> fetchDailyRollUpByDay(String accessToken) throws Exception {
        String body = """
                {
                  "range": {
                    "start": { "date": { "year": %d, "month": %d, "day": %d }, "time": { "hours": 0, "minutes": 0, "seconds": 0 } },
                    "end":   { "date": { "year": %d, "month": %d, "day": %d }, "time": { "hours": 23, "minutes": 59, "seconds": 59 } }
                  },
                  "windowSizeDays": 1
                }
                """.formatted(
                        FROM.getYear(), FROM.getMonthValue(), FROM.getDayOfMonth(),
                        TO.getYear(),   TO.getMonthValue(),   TO.getDayOfMonth());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STEPS_BASE + ":dailyRollUp"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("dailyRollUp fehlgeschlagen (HTTP "
                    + response.statusCode() + "): " + response.body());
        }

        Map<LocalDate, Long> byDay = new HashMap<>();
        for (JsonNode point : MAPPER.readTree(response.body()).path("rollupDataPoints")) {
            LocalDate day = civilDate(point.path("civilStartTime").path("date"));
            long countSum = Long.parseLong(point.path("steps").path("countSum").asText());
            byDay.put(day, countSum);
        }
        return byDay;
    }

    // --- Fitbit-Web-API: heutiger Summary-Schrittwert über die bestehende Klasse ---
    private static Integer fetchFitbitSummarySteps(ApiClient fitbit, LocalDate day) {
        return fitbit.getActivityDaySummary(day).data().getSummary().getSteps();
    }

    // --- Fitbit-Log: totalSteps zum Import-Zeitpunkt (JSON Lines, eine Zeile pro Tag) ---
    private static Map<LocalDate, Integer> readFitbitLog() throws Exception {
        Path logFile = Config.getPath("logFolder").resolve("fitbit_import.log");
        Map<LocalDate, Integer> byDay = new HashMap<>();
 
        List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            JsonNode node = MAPPER.readTree(line);
            JsonNode date = node.path("date"); // neues Format: [Jahr, Monat, Tag]
 
            // Altes Log-Format (date als String, kein totalSteps oben) still überspringen.
            if (!date.isArray() || !node.has("totalSteps")) {
                continue;
            }
 
            LocalDate day = LocalDate.of(date.get(0).asInt(), date.get(1).asInt(), date.get(2).asInt());
            byDay.put(day, node.path("totalSteps").asInt());
        }
        return byDay;
    }

    // --- Hilfsmittel ---

    private static LocalDate civilDate(JsonNode dateNode) {
        return LocalDate.of(
                dateNode.path("year").asInt(),
                dateNode.path("month").asInt(),
                dateNode.path("day").asInt());
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}