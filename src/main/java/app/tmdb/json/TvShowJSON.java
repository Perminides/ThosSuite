package app.tmdb.json;

import java.time.LocalDate;
import java.util.ArrayList;

public class TvShowJSON {
    public Boolean adult;
    public String backdrop_path;
    public ArrayList<CreatedByJSON> created_by;
    public ArrayList<Integer> episode_run_time;
    public LocalDate first_air_date;
    public ArrayList<GenreJSON> genres;
    public String homepage;
    public Integer id;
    public Boolean in_production;
    public ArrayList<String> languages;
    public LocalDate last_air_date;
    public EpisodeToAirJSON last_episode_to_air;
    public String name;
    public String german_name; // Wird durch einen zweiten Request mit language=de befüllt, nicht von der API direkt
    public EpisodeToAirJSON next_episode_to_air;
    public ArrayList<NetworkJSON> networks;
    public Integer number_of_episodes;
    public Integer number_of_seasons;
    public ArrayList<String> origin_country;
    public String original_language;
    public String original_name;
    public String overview;
    public Double popularity;
    public String poster_path;
    public ArrayList<ProductionCompanyJSON> production_companies;
    public ArrayList<ProductionCountryJSON> production_countries;
    public ArrayList<SeasonJSON> seasons;
    public ArrayList<SpokenLanguageJSON> spoken_languages;
    public String status;
    public String tagline;
    public String type;
    public Double vote_average;
    public Integer vote_count;
}