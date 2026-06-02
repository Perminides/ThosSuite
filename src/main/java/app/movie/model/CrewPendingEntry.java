package app.movie.model;

public class CrewPendingEntry {
	public final int movieId;
	public final int personId;
	public final String personName;
	public final String job;
	public final String department;
	public final String creditId;
	public final String movieTitle;

	public CrewPendingEntry(int movieId, int personId, String personName, String job, String department, String creditId, String movieTitle) {
		this.movieId = movieId;
		this.personId = personId;
		this.personName = personName;
		this.job = job;
		this.department = department;
		this.creditId = creditId;
		this.movieTitle = movieTitle;
	}
}