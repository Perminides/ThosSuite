package app.tmdb.json;

import java.time.LocalDate;
import java.util.ArrayList;

public class MovieJSON extends SomethingWithAPosterJSON{
    public Boolean adult;
    public String backdrop_path;
    public BelongsToCollectionJSON belongs_to_collection;
    public Integer budget;
    public ArrayList<GenreJSON> genres;
    public String homepage;
    public Integer id;
    public String imdb_id;
    public String original_language;
    public String original_title;
    public String overview;
    public Double popularity;
    public String poster_path;
    public ArrayList<ProductionCompanyJSON> production_companies;
    public ArrayList<ProductionCountryJSON> production_countries;
    public LocalDate release_date;
    public Long revenue;
    public Integer runtime;
    public ArrayList<SpokenLanguageJSON> spoken_languages;
    public String status;
    public String tagline;
    public String title;
    public String german_title;
    public Boolean video;
    public Double vote_average;
    public Integer vote_count;
    
	@Override
	public Integer getId() {
		return id;
	}
	@Override
	public void setId(Integer id) {
		this.id = id;		
	}
	@Override
	public String getPoster_path() {
		return poster_path;
	}
	@Override
	public void setPoster_path(String poster_path) {
		this.poster_path = poster_path;
	}
}