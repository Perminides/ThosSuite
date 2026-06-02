package app.movie.model.json;

import java.time.LocalDate;
import java.util.ArrayList;

public class EpisodeJSON{
    public LocalDate air_date;
    public ArrayList<CrewJSON> crew;
    public Integer episode_number;
    public ArrayList<CastJSON> guest_stars;
    public String name;
    public String german_name;
    public String overview;
    public Integer id;
    public Integer show_id;
    public String production_code;
    public Integer runtime;
    public Integer season_number;
    public String still_path;
    public Integer vote_average;
    public Integer vote_count;
}