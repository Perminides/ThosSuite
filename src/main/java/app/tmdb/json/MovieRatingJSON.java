package app.tmdb.json;

import java.time.LocalDate;
import java.util.ArrayList;

public class MovieRatingJSON {
	public boolean adult;
	public String backdrop_path;
	public ArrayList<Integer> genre_ids;
	public Integer id;
	public String original_language;
	public String original_title;
	public String overview;
	public double popularity;
	public String poster_path;
	public LocalDate release_date;
	public String title;
	public boolean video;
	public double vote_average;
	public int vote_count;
	public AccountRatingJSON account_rating;
	public String comment;

	public MovieRatingJSON() {
		super();
	}

}