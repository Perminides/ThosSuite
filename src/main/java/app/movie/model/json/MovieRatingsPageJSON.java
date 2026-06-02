package app.movie.model.json;

import java.util.ArrayList;

public class MovieRatingsPageJSON{
    public int page;
    public ArrayList<MovieRatingJSON> results;
    public int total_pages;
    public int total_results;
}