package scripts.fitbit;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Standalone-Messinstrument. Summiert die ROHEN Schrittwerte der Health-API
 * (Methode {@code list}, ohne Rekonziliation und ohne Nicht-getragen-Kürzung)
 * pro lokalem Kalendertag und gibt sie tab-getrennt in die Konsole.
 *
 * <p>Zweck: prüfen, ob der rohe {@code list}-Strom die App-/Uhr-Zahl trifft,
 * die {@code reconcile} und {@code dailyRollUp} um ~1 % unterschreiten.</p>
 *
 * <p>Hängt an keiner Suite-Klasse: eigene Credentials, kein Config, kein ApiClient.
 * Die Tageszuordnung läuft über Googles {@code civilStartTime.date}, der Filter
 * über {@code civil_start_time} (lokale Zeit, kein UTC-Umrechnen nötig).</p>
 *
 * <p><b>Achtung bei der Deutung:</b> {@code list} liefert alle Quellen UNGEMERGED.
 * An Ein-Quellen-Tagen (nur Tracker) ist die Summe direkt vergleichbar. An
 * Mehrquellen-Tagen (Handy + Tracker) würde stumpfes Summieren doppelt zählen —
 * die Quellen-Spalte macht das sichtbar.</p>
 */
public class StepsListSum {

    // ===================== Hartcodiert: vor dem Lauf ausfüllen =====================
	private static final String CLIENT_ID     = "";
    private static final String CLIENT_SECRET = "";
    private static final String REFRESH_TOKEN = "";

    private static final LocalDate FROM = LocalDate.of(2026, 6, 30);
    private static final LocalDate TO   = LocalDate.of(2026, 6, 30);
    // ==============================================================================

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String STEPS_BASE =
            "https://health.googleapis.com/v4/users/me/dataTypes/steps/dataPoints";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient   HTTP   = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {

        String accessToken = fetchAccessToken();

        // Lokaler Kalender-Filter (kein Z, kein Offset -> zivile Zeit).
        String filter = "steps.interval.civil_start_time >= \"" + FROM + "T00:00:00\""
                + " AND steps.interval.civil_start_time < \"" + TO.plusDays(1) + "T00:00:00\"";

        Map<LocalDate, Long>        sums    = new TreeMap<>();
        Map<LocalDate, Set<String>> sources = new HashMap<>();

        String pageToken = null;
        do {
            String url = STEPS_BASE + "?filter=" + enc(filter) + "&page_size=1000";
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
                throw new RuntimeException("list fehlgeschlagen (HTTP "
                        + response.statusCode() + "): " + response.body());
            }

            JsonNode root = MAPPER.readTree(response.body());
            for (JsonNode point : root.path("dataPoints")) {
                JsonNode steps = point.path("steps");
                long count     = Long.parseLong(steps.path("count").asText());
                LocalDate day  = civilDate(steps.path("interval").path("civilStartTime").path("date"));

                sums.merge(day, count, Long::sum);
                sources.computeIfAbsent(day, k -> new TreeSet<>())
                       .add(sourceLabel(point.path("dataSource")));
            }

            pageToken = root.path("nextPageToken").asText(null);
        } while (pageToken != null && !pageToken.isBlank());

        System.out.println("Datum\tlistSum\tQuellen");
        for (LocalDate day = FROM; !day.isAfter(TO); day = day.plusDays(1)) {
            Long sum = sums.get(day);
            Set<String> src = sources.getOrDefault(day, new TreeSet<>());
            System.out.println(day + "\t" + (sum == null ? "" : sum) + "\t" + String.join(" | ", src));
        }
    }

    private static String fetchAccessToken() throws Exception {
        String body = "client_id="  + enc(CLIENT_ID)
                + "&client_secret=" + enc(CLIENT_SECRET)
                + "&refresh_token=" + enc(REFRESH_TOKEN)
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

    private static LocalDate civilDate(JsonNode dateNode) {
        return LocalDate.of(
                dateNode.path("year").asInt(),
                dateNode.path("month").asInt(),
                dateNode.path("day").asInt());
    }

    private static String sourceLabel(JsonNode dataSource) {
        String platform  = dataSource.path("platform").asText("");
        String device    = dataSource.path("device").path("displayName").asText("");
        String recording = dataSource.path("recordingMethod").asText("");
        return platform + "/" + device + "/" + recording;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}