package app.movie.model.json;

import java.util.ArrayList;

public class TvShowRatingJSON{
	public boolean adult;
    public String backdrop_path;
    public ArrayList<Integer> genre_ids;
    public Integer id;
    public ArrayList<String> origin_country;
    public String original_language;
    public String original_name;
    public String overview;
    public double popularity;
    public String poster_path;
    public String first_air_date;
    public String name;
    public double vote_average;
    public int vote_count;
    public AccountRatingJSON account_rating;
	public String comment;
    
	public TvShowRatingJSON() {
		super();
	}
}