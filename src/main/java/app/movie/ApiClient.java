package app.movie;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.movie.model.json.CreditListJSON;
import app.movie.model.json.EpisodeJSON;
import app.movie.model.json.EpisodeRatingsPageJSON;
import app.movie.model.json.MovieJSON;
import app.movie.model.json.MovieRatingsPageJSON;
import app.movie.model.json.PersonJSON;
import app.movie.model.json.SeasonJSON;
import app.movie.model.json.TvShowJSON;
import app.movie.model.json.TvShowRatingsPageJSON;
import app.shared.Config;
import app.shared.Log;

/**
 * Kapselt die gesamte HTTP-Kommunikation mit der TMDB-API.
 *
 * Zwei private Transport-Methoden ({@link #getV3(String, Map)} und
 * {@link #getV4(String, Map)}) übernehmen Authentifizierung und Request-Bau.
 * Alle fachlichen Methoden bauen auf diesen auf und wissen nichts von HTTP.
 *
 * Authentifizierung:
 * - v3: API-Key und Session-ID als URL-Parameter
 * - v4: Bearer-Token im Authorization-Header
 *
 * Alle Methoden werfen eine {@link RuntimeException} bei Netzwerkfehlern oder
 * nicht-parsebarem JSON. Ein Fehler hier ist immer fatal — wir haben keinen
 * sinnvollen Fallback wenn die TMDB-API nicht erreichbar ist.
 */
public class ApiClient {


    private static final String BASE_URL_V3 = "https://api.themoviedb.org/3/";
    private static final String BASE_URL_V4 = "https://api.themoviedb.org/4/";
    private static final String LANG_EN = "en-US";
    private static final String LANG_DE = "de";

    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Liefert eine Seite bewerteter Filme vom TMDB-Account, absteigend nach
     * Bewertungsdatum sortiert.
     *
     * @param page  Seitennummer, beginnt bei 1
     * @return      Die gemappte Seite mit Bewertungen und Paginierungsinformationen
     */
    public MovieRatingsPageJSON getRatedMovies(int page) {
        String call = "getRatedMovies, page " + page;
        Log.info(ApiClient.class, "TMDB " + call);
        String path = "account/" + Config.get("tmdb.v4.accountId") + "/movie/rated";
        Map<String, String> params = Map.of("sort_by", "created_at.desc", "page", String.valueOf(page));

        return parse(getV4(path, params), MovieRatingsPageJSON.class, call);
    }
    
    /**
     * Liefert eine Seite bewerteter Serien vom TMDB-Account, absteigend nach
     * Bewertungsdatum sortiert.
     *
     * @param page  Seitennummer, beginnt bei 1
     * @return      Die gemappte Seite mit Bewertungen und Paginierungsinformationen
     */
    public TvShowRatingsPageJSON getRatedTvShows(int page) {
        String call = "getRatedTvShows, page " + page;
        Log.info(ApiClient.class, "TMDB " + call);
        String path = "account/" + Config.get("tmdb.v4.accountId") + "/tv/rated";
        Map<String, String> params = Map.of("sort_by", "created_at.desc", "page", String.valueOf(page));

        return parse(getV4(path, params), TvShowRatingsPageJSON.class, call);
    }
    
    /**
     * Liefert eine Seite bewerteter Episoden vom TMDB-Account, absteigend nach
     * Bewertungsdatum sortiert.
     * Nutzt v3, da dieser Endpunkt in v4 noch nicht verfügbar ist.
     *
     * @param page  Seitennummer, beginnt bei 1
     * @return      Die gemappte Seite mit Bewertungen und Paginierungsinformationen
     */
    public EpisodeRatingsPageJSON getRatedEpisodes(int page) {
        String call = "getRatedEpisodes, page " + page;
        Log.info(ApiClient.class, "TMDB " + call);
        String path = "account/" + Config.get("tmdb.v3.accountId") + "/rated/tv/episodes";
        Map<String, String> params = Map.of("sort_by", "created_at.desc", "page", String.valueOf(page));

        return parse(getV3(path, params), EpisodeRatingsPageJSON.class, call);
    }
    
    /**
     * Liefert die vollständigen Detaildaten eines Films in Englisch und Deutsch.
     * Dazu werden zwei Requests gemacht — einer für en-US, einer für de —
     * und der deutsche Titel wird in das englische Objekt übernommen.
     *
     * @param movieId   TMDB-ID des Films
     * @return          Vollständiges MovieJSON mit german_title befüllt
     */
    public MovieJSON getMovieDetails(int movieId) {
        String call = "getMovieDetails, movieId " + movieId;
        Log.info(ApiClient.class, "TMDB " + call);
        String path = "movie/" + movieId;

        MovieJSON movieEN = parse(getV3(path, Map.of("language", LANG_EN)), MovieJSON.class, call + " EN");
        MovieJSON movieDE = parse(getV3(path, Map.of("language", LANG_DE)), MovieJSON.class, call + " DE");
        movieEN.german_title = movieDE.title;

        return movieEN;
    }
    
    /**
     * Liefert die vollständigen Detaildaten einer Serie in Englisch und Deutsch.
     * Dazu werden zwei Requests gemacht — einer für en-US, einer für de —
     * und der deutsche Name wird in das englische Objekt übernommen.
     *
     * @param tvShowId  TMDB-ID der Serie
     * @return          Vollständiges TvShowJSON mit german_name befüllt
     */
    public TvShowJSON getTvShowDetails(int tvShowId) {
        String call = "getTvShowDetails, tvShowId " + tvShowId;
        Log.info(ApiClient.class, "TMDB " + call);

        TvShowJSON tvShowEN = getTvShowDetailsEnOnly(tvShowId);
        TvShowJSON tvShowDE = parse(getV3("tv/" + tvShowId, Map.of("language", LANG_DE)),
                TvShowJSON.class, call + " DE");
        tvShowEN.german_name = tvShowDE.name;

        return tvShowEN;
    }

    /**
     * Wie {@link #getTvShowDetails}, aber nur der englische Request — der deutsche Name bleibt leer.
     *
     * <p>Für alles, was nur sprachunabhängige Felder braucht: der Daten-Check vergleicht
     * Staffel- und Episodenzahl, Status und {@code last_air_date}, der Lückencheck will
     * {@code overview} und {@code poster_path} — alle vier bzw. beide stehen schon in der englischen
     * Antwort. Das halbiert die Requests dieser Läufe.</p>
     *
     * @param tvShowId  TMDB-ID der Serie
     * @return          TvShowJSON ohne {@code german_name}
     */
    public TvShowJSON getTvShowDetailsEnOnly(int tvShowId) {
        String call = "getTvShowDetailsEnOnly, tvShowId " + tvShowId;
        Log.info(ApiClient.class, "TMDB " + call);

        return parse(getV3("tv/" + tvShowId, Map.of("language", LANG_EN)), TvShowJSON.class, call);
    }
    
    /**
     * Liefert die vollständigen Detaildaten einer Staffel in Englisch und Deutsch.
     * Dazu werden zwei Requests gemacht — einer für en-US, einer für de —
     * und der deutsche Name wird in das englische Objekt übernommen.
     * 
     * Das last_air_date wird aus den Episoden berechnet, da die API dieses
     * nicht direkt liefert.
     *
     * @param tvShowId      TMDB-ID der Serie
     * @param seasonNumber  Staffelnummer
     * @return              Vollständiges SeasonJSON mit german_name und last_air_date befüllt
     */
    public SeasonJSON getSeasonDetails(int tvShowId, int seasonNumber) {
        String call = "getSeasonDetails, tvShowId " + tvShowId + ", seasonNumber " + seasonNumber;
        Log.info(ApiClient.class, "TMDB " + call);
        String path = "tv/" + tvShowId + "/season/" + seasonNumber;

        SeasonJSON seasonEN = parse(getV3(path, Map.of("language", LANG_EN)), SeasonJSON.class, call + " EN");
        seasonEN.tvShowID = tvShowId;
        seasonEN.season_number = seasonNumber;

        // last_air_date aus Episoden berechnen, da die API dieses nicht direkt liefert
        if (seasonEN.air_date != null && seasonEN.episodes != null) {
            seasonEN.last_air_date = seasonEN.air_date;
            for (EpisodeJSON episode : seasonEN.episodes)
                if (episode.air_date != null && episode.air_date.isAfter(seasonEN.last_air_date))
                    seasonEN.last_air_date = episode.air_date;
        }

        SeasonJSON seasonDE = parse(getV3(path, Map.of("language", LANG_DE)), SeasonJSON.class, call + " DE");
        seasonEN.germanName = seasonDE.name;

        return seasonEN;
    }
    
    /**
     * Liefert die vollständigen Detaildaten einer Episode in Englisch und Deutsch.
     * Dazu werden zwei Requests gemacht — einer für en-US, einer für de —
     * und der deutsche Name wird in das englische Objekt übernommen.
     *
     * @param tvShowId      TMDB-ID der Serie
     * @param seasonNumber  Staffelnummer
     * @param episodeNumber Episodennummer
     * @return              Vollständiges EpisodeJSON mit german_name befüllt
     */
    public EpisodeJSON getEpisodeDetails(int tvShowId, int seasonNumber, int episodeNumber) {
        String call = "getEpisodeDetails, tvShowId " + tvShowId + ", seasonNumber " + seasonNumber
                + ", episodeNumber " + episodeNumber;
        Log.info(ApiClient.class, "TMDB " + call);
        String path = "tv/" + tvShowId + "/season/" + seasonNumber + "/episode/" + episodeNumber;

        EpisodeJSON episodeEN = parse(getV3(path, Map.of("language", LANG_EN)), EpisodeJSON.class, call + " EN");
        episodeEN.show_id = tvShowId;

        EpisodeJSON episodeDE = parse(getV3(path, Map.of("language", LANG_DE)), EpisodeJSON.class, call + " DE");
        episodeEN.german_name = episodeDE.name;

        return episodeEN;
    }
    
    /**
     * Liefert die Credits (Cast und Crew) eines Films.
     *
     * @param movieId   TMDB-ID des Films
     * @return          CreditListJSON mit Cast und Crew
     */
    public CreditListJSON getMovieCredits(int movieId) {
        String call = "getMovieCredits, movieId " + movieId;
        Log.info(ApiClient.class, "TMDB " + call);
        String path = "movie/" + movieId + "/credits";

        return parse(getV3(path, Map.of("language", LANG_EN)), CreditListJSON.class, call);
    }
    
    /**
     * Liefert die aggregierten Credits (Cast und Crew) einer Serie über alle Staffeln.
     * Aggregiert bedeutet: Rollen und Jobs sind als Listen innerhalb von Cast/Crew
     * verschachtelt, nicht als flache Liste.
     *
     * @param tvShowId  TMDB-ID der Serie
     * @return          CreditListJSON mit aggregiertem Cast und Crew
     */
    public CreditListJSON getAggregatedTvShowCredits(int tvShowId) {
        String call = "getAggregatedTvShowCredits, tvShowId " + tvShowId;
        Log.info(ApiClient.class, "TMDB " + call);
        String path = "tv/" + tvShowId + "/aggregate_credits";

        return parse(getV3(path, Map.of("language", LANG_EN)), CreditListJSON.class, call);
    }
    
    /**
     * Liefert die regular Credits (Cast und Crew) einer Staffel.
     *
     * @param tvShowId      TMDB-ID der Serie
     * @param seasonNumber  Staffelnummer
     * @return              CreditListJSON mit aggregiertem Cast und Crew
     */
    public CreditListJSON getRegularSeasonCredits(int tvShowId, int seasonNumber) {
        String call = "getRegularSeasonCredits, tvShowId " + tvShowId + ", seasonNumber " + seasonNumber;
        Log.info(ApiClient.class, "TMDB " + call);
        String path = "tv/" + tvShowId + "/season/" + seasonNumber + "/credits";

        return parse(getV3(path, Map.of("language", LANG_EN)), CreditListJSON.class, call);
    }
    
    /**
     * Liefert die aggregierten Credits (Cast und Crew) einer Staffel.
     * Aggregiert bedeutet: Rollen und Jobs sind als Listen innerhalb von Cast/Crew
     * verschachtelt, nicht als flache Liste.
     *
     * @param tvShowId      TMDB-ID der Serie
     * @param seasonNumber  Staffelnummer
     * @return              CreditListJSON mit aggregiertem Cast und Crew
     */
    public CreditListJSON getAggregatedSeasonCredits(int tvShowId, int seasonNumber) {
        String call = "getAggregatedSeasonCredits, tvShowId " + tvShowId + ", seasonNumber " + seasonNumber;
        Log.info(ApiClient.class, "TMDB " + call);
        String path = "tv/" + tvShowId + "/season/" + seasonNumber + "/aggregate_credits";

        return parse(getV3(path, Map.of("language", LANG_EN)), CreditListJSON.class, call);
    }
    
    /**
     * Liefert die Detaildaten einer Person.
     *
     * @param personId  TMDB-ID der Person
     * @return          PersonJSON mit allen Detaildaten
     */
    public PersonJSON getPerson(int personId) {
        String call = "getPerson, personId " + personId;
        Log.info(ApiClient.class, "TMDB " + call);

        return parse(getV3("person/" + personId, Map.of("language", LANG_EN)), PersonJSON.class, call);
    }
    
    /**
     * Protokolliert die Antwort und mappt sie auf den Zieltyp.
     *
     * <p>{@code call} ist dieselbe Beschreibung, die der Aufrufer schon ins {@code Log.info}
     * gegeben hat — damit steht der Methodenname genau einmal in der Methode und kann in der
     * Fehlermeldung nicht veralten. Der Zieltyp kommt dazu, der sagt, welche Form nicht passte.</p>
     */
    private <T> T parse(String json, Class<T> type, String call) {
        Log.debug(ApiClient.class, "TMDB " + call + " response: " + json);
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB " + call + ": JSON-Mapping nach "
                    + type.getSimpleName() + " fehlgeschlagen", e);
        }
    }

    /**
     * Lädt alle Postergrößen zu einem Bildpfad, in der Reihenfolge von {@link PosterWidth}.
     *
     * <p>Je Größe ein Request. Der Aufrufer holt sie damit <b>vor</b> seiner Transaktion — in der
     * Schleife eines offenen {@code Connection} wären es Downloads bei offener Verbindung.</p>
     *
     * @param posterPath Bildpfad wie von der API geliefert, z.B. "/abc123.jpg"
     * @return           je ein Byte-Array, nie {@code null} und nie leer
     */
    public List<byte[]> getPosters(String posterPath) {
        List<byte[]> images = new ArrayList<>();
        for (PosterWidth width : PosterWidth.values())
            images.add(getImage(posterPath, width));

        return images;
    }

    /**
     * Lädt ein Bild vom TMDB-Bildserver herunter.
     *
     * @param path   Bildpfad wie von der API geliefert, z.B. "/abc123.jpg"
     * @param width  Gewünschte Breite
     * @return       Rohe Bilddaten als Byte-Array
     */
    private byte[] getImage(String path, PosterWidth width) {
        String urlString = "https://image.tmdb.org/t/p/" + width.token() + path;
        Log.info(ApiClient.class, "TMDB getImage, url " + urlString);
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(urlString).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            try (var is = con.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB getImage: Download fehlgeschlagen. url: " + urlString, e);
        }
    }

    /**
     * Sendet einen GET-Request gegen die TMDB v3-API.
     * API-Key und Session-ID werden automatisch als URL-Parameter angehängt.
     *
     * @param path      Pfad relativ zur v3-Basis-URL, z.B. "movie/123"
     * @param params    Zusätzliche URL-Parameter, z.B. language=en-US. Darf null sein.
     * @return          Der Response-Body als String
     */
    private String getV3(String path, Map<String, String> params) {
        StringBuilder url = new StringBuilder(BASE_URL_V3).append(path);
        url.append("?api_key=").append(Config.get("tmdb.v3.apiKey"));
        url.append("&session_id=").append(Config.get("tmdb.v3.sessionId"));
        if (params != null)
            for (Map.Entry<String, String> param : params.entrySet())
                url.append("&").append(param.getKey()).append("=").append(param.getValue());
        return sendGet(url.toString(), null);
    }

    /**
     * Sendet einen GET-Request gegen die TMDB v4-API.
     * Der Bearer-Token wird automatisch im Authorization-Header gesetzt.
     *
     * @param path      Pfad relativ zur v4-Basis-URL, z.B. "account/xyz/movie/rated"
     * @param params    Zusätzliche URL-Parameter. Darf null sein.
     */
    private String getV4(String path, Map<String, String> params) {
        StringBuilder url = new StringBuilder(BASE_URL_V4).append(path);
        if (params != null) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, String> param : params.entrySet()) {
                if (!first) url.append("&");
                url.append(param.getKey()).append("=").append(param.getValue());
                first = false;
            }
        }
        return sendGet(url.toString(), "Bearer " + Config.get("tmdb.v4.accessToken"));
    }

    /**
     * Führt den eigentlichen HTTP-GET-Request aus.
     *
     * @param urlString           Vollständige URL inkl. aller Parameter
     * @param authorizationHeader Wert für den Authorization-Header, oder null für keine
     * @return                    Response-Body als String
     */
    private String sendGet(String urlString, String authorizationHeader) {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(urlString).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestProperty("Accept", "application/json");
            if (authorizationHeader != null)
                con.setRequestProperty("Authorization", authorizationHeader);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null)
                    response.append(line.trim());
                return response.toString();
            }
        } catch (Exception e) {
            // Ohne Query-String: der trägt bei v3 den api_key und die session_id.
            int query = urlString.indexOf('?');
            throw new RuntimeException("[FAILFAST] TMDB API request failed for URL: "
                    + (query < 0 ? urlString : urlString.substring(0, query)), e);
        }
    }
}
