package app.tmdb.json;

import java.time.LocalDate;
import java.util.List;

public class SeasonJSON {

	public String _id;
	public LocalDate air_date;
	public LocalDate last_air_date;
	public List<EpisodeJSON> episodes;
	public Integer episode_count;
    public String name;
    public String germanName;
    public String overview;
    public Integer id; // Scheint die ID der Staffel zu sein, interessant...
    public Integer seasonID;
    public String poster_path;
    public Integer season_number;
    public Integer vote_average;
    public Integer tvShowID;
    
	public SeasonJSON() {
		super();
	}
}