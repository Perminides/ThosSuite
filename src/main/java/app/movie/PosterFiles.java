package app.movie;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import app.shared.Config;
import app.shared.Log;

/**
 * Die Posterdateien der TMDB-Importe: Dateiname bauen, ablegen, nach einem Rollback wegräumen.
 *
 * <p>Alle Poster liegen im selben Ordner, gleich ob sie zu einem Film, einer Serie oder einer
 * Staffel gehören — deshalb steht der Pfad hier einmal und nicht in jedem Importer.</p>
 *
 * <p><b>Zwei Arten zu speichern, und der Name sagt welche.</b> {@link #save} besteht darauf, dass
 * die Datei noch nicht da ist; {@link #saveIfAbsent} lässt eine vorhandene stehen. Welche richtig
 * ist, hängt am Aufrufer und steht im Javadoc der beiden.</p>
 */
class PosterFiles {

	private PosterFiles() {
	}

	/** {@code originalname_language_width_height.jpg} */
	static String buildFilename(String posterPath, String language, int width, int height) {
		String base = posterPath.startsWith("/") ? posterPath.substring(1) : posterPath;
		base = base.substring(0, base.lastIndexOf('.'));
		return base + "_" + language + "_" + width + "_" + height + ".jpg";
	}

	/**
	 * Legt ein Poster ab, das es noch nicht geben darf — für den Import eines <i>neuen</i> Titels.
	 *
	 * <p>Eine schon vorhandene Datei ist dort eine echte Namenskollision und fliegt. Damit das
	 * stimmt, räumt der Aufrufer seine eigenen Trümmer selbst weg: Poster liegen im Dateisystem und
	 * nicht in der Transaktion, ein {@code rollback()} erwischt sie nicht — dafür ist
	 * {@link #delete} da.</p>
	 */
	static void save(String filename, byte[] image) {
		File file = pathFor(filename).toFile();
		if (file.exists())
			throw new RuntimeException("Bild existiert bereits, das sollte nicht passieren: " + filename);
		write(file, image, filename);
	}

	/**
	 * Legt ein Poster ab und lässt eine vorhandene Datei stehen — fürs Nachholen und für Staffeln,
	 * die sich das Bild ihrer Serie teilen.
	 */
	static void saveIfAbsent(String filename, byte[] image) {
		File file = pathFor(filename).toFile();
		if (file.exists())
			return;
		write(file, image, filename);
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
