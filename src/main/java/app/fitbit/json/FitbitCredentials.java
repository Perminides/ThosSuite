package app.fitbit.json;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Repräsentiert die OAuth2-Credentials für die Fitbit-API.
 * Wird in fitbit_credentials.json gespeichert und bei jedem Token-Refresh aktualisiert.
 * 
 * @param access_token Das aktuelle Zugriffsticket für API-Calls (läuft nach ~8h ab)
 * @param refresh_token Token zum Erneuern des access_token
 * @param expires_in Gültigkeitsdauer des access_token in Sekunden (typisch: 28800 = 8h)
 * @param created Zeitstempel wann diese Credentials erstellt/erneuert wurden
 */
public record FitbitCredentials(
    @JsonProperty("access_token") String access_token,
    @JsonProperty("refresh_token") String refresh_token,
    @JsonProperty("expires_in") String expires_in,
    @JsonProperty("created") LocalDateTime created
) {
}