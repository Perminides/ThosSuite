package app.movie.model;

public class NullCommentEntry {
    public final int movieId;
    public final String title;
    public final String germanTitle;
    public final int rating;
    public final String firstRatedAt;

    public NullCommentEntry(int movieId, String title, String germanTitle,
            int rating, String firstRatedAt) {
        this.movieId = movieId;
        this.title = title;
        this.germanTitle = germanTitle;
        this.rating = rating;
        this.firstRatedAt = firstRatedAt;
    }
}