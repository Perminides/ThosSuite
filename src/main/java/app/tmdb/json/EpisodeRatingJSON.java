package app.tmdb.json;

import java.time.Instant;

public class EpisodeRatingJSON{
    public String air_date;
    public Integer episode_number;
    public Integer id;
    public String name;
	public  String original_name;
	public  String german_name;
    public String overview;
    public String production_code;
    public Integer runtime;
    public Integer season_number;
    public Integer show_id;
    public String still_path;
    public Double vote_average;
    public Integer vote_count;
    public Integer rating;
	public Instant first_rated_at;
	public Instant last_updated_at;
	public Boolean directorsFromShow;
	public Boolean actorsFromShow;
	public Boolean standsForWholeSeason;
	public String comment;
	
	public EpisodeRatingJSON() {
		super();
	}
		
}