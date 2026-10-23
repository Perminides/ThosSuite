package scripts.flaggen;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Eine Zeile des Blatts, wie die Kartenbauer sie lesen: Zellen über ihre Überschrift und alles, was
 * sich aus der Zeile ableitet — Id, Bemerkung, Fläche auf der Weltkarte, Flaggenbild.
 *
 * <p>Dazu die Syntax der Toleranzklammer ({@code 4(9|5)}), die in jeder Spalte gleich gelesen wird.</p>
 */
final class Zeile {

	private final FlagSheet sheet;
	private final List<String> row;
	/** Flaggenbilder, die es noch nicht gibt — gesammelt für den Bericht am Ende, nicht abgebrochen. */
	private final Set<String> fehlendeBilder;

	Zeile(FlagSheet sheet, List<String> row, Set<String> fehlendeBilder) {
		this.sheet = sheet;
		this.row = row;
		this.fehlendeBilder = fehlendeBilder;
	}

	String land() {
		return sheet.country(row);
	}

	String value(String column) {
		return sheet.value(row, column);
	}

	/** Der gewertete Wert einer Zelle: alles vor ihrer Toleranzklammer. */
	String plain(String column) {
		return untolerated(value(column));
	}

	/** Eine Zelle ohne Frage: Eine Toleranzklammer hat dort keine Bedeutung und wäre ein Versehen. */
	String strict(String column) {
		String value = value(column);
		if (value.contains("("))
			throw new RuntimeException("In '" + column + "' gibt es keine Toleranzklammer: " + value);
		return value;
	}

	boolean hauptzeile() {
		return sheet.hauptzeile(row);
	}

	/** Ein Landesfakt steht nur in der Hauptzeile — gefüllt heißt also: Hauptzeile und nicht leer. */
	boolean hat(String column) {
		return hauptzeile() && !value(column).isBlank();
	}

	/**
	 * Die Karten-Id: {@code (Version − 1) × 1000 + Land-Id}. Version 1 behält also die Landnummer,
	 * die zweite Flagge eines Landes bekommt ihre eigenen Tausend.
	 *
	 * <p>Gerechnet statt im Blatt vergeben, weil die <b>Land-Id</b> als Wert gebraucht wird: Die
	 * handgeschriebenen Zusatzfragen gruppieren nach Land, nicht nach Flagge. Stünde im Blatt die
	 * fertige Karten-Id, ließe sich das Land daraus nur über eine Konvention zurückrechnen, die
	 * nichts erzwingt.</p>
	 *
	 * <p>Die Id trägt den Lernfortschritt und darf sich nie verschieben. Deshalb die Tausenderstufen:
	 * Eine weitere Flagge hängt sich hinten an, statt etwas dazwischenzuschieben. Bei 214 Ländern
	 * reicht das für <b>zehn</b> Versionen — die elfte ergäbe 10214 und läge im Block der
	 * Kartenkarten.</p>
	 */
	int id() {
		return (version() - 1) * 1000 + Integer.parseInt(sheet.number(row));
	}

	/** Die wievielte Flagge dieses Landes. Leer und {@code x} heißen beide: die erste. */
	int version() {
		String value = strict("Version");
		return FlagSheet.isSet(value) ? Integer.parseInt(value) : 1;
	}

	/** Spalte 2 der Deck-Datei. Die Suite liest sie nicht — sie macht den {@code git diff} lesbar. */
	String remark() {
		return land() + (version() > 1 ? " (" + version() + ")" : "");
	}

	/**
	 * Die Fläche, die auf der Weltkarte markiert wird. Fast immer ist das der deutsche Name; nur wo
	 * die Karte ihn nicht kennt, steht in {@code ShapeId} eine Ausnahme (Abchasien, Singapur).
	 */
	String shape() {
		String value = strict("ShapeId");
		return FlagSheet.isSet(value) ? value : land();
	}

	/**
	 * Die echte Flagge als SVG, benannt nach dem deutschen Namen und ab der zweiten Flagge mit der
	 * Version dahinter: {@code Afghanistan2.svg}. Den Ordner kennt der Lader; dass die Datei da ist,
	 * prüfen wir trotzdem — sonst bliebe die Karte am Ende einfach leer.
	 */
	String image() {
		String file = land() + (version() > 1 ? String.valueOf(version()) : "") + ".svg";
		if (!Files.exists(FlagDeckGenerator.DATA.resolve("images").resolve("svg").resolve(file)))
			fehlendeBilder.add(file);
		return file;
	}

	// ---- Toleranzklammer ------------------------------------------------------

	/** Alles vor einer Toleranzklammer: {@code 4(9|5)} ist die 4. */
	static String untolerated(String value) {
		int open = value.indexOf('(');
		return open < 0 ? value.trim() : value.substring(0, open).trim();
	}

	/**
	 * Die Werte in der Toleranzklammer: {@code 4(9|5)} lässt die 9 und die 5 durchgehen. Getrennt wird
	 * wie überall im Blatt mit {@code |} — ein Komma hieße Schreibweisen derselben Antwort.
	 */
	static List<String> bracket(String value) {
		int open = value.indexOf('(');
		if (open < 0)
			return List.of();
		int close = value.lastIndexOf(')');
		if (close < open)
			throw new RuntimeException("Die Toleranzklammer ist nicht geschlossen: " + value);
		return split(value.substring(open + 1, close));
	}

	/** Die tolerierten Werte einer Zelle, übersetzt in ihre Antworttexte. */
	static List<String> tolerated(String cell, Function<String, String> text) {
		List<String> result = new ArrayList<>();
		for (String code : bracket(cell))
			result.add(text.apply(code));
		return result;
	}

	static List<String> split(String value) {
		List<String> result = new ArrayList<>();
		for (String part : value.split("\\|"))
			if (!part.isBlank())
				result.add(part.trim());
		return result;
	}
}
