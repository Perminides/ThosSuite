package app.tmdb;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.config.Config;
import app.tmdb.json.CreditListJSON;
import app.tmdb.json.EpisodeJSON;
import app.tmdb.json.EpisodeRatingsPageJSON;
import app.tmdb.json.MovieJSON;
import app.tmdb.json.MovieRatingsPageJSON;
import app.tmdb.json.PersonJSON;
import app.tmdb.json.SeasonJSON;
import app.tmdb.json.TvShowJSON;
import app.tmdb.json.TvShowRatingsPageJSON;

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
public class TmdbApiClient {

    private static final Logger log = Logger.getLogger(TmdbApiClient.class.getName());

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
        log.info("TMDB getRatedMovies, page " + page);
        String path = "account/" + Config.get("tmdb.v4.accountId") + "/movie/rated";
        Map<String, String> params = Map.of("sort_by", "created_at.desc", "page", String.valueOf(page));
        String json = getV4(path, params);
        log.fine("TMDB getRatedMovies response: " + json);
        try {
            return mapper.readValue(json, MovieRatingsPageJSON.class);
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB getRatedMovies: JSON-Mapping fehlgeschlagen. Page: " + page, e);
        }
    }
    
    /**
     * Liefert eine Seite bewerteter Serien vom TMDB-Account, absteigend nach
     * Bewertungsdatum sortiert.
     *
     * @param page  Seitennummer, beginnt bei 1
     * @return      Die gemappte Seite mit Bewertungen und Paginierungsinformationen
     */
    public TvShowRatingsPageJSON getRatedTvShows(int page) {
        log.info("TMDB getRatedTvShows, page " + page);
        String path = "account/" + Config.get("tmdb.v4.accountId") + "/tv/rated";
        Map<String, String> params = Map.of("sort_by", "created_at.desc", "page", String.valueOf(page));
        String json = getV4(path, params);
        log.fine("TMDB getRatedTvShows response: " + json);
        try {
            return mapper.readValue(json, TvShowRatingsPageJSON.class);
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB getRatedTvShows: JSON-Mapping fehlgeschlagen. Page: " + page, e);
        }
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
        log.info("TMDB getRatedEpisodes, page " + page);
        String path = "account/" + Config.get("tmdb.v3.accountId") + "/rated/tv/episodes";
        Map<String, String> params = Map.of("sort_by", "created_at.desc", "page", String.valueOf(page));
        String json = getV3(path, params);
        log.fine("TMDB getRatedEpisodes response: " + json);
        try {
            return mapper.readValue(json, EpisodeRatingsPageJSON.class);
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB getRatedEpisodes: JSON-Mapping fehlgeschlagen. Page: " + page, e);
        }
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
        log.info("TMDB getMovieDetails, movieId " + movieId);
        String path = "movie/" + movieId;
        String jsonEn = getV3(path, Map.of("language", LANG_EN));
        log.fine("TMDB getMovieDetails EN response: " + jsonEn);
        try {
            MovieJSON movieEN = mapper.readValue(jsonEn, MovieJSON.class);
            String jsonDe = getV3(path, Map.of("language", LANG_DE));
            log.fine("TMDB getMovieDetails DE response: " + jsonDe);
            MovieJSON movieDE = mapper.readValue(jsonDe, MovieJSON.class);
            movieEN.german_title = movieDE.title;
            return movieEN;
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB getMovieDetails: JSON-Mapping fehlgeschlagen. movieId: " + movieId, e);
        }
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
        log.info("TMDB getTvShowDetails, tvShowId " + tvShowId);
        String path = "tv/" + tvShowId;
        String jsonEn = getV3(path, Map.of("language", LANG_EN));
        log.fine("TMDB getTvShowDetails EN response: " + jsonEn);
        try {
            TvShowJSON tvShowEN = mapper.readValue(jsonEn, TvShowJSON.class);
            String jsonDe = getV3(path, Map.of("language", LANG_DE));
            log.fine("TMDB getTvShowDetails DE response: " + jsonDe);
            TvShowJSON tvShowDE = mapper.readValue(jsonDe, TvShowJSON.class);
            tvShowEN.german_name = tvShowDE.name;
            return tvShowEN;
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB getTvShowDetails: JSON-Mapping fehlgeschlagen. tvShowId: " + tvShowId, e);
        }
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
        log.info("TMDB getSeasonDetails, tvShowId " + tvShowId + ", seasonNumber " + seasonNumber);
        String path = "tv/" + tvShowId + "/season/" + seasonNumber;
        String jsonEn = getV3(path, Map.of("language", LANG_EN));
        log.fine("TMDB getSeasonDetails EN response: " + jsonEn);
        try {
            SeasonJSON seasonEN = mapper.readValue(jsonEn, SeasonJSON.class);
            seasonEN.tvShowID = tvShowId;
            seasonEN.season_number = seasonNumber;
            // last_air_date aus Episoden berechnen, da die API dieses nicht direkt liefert
            if (seasonEN.air_date != null && seasonEN.episodes != null) {
                seasonEN.last_air_date = seasonEN.air_date;
                for (EpisodeJSON episode : seasonEN.episodes)
                    if (episode.air_date != null && episode.air_date.isAfter(seasonEN.last_air_date))
                        seasonEN.last_air_date = episode.air_date;
            }
            String jsonDe = getV3(path, Map.of("language", LANG_DE));
            log.fine("TMDB getSeasonDetails DE response: " + jsonDe);
            SeasonJSON seasonDE = mapper.readValue(jsonDe, SeasonJSON.class);
            seasonEN.germanName = seasonDE.name;
            return seasonEN;
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB getSeasonDetails: JSON-Mapping fehlgeschlagen. tvShowId: " + tvShowId + ", seasonNumber: " + seasonNumber, e);
        }
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
        log.info("TMDB getEpisodeDetails, tvShowId " + tvShowId + ", seasonNumber " + seasonNumber + ", episodeNumber " + episodeNumber);
        String path = "tv/" + tvShowId + "/season/" + seasonNumber + "/episode/" + episodeNumber;
        String jsonEn = getV3(path, Map.of("language", LANG_EN));
        log.fine("TMDB getEpisodeDetails EN response: " + jsonEn);
        try {
            EpisodeJSON episodeEN = mapper.readValue(jsonEn, EpisodeJSON.class);
            episodeEN.show_id = tvShowId;
            String jsonDe = getV3(path, Map.of("language", LANG_DE));
            log.fine("TMDB getEpisodeDetails DE response: " + jsonDe);
            EpisodeJSON episodeDE = mapper.readValue(jsonDe, EpisodeJSON.class);
            episodeEN.german_name = episodeDE.name;
            return episodeEN;
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB getEpisodeDetails: JSON-Mapping fehlgeschlagen. tvShowId: " + tvShowId + ", seasonNumber: " + seasonNumber + ", episodeNumber: " + episodeNumber, e);
        }
    }
    
    /**
     * Liefert die Credits (Cast und Crew) eines Films.
     *
     * @param movieId   TMDB-ID des Films
     * @return          CreditListJSON mit Cast und Crew
     */
    public CreditListJSON getMovieCredits(int movieId) {
        log.info("TMDB getMovieCredits, movieId " + movieId);
        String path = "movie/" + movieId + "/credits";
        String json = getV3(path, Map.of("language", LANG_EN));
        log.fine("TMDB getMovieCredits response: " + json);
        try {
            return mapper.readValue(json, CreditListJSON.class);
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB getMovieCredits: JSON-Mapping fehlgeschlagen. movieId: " + movieId, e);
        }
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
        log.info("TMDB getAggregatedTvShowCredits, tvShowId " + tvShowId);
        String path = "tv/" + tvShowId + "/aggregate_credits";
        String json = getV3(path, Map.of("language", LANG_EN));
        log.fine("TMDB getAggregatedTvShowCredits response: " + json);
        try {
            return mapper.readValue(json, CreditListJSON.class);
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB getAggregatedTvShowCredits: JSON-Mapping fehlgeschlagen. tvShowId: " + tvShowId, e);
        }
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
        log.info("TMDB getAggregatedSeasonCredits, tvShowId " + tvShowId + ", seasonNumber " + seasonNumber);
        String path = "tv/" + tvShowId + "/season/" + seasonNumber + "/aggregate_credits";
        String json = getV3(path, Map.of("language", LANG_EN));
        log.fine("TMDB getAggregatedSeasonCredits response: " + json);
        try {
            return mapper.readValue(json, CreditListJSON.class);
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB getAggregatedSeasonCredits: JSON-Mapping fehlgeschlagen. tvShowId: " + tvShowId + ", seasonNumber: " + seasonNumber, e);
        }
    }
    
    /**
     * Liefert die Detaildaten einer Person.
     *
     * @param personId  TMDB-ID der Person
     * @return          PersonJSON mit allen Detaildaten
     */
    public PersonJSON getPerson(int personId) {
        log.info("TMDB getPerson, personId " + personId);
        String json = getV3("person/" + personId, Map.of("language", LANG_EN));
        log.fine("TMDB getPerson response: " + json);
        try {
            return mapper.readValue(json, PersonJSON.class);
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] TMDB getPerson: JSON-Mapping fehlgeschlagen. personId: " + personId, e);
        }
    }
    
    /**
     * Lädt ein Bild von der TMDB-Bildserver herunter.
     *
     * @param path   Bildpfad wie von der API geliefert, z.B. "/abc123.jpg"
     * @param width  Gewünschte Breite, z.B. "w92" oder "w154"
     * @return       Rohe Bilddaten als Byte-Array
     */
    public byte[] getImage(String path, String width) {
        String urlString = "https://image.tmdb.org/t/p/" + width + path;
        log.info("TMDB getImage, url " + urlString);
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
            throw new RuntimeException("[FAILFAST] TMDB API request failed for URL: " + urlString, e);
        }
    }
}