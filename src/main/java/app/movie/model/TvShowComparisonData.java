package app.movie.model;

import app.movie.model.json.TvShowJSON;

/**
 * Vergleichsdaten einer Serie aus der DB, um Änderungen zu erkennen.
 * Nur die Felder, die sich bei laufenden Serien ändern können.
 */
public class TvShowComparisonData {
    public final int numberOfSeasons;
    public final int numberOfEpisodes;
    public final String lastAirDate;
    public final String status;

    public TvShowComparisonData(int numberOfSeasons, int numberOfEpisodes,
            String lastAirDate, String status) {
        this.numberOfSeasons = numberOfSeasons;
        this.numberOfEpisodes = numberOfEpisodes;
        this.lastAirDate = lastAirDate;
        this.status = status;
    }
    
    /**
     * Vergleicht mit den Daten aus der API.
     * @return true wenn sich etwas geändert hat
     */
    public boolean differs(TvShowJSON webData) {
        if (webData.number_of_seasons != null && webData.number_of_seasons != numberOfSeasons)
            return true;
        if (webData.number_of_episodes != null && webData.number_of_episodes != numberOfEpisodes)
            return true;
        String webStatus = webData.status;
        if (webStatus != null && !webStatus.equals(status))
            return true;
        String webLastAirDate = webData.last_air_date != null ? webData.last_air_date.toString() : null;
        if (webLastAirDate != null && !webLastAirDate.equals(lastAirDate))
            return true;
        return false;
    }
}