package app.movie.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.shared.model.CardData;

/**
 * Baut das dumme {@link CardData}-DTO für den Viewer. Die typ-spezifische Anzeige-Logik (welche Header-/
 * Detail-Zeilen, Auswahl von Overview und Bild) lebt hier in der movie-Domäne; das skin-Paket bekommt nur
 * noch das fertig formatierte Record gereicht und rendert es uniform.
 *
 * Das Repository reicht nur Rohfelder durch und enthält keine Anzeige-Logik.
 */
public final class CardDataFactory {

	private CardDataFactory() {}

	// -------------------------------------------------------------------------
	// Film
	// -------------------------------------------------------------------------

	/**
	 * Factory für Filme.
	 *
	 * Header (bold):
	 *   "Title (Year)"
	 *   "German Title" — nur wenn abweichend vom Title
	 *
	 * Detail: keine
	 * sortDate: release_date
	 */
	public static CardData forMovie(
			int id,
			String title,
			String germanTitle,
			String originalTitle,
			LocalDate releaseDate,
			int rating,
			LocalDate ratedAt,
			List<String> directors,
			List<String> actors,
			String overview,
			String comment,
			String imageFilename) {

		List<String> header = new ArrayList<>();
		String year = releaseDate != null ? String.valueOf(releaseDate.getYear()) : "?";
		header.add(title + " (" + year + ")");

		if (germanTitle != null && !germanTitle.equals(title)) {
			header.add(germanTitle);
		}

		return new CardData(
				id,
				Collections.unmodifiableList(header),
				List.of(),
				releaseDate,
				rating,
				ratedAt,
				Collections.unmodifiableList(directors),
				Collections.unmodifiableList(actors),
				overview,
				comment,
				imageFilename);
	}

	// -------------------------------------------------------------------------
	// Serie (TvShow)
	// -------------------------------------------------------------------------

	/**
	 * Factory für bewertete Serien.
	 *
	 * Header (bold):
	 *   "Name (FirstAirYear - LastAirYear)"
	 *   "German Name" — nur wenn abweichend vom Name
	 *
	 * Detail:
	 *   "Seasons: 12"
	 *   "Episodes: 120"
	 *
	 * sortDate: first_air_date
	 */
	public static CardData forTvShow(
			int id,
			String name,
			String germanName,
			LocalDate firstAirDate,
			LocalDate lastAirDate,
			int numberOfSeasons,
			int numberOfEpisodes,
			int rating,
			LocalDate ratedAt,
			List<String> directors,
			List<String> actors,
			String overview,
			String comment,
			String imageFilename) {

		List<String> header = new ArrayList<>();
		String firstYear = firstAirDate != null ? String.valueOf(firstAirDate.getYear()) : "?";
		String lastYear = lastAirDate != null ? String.valueOf(lastAirDate.getYear()) : "?";
		header.add(name + " (" + firstYear + " - " + lastYear + ")");

		if (germanName != null && !germanName.equals(name)) {
			header.add(germanName);
		}

		List<String> detail = new ArrayList<>();
		detail.add("Seasons: " + numberOfSeasons);
		detail.add("Episodes: " + numberOfEpisodes);

		return new CardData(
				id,
				Collections.unmodifiableList(header),
				Collections.unmodifiableList(detail),
				firstAirDate,
				rating,
				ratedAt,
				Collections.unmodifiableList(directors),
				Collections.unmodifiableList(actors),
				overview,
				comment,
				imageFilename);
	}

	// -------------------------------------------------------------------------
	// Episode
	// -------------------------------------------------------------------------

	/**
	 * Factory für bewertete Episoden.
	 *
	 * Zwei Fälle, gesteuert über ratedSeason:
	 *
	 * ratedSeason=true (Staffelbewertung):
	 *   Header: "Show Name / Show German Name"
	 *           "Season Name (SeasonFirstAirYear)"
	 *   Detail: "Episode: S01E05"
	 *   Overview: season_overview, Fallback show_overview
	 *   sortDate: season release_date
	 *
	 * ratedSeason=false (Einzelepisode):
	 *   Header: "Show Name / Show German Name"
	 *           "Episode Name / Episode German Name (EpisodeAirYear)"
	 *   Detail: "Episode: S01E05"
	 *   Overview: episode_overview (kein Fallback)
	 *   sortDate: episode_release_date
	 *
	 * Bild: season_image_filename, Fallback show_image_filename.
	 *
	 * Wenn Show-Name = Show-German-Name, kein " / German Name" im Header.
	 * Analog für Episode-Name.
	 *
	 * Die Factory bekommt Rohfelder und löst Overview und Bild selbst auf.
	 */
	public static CardData forEpisode(
			int id,
			String showName,
			String showGermanName,
			String seasonName,
			String seasonGermanName,
			String episodeName,
			String episodeGermanName,
			int seasonNumber,
			int episodeNumber,
			LocalDate seasonFirstAirDate,
			LocalDate episodeAirDate,
			boolean ratedSeason,
			int rating,
			LocalDate ratedAt,
			List<String> directors,
			List<String> actors,
			String episodeOverview,
			String seasonOverview,
			String showOverview,
			String comment,
			String seasonImageFilename,
			String showImageFilename) {

		List<String> header = new ArrayList<>();
		List<String> detail = new ArrayList<>();

		// Zeile 1: Show-Name, ggf. mit deutschem Namen
		String showLine = showName;
		if (showGermanName != null && !showGermanName.equals(showName)) {
			showLine = showName + " / " + showGermanName;
		}
		header.add(showLine);

		String overview;
		if (ratedSeason) {
			// Staffelbewertung
			String seasonLine = seasonName != null ? seasonName : "Season " + seasonNumber;
			if (seasonGermanName != null && !seasonGermanName.equals(seasonName)) {
				seasonLine = seasonLine + " / " + seasonGermanName;
			}
			String seasonYear = seasonFirstAirDate != null
					? " (" + seasonFirstAirDate.getYear() + ")"
					: "";
			header.add(seasonLine + seasonYear);
			overview = (seasonOverview == null || seasonOverview.isBlank())
					? showOverview : seasonOverview;
		} else {
			// Einzelepisode
			String episodeLine = episodeName != null ? episodeName : "Episode " + episodeNumber;
			if (episodeGermanName != null && !episodeGermanName.equals(episodeName)) {
				episodeLine = episodeLine + " / " + episodeGermanName;
			}
			String episodeYear = episodeAirDate != null
					? " (" + episodeAirDate.getYear() + ")"
					: "";
			header.add(episodeLine + episodeYear);
			overview = episodeOverview;
		}

		detail.add("Episode: S" + String.format("%02d", seasonNumber)
				+ "E" + String.format("%02d", episodeNumber));

		String imageFilename = seasonImageFilename != null
				? seasonImageFilename : showImageFilename;

		return new CardData(
				id,
				Collections.unmodifiableList(header),
				Collections.unmodifiableList(detail),
				ratedSeason ? seasonFirstAirDate : episodeAirDate,
				rating,
				ratedAt,
				Collections.unmodifiableList(directors),
				Collections.unmodifiableList(actors),
				overview,
				comment,
				imageFilename);
	}
}