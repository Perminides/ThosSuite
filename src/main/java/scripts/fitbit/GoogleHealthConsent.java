package scripts.fitbit;

import java.awt.Desktop;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

/**
 * Einmaliges Einwilligungs-Werkzeug für die Google Health API.
 *
 * <p>Holt per OAuth2-Authorization-Code-Flow (mit PKCE) das erste Token-Paar für den
 * Google-Health-Zugriff und gibt das Refresh-Token auf der Konsole aus. Das Refresh-Token
 * wird danach von der Suite benutzt, um still Access-Tokens nachzuladen.</p>
 *
 * <p>Standalone, keine Kopplung an Suite-Klassen. Nutzt nur das JDK (Loopback-Empfänger,
 * PKCE, HTTP) plus Jackson für das Parsen der einen Token-Antwort.</p>
 *
 * <p><b>Kein Wegwerf-Skript.</b> Läuft im Regelfall einmal und dann jahrelang nicht mehr,
 * muss aber als Reaktivierungs-Werkzeug erhalten bleiben.</p>
 *
 * <p>Ablauf: PKCE + State erzeugen → Loopback-Empfänger auf freiem Port starten →
 * Browser mit Auth-URL öffnen → Rücksprung mit Authorization Code abfangen →
 * Code gegen Tokens tauschen → Refresh-Token ausgeben.</p>
 */
public class GoogleHealthConsent {

    // --- Auszufüllen: Zugangsdaten des Desktop-OAuth-Clients aus dem Projekt thos-suite ---
    private static final String CLIENT_ID     = "";
    private static final String CLIENT_SECRET = "";

    private static final String SCOPE =
            "https://www.googleapis.com/auth/googlehealth.activity_and_fitness.readonly";

    private static final String AUTH_ENDPOINT  = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient   HTTP   = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {

        // 1. PKCE + State erzeugen
        String verifier  = randomUrlSafe(32);
        String challenge = s256Challenge(verifier);
        String state     = randomUrlSafe(16);

        // 2. Loopback-Empfänger auf freiem Port starten
        var received = new ArrayBlockingQueue<Map<String, String>>(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int    port        = server.getAddress().getPort();
        String redirectUri = "http://127.0.0.1:" + port;

        server.createContext("/", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            // Nur der echte Rücksprung trägt code oder error; alles andere (z.B. favicon) ignorieren.
            if (!params.containsKey("code") && !params.containsKey("error")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            byte[] page = "Einwilligung erhalten. Dieses Fenster kann geschlossen werden."
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, page.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(page);
            }
            received.add(params);
        });
        server.start();

        // 3. Auth-URL bauen und Browser öffnen
        String authUrl = AUTH_ENDPOINT
                + "?client_id="            + enc(CLIENT_ID)
                + "&redirect_uri="         + enc(redirectUri)
                + "&response_type=code"
                + "&scope="                + enc(SCOPE)
                + "&code_challenge="       + enc(challenge)
                + "&code_challenge_method=S256"
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state="                + enc(state);

        openBrowser(authUrl);

        // 4. Auf den Rücksprung warten
        Map<String, String> callback = received.take();
        server.stop(0);

        if (callback.containsKey("error")) {
            throw new IllegalStateException("Einwilligung fehlgeschlagen: " + callback.get("error"));
        }
        if (!state.equals(callback.get("state"))) {
            throw new IllegalStateException("State stimmt nicht überein - Abbruch.");
        }

        // 5. Authorization Code gegen Tokens tauschen
        String form = "client_id="        + enc(CLIENT_ID)
                + "&client_secret="        + enc(CLIENT_SECRET)
                + "&code="                 + enc(callback.get("code"))
                + "&code_verifier="        + enc(verifier)
                + "&grant_type=authorization_code"
                + "&redirect_uri="         + enc(redirectUri);

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = HTTP.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Token-Tausch fehlgeschlagen (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonNode json = MAPPER.readTree(response.body());
        if (!json.has("refresh_token")) {
            throw new IllegalStateException(
                    "Antwort enthält kein refresh_token. access_type/prompt prüfen. Antwort: " + response.body());
        }

        // 6. Ausgeben
        String refreshToken = json.get("refresh_token").asText();
        String accessToken  = json.get("access_token").asText();
        long   expiresIn    = json.get("expires_in").asLong();

        System.out.println("""

                ============================================================
                Einwilligung erfolgreich.

                REFRESH_TOKEN (dauerhaft, wie ein Passwort behandeln):
                %s

                Access-Token (nur zur Sofortkontrolle, läuft in %d s ab):
                %s

                Nächster Schritt:
                Dieses Refresh-Token dort ablegen, wo die Suite es beim
                Start erwartet (Ablageort noch festzulegen).
                ============================================================
                """.formatted(refreshToken, expiresIn, accessToken));
    }

    /** Zufälliger Base64-URL-String ohne Padding (für code_verifier und state). */
    private static String randomUrlSafe(int numBytes) {
        byte[] bytes = new byte[numBytes];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 des Verifiers, Base64-URL ohne Padding (PKCE S256). */
    private static String s256Challenge(String verifier) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    /** Query-String in ein Key/Value-Map zerlegen, Werte URL-dekodiert. */
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                result.put(key, val);
            }
        }
        return result;
    }

    /** Systembrowser öffnen; falls nicht unterstützt, URL zum manuellen Öffnen ausgeben. */
    private static void openBrowser(String url) throws Exception {
        //if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        //    Desktop.getDesktop().browse(URI.create(url));
        //} else {
            System.out.println("Bitte diese URL manuell im Browser öffnen:\n" + url);
        //}
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}