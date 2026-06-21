package app.fitbit;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.fitbit.model.json.Activity;
import app.fitbit.model.json.ActivityDaySummary;
import app.fitbit.model.json.ActivityLogList;
import app.fitbit.model.json.FitbitCredentials;
import app.shared.Config;
import app.shared.Log;

/**
 * Verantwortlich für die Kommunikation mit der Fitbit-API und das Token-Management.
 * 
 * <h2>OAuth2-Token-System (Wichtig zum Verständnis!):</h2>
 * 
 * <p><b>fitbitClientPlusSecret</b> (aus Config):</p>
 * <ul>
 *   <li>Das ist die "App-Identität" der ThosSuite bei Fitbit</li>
 *   <li>Base64-kodiert: "ClientID:ClientSecret"</li>
 *   <li>Wird bei JEDEM Token-Refresh benötigt</li>
 *   <li>Bleibt IMMER gleich (Master-Key)</li>
 *   <li>Registriert auf: https://dev.fitbit.com/apps/details/23BHGZ</li>
 * </ul>
 * 
 * <p><b>access_token</b> (in fitbit_credentials.json):</p>
 * <ul>
 *   <li>Das "Zugriffsticket" für die API-Calls</li>
 *   <li>Läuft nach ~8 Stunden ab (expires_in)</li>
 *   <li>Wird bei JEDEM API-Request im Authorization-Header mitgeschickt</li>
 * </ul>
 * 
 * <p><b>refresh_token</b> (in fitbit_credentials.json):</p>
 * <ul>
 *   <li>Wird benutzt, um einen NEUEN access_token zu holen</li>
 *   <li>Erspart erneutes Login bei Fitbit</li>
 *   <li>Wird selbst auch erneuert bei jedem Refresh</li>
 * </ul>
 * 
 * <h3>Workflow:</h3>
 * <ol>
 *   <li><b>Einmalig (Initialize.java):</b> User loggt sich bei Fitbit ein
 *       → Erster access_token + refresh_token werden in fitbit_credentials.json gespeichert</li>
 *   <li><b>Täglich (beim Import):</b> access_token wird benutzt um Daten zu holen</li>
 *   <li><b>Nach 8h (automatisch):</b> access_token ist abgelaufen
 *       → refresh_token + fitbitClientPlusSecret werden benutzt um neuen access_token zu holen
 *       → fitbit_credentials.json wird aktualisiert</li>
 * </ol>
 */
public class ApiClient {
    
    public static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    
    // OAuth2 Authorization für Token-Refresh (bleibt immer gleich)
    private static final String CLIENT_AUTHORIZATION;
    
    // Pfad zur Credentials-Datei
    private static final Path CREDENTIALS_FILE_PATH;
    
    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        CLIENT_AUTHORIZATION = "Basic " + Config.get("fitbitClientPlusSecret");
        CREDENTIALS_FILE_PATH = Config.getPath("fitbitFolder").resolve("fitbit_credentials.json");
    }
    
    private FitbitCredentials credentials;
    
    /**
     * Konstruktor lädt Credentials und prüft Token-Gültigkeit.
     * Refresht Token automatisch bei Bedarf.
     * 
     * @throws RuntimeException wenn Credentials nicht geladen werden können
     *         oder Token-Refresh fehlschlägt
     */
    ApiClient() {
        loadCredentials();
        refreshTokenIfNeeded();
    }
    
    /**
     * Holt die Activity-Zusammenfassung für einen bestimmten Tag.
     * Enthält: Schritte, Distanzen, Kalorien, etc.
     * 
     * @param date Das Datum (YYYY-MM-DD)
     * @return Tupel aus (Parsed Object, Original JSON String)
     * @throws RuntimeException bei API-Fehlern
     */
    public ApiResponse<ActivityDaySummary> getActivityDaySummary(LocalDate date) {
        String url = "https://api.fitbit.com/1/user/-/activities/date/" + date + ".json";
        String json = executeApiRequest(url);
        
        try {
            ActivityDaySummary parsed = MAPPER.readValue(json, ActivityDaySummary.class);
            return new ApiResponse<>(parsed, json);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Parsen der Activity-Zusammenfassung für " + date, e);
        }
    }
    
    /**
     * Holt alle Activities eines bestimmten Tages.
     * Filtert die API-Antwort so, dass nur Activities vom angegebenen Datum zurückkommen.
     * 
     * @param date Das Datum (YYYY-MM-DD)
     * @return Tupel aus (Gefilterte Activities, Original JSON String)
     * @throws RuntimeException bei API-Fehlern
     */
    public ApiResponse<ActivityLogList> getActivitiesLogList(LocalDate date) {
        String url = "https://api.fitbit.com/1/user/-/activities/list.json?afterDate=" + date + "&sort=asc&offset=0&limit=100";
        String json = executeApiRequest(url);
        
        try {
            ActivityLogList fullResponse = MAPPER.readValue(json, ActivityLogList.class);
            
            // Filtern: Nur Activities vom gewünschten Datum
            List<Activity> activitiesForDate = new ArrayList<>();
            for (Activity activity : fullResponse.getActivities()) {
                if (activity.getDate().equals(date)) {
                    activitiesForDate.add(activity);
                }
            }
            
            ActivityLogList filteredResponse = new ActivityLogList();
            filteredResponse.setActivities(activitiesForDate);
            
            return new ApiResponse<>(filteredResponse, json);
            
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Parsen der Activity-Liste für " + date, e);
        }
    }
    
    /**
     * Führt einen GET-Request zur Fitbit-API aus.
     * Benutzt den aktuellen access_token aus den Credentials.
     * 
     * @param url Die vollständige API-URL
     * @return Die JSON-Antwort als String
     * @throws RuntimeException bei Netzwerkfehlern oder API-Errors
     */
    private String executeApiRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + credentials.access_token())
                .header("Accept", "application/json")
                .GET()
                .build();
        
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException(
                    "Fitbit-API Fehler (HTTP " + response.statusCode() + "): " + response.body()
                );
            }
            
            Log.debug(this, "API-Response: " + response.body());
            return response.body();
            
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Fehler bei der Kommunikation mit Fitbit-API: " + url, e);
        }
    }
    
    /**
     * Lädt die Credentials aus fitbit_credentials.json.
     * Wirft Exception wenn Datei nicht existiert oder ungültig ist.
     * 
     * @throws RuntimeException wenn Credentials nicht geladen werden können
     */
    private void loadCredentials() {
        File credentialsFile = CREDENTIALS_FILE_PATH.toFile();
        
        if (!credentialsFile.exists()) {
            throw new RuntimeException(
                "Fitbit-Credentials nicht gefunden: " + CREDENTIALS_FILE_PATH + "\n" +
                "Bitte Initialize.java ausführen, um die initiale Autorisierung durchzuführen."
            );
        }
        
        try {
            credentials = MAPPER.readValue(credentialsFile, FitbitCredentials.class);
            Log.info(this, "Fitbit-Credentials geladen. Token erstellt: " + credentials.created());
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Lesen der Fitbit-Credentials", e);
        }
    }
    
    /**
     * Prüft ob der access_token noch gültig ist.
     * Wenn nicht: Automatischer Refresh mit dem refresh_token.
     * 
     * Token gilt als abgelaufen, wenn:
     * created + expires_in < jetzt - 10 Minuten (Sicherheitspuffer)
     * 
     * @throws RuntimeException wenn Token-Refresh fehlschlägt
     */
    private void refreshTokenIfNeeded() {
        LocalDateTime expiresAt = credentials.created().plusSeconds(Long.parseLong(credentials.expires_in()));
        LocalDateTime now = LocalDateTime.now();
        
        // 10 Minuten Puffer vor Ablauf
        if (expiresAt.isBefore(now.minusMinutes(10))) {
            Log.info(this, "Access-Token abgelaufen. Führe Refresh durch...");
            refreshToken();
        } else {
            Log.debug(this, "Access-Token noch gültig bis: " + expiresAt);
        }
    }
    
    /**
     * Erneuert den access_token mittels refresh_token.
     * 
     * Fitbit-API: POST /oauth2/token
     * Body: grant_type=refresh_token&refresh_token={refresh_token}
     * Authorization: Basic {fitbitClientPlusSecret}
     * 
     * @throws RuntimeException wenn Refresh fehlschlägt
     */
    private void refreshToken() {
        String requestBody = "grant_type=refresh_token&refresh_token=" + credentials.refresh_token();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.fitbit.com/oauth2/token"))
                .header("Authorization", CLIENT_AUTHORIZATION)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException(
                    "Token-Refresh fehlgeschlagen (HTTP " + response.statusCode() + "): " + response.body() + "\n" +
                    "Möglicherweise ist die Autorisierung abgelaufen. Bitte Initialize.java erneut ausführen."
                );
            }
            
            // Neue Credentials aus Response parsen
            FitbitCredentials newCredentials = MAPPER.readValue(response.body(), FitbitCredentials.class);
            
            // Created-Zeitstempel setzen (kommt nicht von API)
            credentials = new FitbitCredentials(
                newCredentials.access_token(),
                newCredentials.refresh_token(),
                newCredentials.expires_in(),
                LocalDateTime.now()
            );
            
            // In Datei speichern
            saveCredentials();
            
            Log.info(this, "Token erfolgreich erneuert. Gültig bis: " + 
                     credentials.created().plusSeconds(Long.parseLong(credentials.expires_in())));
            
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Fehler beim Token-Refresh", e);
        }
    }
    
    /**
     * Speichert die aktuellen Credentials in fitbit_credentials.json.
     * 
     * @throws RuntimeException bei Schreibfehlern
     */
    private void saveCredentials() {
        try {
            MAPPER.writerWithDefaultPrettyPrinter()
                  .writeValue(CREDENTIALS_FILE_PATH.toFile(), credentials);
            Log.debug(this, "Credentials gespeichert: " + CREDENTIALS_FILE_PATH);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Speichern der Credentials", e);
        }
    }
    
    /**
     * Container für API-Response mit parsed Object und original JSON.
     * 
     * @param <T> Der Typ des geparsten Objekts
     * @param data Das geparste Objekt
     * @param originalJson Der originale JSON-String von der API
     */
    record ApiResponse<T>(T data, String originalJson) {}
}