package app.movie;

/**
 * Die Postergrößen, die die Suite von TMDB holt.
 *
 * <p>Zu jedem Poster werden alle geladen: die kleine für die Listen, die große für die Karte. Die
 * Aufrufer laufen über {@code values()}, eine dritte Größe ist deshalb eine Konstante mehr.</p>
 *
 * <p>Die Zahl steht nur noch hier. In die Datenbank geht die <i>gemessene</i> Breite des Bildes,
 * nicht die angeforderte — siehe {@code PosterFiles.StoredPoster}.</p>
 */
enum PosterWidth {

	W92(92),
	W154(154);

	private final int pixels;

	PosterWidth(int pixels) {
		this.pixels = pixels;
	}

	/** Der Pfadbestandteil, den der TMDB-Bildserver erwartet: {@code w92}. */
	String token() {
		return "w" + pixels;
	}
}
