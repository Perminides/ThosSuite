package app.movie;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import app.shared.Config;
import app.shared.ImageUtils;
import app.shared.Log;

/**
 * Die Posterdateien der TMDB-Importe: Dateiname bauen, ablegen, nach einem Rollback wegräumen.
 *
 * <p>Alle Poster liegen im selben Ordner, gleich ob sie zu einem Film, einer Serie oder einer
 * Staffel gehören — deshalb steht der Pfad hier einmal und nicht in jedem Importer.</p>
 *
 * <p><b>Zwei Arten zu speichern, und der Name sagt welche.</b> {@link #storeNew} besteht darauf,
 * dass die Datei noch nicht da ist; {@link #storeIfAbsent} lässt eine vorhandene stehen. Welche
 * richtig ist, hängt am Aufrufer und steht im Javadoc der beiden.</p>
 *
 * <p>Beide nehmen den rohen {@code poster_path} von TMDB und das Bild — Maße und Dateiname
 * entstehen hier, nicht beim Aufrufer. Zurück kommt, was dessen DB-Schreiben braucht.</p>
 */
class PosterFiles {

	/** Die Sprache im Dateinamen und in der Bild-Tabelle. Alle Poster kommen in der TMDB-Grundsprache. */
	static final String LANGUAGE = "en-US";

	/** Ein abgelegtes Poster: wie die Datei heißt und welche Maße das Bild wirklich hat. */
	record StoredPoster(String filename, int width, int height) {}

	private PosterFiles() {
	}

	/**
	 * Legt ein Poster ab, das es noch nicht geben darf — für den Import eines <i>neuen</i> Titels.
	 *
	 * <p>Eine schon vorhandene Datei ist dort eine echte Namenskollision und fliegt. Damit das
	 * stimmt, räumt der Aufrufer seine eigenen Trümmer selbst weg: Poster liegen im Dateisystem und
	 * nicht in der Transaktion, ein {@code rollback()} erwischt sie nicht — dafür ist
	 * {@link #delete} da.</p>
	 */
	static StoredPoster storeNew(String posterPath, byte[] image) {
		StoredPoster stored = describe(posterPath, image);
		File file = pathFor(stored.filename()).toFile();
		if (file.exists())
			throw new RuntimeException("Bild existiert bereits, das sollte nicht passieren: " + stored.filename());
		write(file, image, stored.filename());
		return stored;
	}

	/**
	 * Legt ein Poster ab und lässt eine vorhandene Datei stehen — fürs Nachholen und für Staffeln,
	 * die sich das Bild ihrer Serie teilen.
	 */
	static StoredPoster storeIfAbsent(String posterPath, byte[] image) {
		StoredPoster stored = describe(posterPath, image);
		File file = pathFor(stored.filename()).toFile();
		if (!file.exists())
			write(file, image, stored.filename());
		return stored;
	}

	private static StoredPoster describe(String posterPath, byte[] image) {
		int[] dim = ImageUtils.dimensions(image);
		return new StoredPoster(buildFilename(posterPath, dim[0], dim[1]), dim[0], dim[1]);
	}

	/** {@code originalname_language_width_height.jpg} */
	private static String buildFilename(String posterPath, int width, int height) {
		String base = posterPath.startsWith("/") ? posterPath.substring(1) : posterPath;
		base = base.substring(0, base.lastIndexOf('.'));
		return base + "_" + LANGUAGE + "_" + width + "_" + height + ".jpg";
	}

	/**
	 * Räumt geschriebene Poster nach einem gescheiterten Import weg.
	 *
	 * <p>Das Dateisystem-Gegenstück zum {@code rollback()}. Wirft bewusst nicht weiter — hier wird
	 * ein bereits gescheiterter Import aufgeräumt, und ein Problem beim Aufräumen darf die
	 * eigentliche Ursache nicht verdecken. Es wird geloggt, mehr nicht.</p>
	 */
	static void delete(List<String> filenames) {
		for (String filename : filenames) {
			try {
				Files.deleteIfExists(pathFor(filename));
				Log.info(PosterFiles.class, "Poster nach Rollback entfernt: " + filename);
			} catch (Exception e) {
				Log.warn(PosterFiles.class, "Poster konnte nach Rollback nicht entfernt werden: " + filename + " (" + e + ")");
			}
		}
	}

	private static Path pathFor(String filename) {
		return Config.getPath("imageFolder").resolve("tmdb").resolve(filename);
	}

	private static void write(File file, byte[] image, String filename) {
		try {
			file.getParentFile().mkdirs();
			Files.write(file.toPath(), image);
			Log.debug(PosterFiles.class, "Bild gespeichert: " + filename);
		} catch (Exception e) {
			throw new RuntimeException("Poster speichern fehlgeschlagen. filename: " + filename, e);
		}
	}
}
