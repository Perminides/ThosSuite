package scripts.flaggen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Prüft das Systematik-Blatt auf Widersprüche, die man am Bild nicht sieht.
 *
 * <p>Aufruf: {@code java scripts.flaggen.FlagSheetCheck} — holt das Blatt selbst,
 * mit {@code --offline} nimmt es die vorhandene {@code systematik.csv}.</p>
 *
 * <p>Vier Sorten von Prüfung:</p>
 * <ol>
 *   <li><b>Kettenregeln</b> — eine Spalte muss <b>genau dann</b> einen Wert tragen, wenn die Frage
 *       davor hierher geführt hat. Eine Flagge mit Hintergrundtyp 2 ohne Kreuzantworten ist ein
 *       Widerspruch, egal wie sie aussieht.</li>
 *   <li><b>Elemente</b> — je Spalte ein Vokabular, dazu die Kopplung von Element und Ort. Seit dem
 *       Umbau auf vier Spalten je Element sind das keine Ketten mehr, sondern Wertelisten.</li>
 *   <li><b>Invarianten</b> — was sich nicht als Regel über eine Spalte schreiben lässt.</li>
 *   <li><b>Ausreißer</b> — Einzelgänger, die sich von einem großen Cluster in genau einem Attribut
 *       unterscheiden. Findet Tippfehler, meldet aber auch echte Sonderfälle; die Liste ist zum
 *       Durchsehen, nicht zum Abarbeiten.</li>
 * </ol>
 *
 * <p>Beim Regelschreiben: Eine Abweichung ist zuerst ein Verdacht gegen die Regel, nicht gegen die
 * Daten.</p>
 */
public class FlagSheetCheck {

	/** Bedingungsspalte, die Werte die hinführen, und die Spalte die dann gesetzt sein muss. */
	private record Chain(String column, Set<String> values, String required) {}

	private static final List<Chain> CHAINS = List.of(
			new Chain("Hintergrundtyp", Set.of("0"), "W-Streifen"),
			new Chain("W-Streifen", Set.of("3"), "3W"),
			new Chain("W-Streifen", Set.of("5"), "5W"),
			new Chain("Hintergrundtyp", Set.of("1"), "S-Streifen"),
			new Chain("Hintergrundtyp", Set.of("1"), "S-Anordnung"),
			new Chain("Hintergrundtyp", Set.of("2"), "Kreuzausrichtung"),
			new Chain("Hintergrundtyp", Set.of("2"), "Kreuzarme"),
			new Chain("Hintergrundtyp", Set.of("3"), "Diagonal Richtung"),
			new Chain("Hintergrundtyp", Set.of("3"), "Diagonal Anzahl Streifen"),
			new Chain("Hintergrundtyp", Set.of("5"), "SW Streifen"),
			new Chain("Hintergrundtyp", Set.of("7"), "Spezial"),
			new Chain("Dreieck von links?", Set.of("1", "2", "3", "4", "5"),
					"Die Dreiecksform(en) bestehen aus wie vielen Farben?"));

	/** Die acht Farben. Für Elemente dürfen mehrere in einer Zelle stehen, mit {@code |} getrennt. */
	private static final Set<String> COLORS = new LinkedHashSet<>(
			List.of("Rot", "Blau", "Hellblau", "Grün", "Gelb", "Orange", "Weiß", "Schwarz"));

	/** 0…8 sind die Richtungen vom Mittelpunkt, 9 ist verstreut. */
	private static final Set<String> POSITIONS = Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");

	private final FlagSheet sheet;
	private int problems;

	private FlagSheetCheck(FlagSheet sheet) {
		this.sheet = sheet;
	}

	public static void main(String[] args) {
		FlagSheet sheet = List.of(args).contains("--offline") ? FlagSheet.read() : FlagSheet.fetch();
		FlagSheetCheck check = new FlagSheetCheck(sheet);
		System.out.printf("%d Flaggen, %d Attribute%n%n", sheet.flags().size(), sheet.attributeNames().size());
		if (!sheet.withoutSignature().isEmpty())
			System.out.println("  ! ohne Signatur, wird nicht geprüft: " + String.join(", ", sheet.withoutSignature()) + "\n");

		check.checkChains();
		check.checkElements();
		check.checkInvariants();
		check.reportOutliers();
		System.out.println("Widersprüche gesamt: " + check.problems);
	}

	// ---- 1. Kettenregeln -------------------------------------------------------

	private void checkChains() {
		System.out.println("Kettenregeln");
		for (Chain chain : CHAINS) {
			String title = chain.column() + "=" + String.join("/", new java.util.TreeSet<>(chain.values()))
					+ " -> " + chain.required();
			report(title, row -> chain.values().contains(sheet.value(row, chain.column()))
					!= FlagSheet.isSet(sheet.value(row, chain.required())),
					row -> chain.required() + "=" + orEmpty(sheet.value(row, chain.required())));
		}
		System.out.println();
	}

	// ---- 2. Elemente -----------------------------------------------------------

	private void checkElements() {
		System.out.println("Zusatzelemente");
		for (int slot = 1; slot <= 4; slot++) {
			int n = slot;
			report("E" + n + " gesetzt <-> E" + n + " Position gesetzt",
					row -> FlagSheet.isSet(sheet.value(row, "E" + n))
							!= FlagSheet.isSet(sheet.value(row, "E" + n + " Position")),
					row -> "E" + n + "=" + orEmpty(sheet.value(row, "E" + n))
							+ ", Position=" + orEmpty(sheet.value(row, "E" + n + " Position")));
			if (n > 1)
				report("E" + n + " gesetzt, aber E" + (n - 1) + " leer (Lücke)",
						row -> FlagSheet.isSet(sheet.value(row, "E" + n))
								&& !FlagSheet.isSet(sheet.value(row, "E" + (n - 1))),
						row -> "E" + (n - 1) + " leer");
			report("E" + n + " Position ist 0..9",
					row -> FlagSheet.isSet(sheet.value(row, "E" + n + " Position"))
							&& !arePositions(sheet.value(row, "E" + n + " Position")),
					row -> "Position=" + sheet.value(row, "E" + n + " Position"));
			report("E" + n + " Farbe aus der Farbliste",
					row -> FlagSheet.isSet(sheet.value(row, "E" + n + " Farbe"))
							&& !areColors(sheet.value(row, "E" + n + " Farbe")),
					row -> "Farbe=" + sheet.value(row, "E" + n + " Farbe"));
			report("E" + n + " Anzahl ist eine Zahl",
					row -> FlagSheet.isSet(sheet.value(row, "E" + n + " Anzahl"))
							&& !sheet.value(row, "E" + n + " Anzahl").matches("\\d+"),
					row -> "Anzahl=" + sheet.value(row, "E" + n + " Anzahl"));
		}
		report("Hintergrundfarben aus der Farbliste",
				row -> !sheet.value(row, "Hintergrundfarben").isEmpty()
						&& !areColors(sheet.value(row, "Hintergrundfarben")),
				row -> "Hintergrundfarben=" + sheet.value(row, "Hintergrundfarben"));
		System.out.println();
	}

	/** Der Ortswert samt Toleranzklammer — {@code 4(9|5)} ist nur gültig, wenn 4, 9 und 5 es sind. */
	private static boolean arePositions(String value) {
		for (String part : positionValues(value))
			if (!POSITIONS.contains(part))
				return false;
		return true;
	}

	/** Der Hauptwert und die tolerierten aus der Klammer. Getrennt wird mit {@code |}, nie mit Komma. */
	private static List<String> positionValues(String value) {
		int open = value.indexOf('(');
		int close = value.lastIndexOf(')');
		if (open < 0)
			return List.of(value.trim());
		if (close < open)
			return List.of(value); // nicht geschlossen — fällt als kein gültiger Ort auf
		List<String> result = new ArrayList<>();
		result.add(value.substring(0, open).trim());
		for (String part : value.substring(open + 1, close).split("\\|"))
			if (!part.isBlank())
				result.add(part.trim());
		return result;
	}

	/** Ein fuehrendes {@code &} heisst „gilt fuers ganze Element" und ist keine Farbe. */
	private static boolean areColors(String value) {
		for (String part : (value.startsWith("&") ? value.substring(1) : value).split("\\|"))
			if (!COLORS.contains(part.trim()))
				return false;
		return true;
	}

	// ---- 3. Invarianten --------------------------------------------------------

	private void checkInvariants() {
		System.out.println("Invarianten");
		// Eine einfarbige Fläche ohne Gösch, ohne Dreieck und ohne Element wäre leer. Die gibt es nicht.
		report("einfarbig, ohne Gösch, ohne Dreieck, ohne Element (gibt es nicht)",
				row -> sheet.value(row, "Hintergrundtyp").equals("4")
						&& sheet.value(row, "Gösch?").equals("0")
						&& sheet.value(row, "Dreieck von links?").equals("0")
						&& !FlagSheet.isSet(sheet.value(row, "E1")),
				row -> "keine Fläche zum Einfärben");
		// Die Id wird von Hand vergeben und trägt den Lernfortschritt. Zwei Karten mit derselben Id
		// gäbe es nur einmal im Deck — die zweite verschwände beim Schreiben.
		report("ID ist eine Zahl",
				row -> !sheet.number(row).matches("[1-9][0-9]*"),
				row -> "ID=" + orEmpty(sheet.number(row)));
		checkUnique("ID", this::sheetId);
		// Was generiert werden soll, braucht seine Farben — auch die des Gösch und des Dreiecks.
		// Ohne Farbe bliebe die Fläche grau und würde stillschweigend nicht gefragt.
		report("Generieren=1 -> Hintergrundfarben gesetzt",
				row -> sheet.value(row, "Generieren").equals("1")
						&& sheet.value(row, "Hintergrundfarben").isEmpty(),
				row -> "Hintergrundfarben fehlen");
		report("Generieren=1 und Gösch -> Gösch Farbe gesetzt",
				row -> sheet.value(row, "Generieren").equals("1")
						&& sheet.value(row, "Gösch?").equals("1")
						&& !FlagSheet.isSet(sheet.value(row, "Gösch Farbe")),
				row -> "Gösch Farbe fehlt");
		report("Generieren=1 und Dreieck -> Dreieck Farbe gesetzt",
				row -> sheet.value(row, "Generieren").equals("1")
						&& !sheet.value(row, "Dreieck von links?").equals("0")
						&& FlagSheet.isSet(sheet.value(row, "Dreieck von links?"))
						&& !FlagSheet.isSet(sheet.value(row, "Dreieck Farbe")),
				row -> "Dreieck Farbe fehlt");
		// Die Version hängt am Namen der SVG-Datei und trennt zwei Flaggen desselben Landes.
		report("Version ist eine Zahl ab 1",
				row -> FlagSheet.isSet(sheet.value(row, "Version"))
						&& !sheet.value(row, "Version").matches("[1-9][0-9]*"),
				row -> "Version=" + sheet.value(row, "Version"));
		// Die erste Flagge eines Landes ist die normale und braucht keinen Hinweis. Jede weitere
		// schon — sonst weiß niemand, welche der beiden gerade gefragt ist.
		report("Version ab 2 -> Hinweistext gesetzt",
				row -> version(row) > 1 && !FlagSheet.isSet(sheet.value(row, "Hinweistext")),
				row -> "Hinweistext fehlt");
		checkUnique("Land und Version zusammen", row -> sheet.country(row) + " / Version " + version(row));
		System.out.println();
	}

	/**
	 * Ein Wert, den es im ganzen Blatt nur einmal geben darf. Zwei Zeilen für dasselbe Land sind
	 * erlaubt — zwei Zeilen mit derselben Version nicht, und zwei mit derselben Id erst recht nicht:
	 * Der Generator schriebe nur eine der beiden ins Deck.
	 */
	private void checkUnique(String was, java.util.function.Function<List<String>, String> key) {
		Set<String> seen = new LinkedHashSet<>();
		List<String> hits = new ArrayList<>();
		for (List<String> row : sheet.flags())
			if (!seen.add(key.apply(row)))
				hits.add(key.apply(row) + " steht zweimal im Blatt");
		problems += hits.size();
		System.out.printf("  %-62s %s%n", was + " eindeutig", hits.isEmpty() ? "ok" : hits.size() + " Abweichung(en)");
		for (String hit : hits)
			System.out.println("        " + hit);
	}

	/** Die wievielte Flagge dieses Landes. Leer und {@code x} heißen beide: die erste. */
	private int version(List<String> row) {
		String value = sheet.value(row, "Version");
		return FlagSheet.isSet(value) && value.matches("[1-9][0-9]*") ? Integer.parseInt(value) : 1;
	}

	private String sheetId(List<String> row) {
		return "ID " + sheet.number(row);
	}

	// ---- 4. Ausreißer ----------------------------------------------------------

	private void reportOutliers() {
		Map<List<String>, List<String>> groups = new LinkedHashMap<>();
		for (List<String> row : sheet.flags())
			groups.computeIfAbsent(sheet.attributes(row), key -> new ArrayList<>()).add(sheet.country(row));

		System.out.println("Einzelgänger mit genau einer Abweichung von einem Cluster (>=3) — oft echte Sonderfälle:");
		List<String> names = sheet.attributeNames();
		for (Map.Entry<List<String>, List<String>> single : groups.entrySet()) {
			if (single.getValue().size() != 1)
				continue;
			for (Map.Entry<List<String>, List<String>> cluster : groups.entrySet()) {
				if (cluster.getValue().size() < 3)
					continue;
				int differing = -1;
				int count = 0;
				for (int i = 0; i < names.size(); i++)
					if (!single.getKey().get(i).equals(cluster.getKey().get(i))) {
						differing = i;
						count++;
					}
				if (count == 1) {
					System.out.printf("     %-24s %-26s %-4s statt %-4s (Cluster von %d, z.B. %s)%n",
							single.getValue().get(0), names.get(differing), single.getKey().get(differing),
							cluster.getKey().get(differing), cluster.getValue().size(), cluster.getValue().get(0));
					break;
				}
			}
		}
		System.out.println();
	}

	// ---- Ausgabe ---------------------------------------------------------------

	private void report(String title, Predicate<List<String>> isWrong, java.util.function.Function<List<String>, String> detail) {
		List<String> hits = new ArrayList<>();
		for (List<String> row : sheet.flags())
			if (isWrong.test(row))
				hits.add(sheet.country(row) + " (" + detail.apply(row) + ")");
		problems += hits.size();
		System.out.printf("  %-62s %s%n", title, hits.isEmpty() ? "ok" : hits.size() + " Abweichung(en)");
		for (String hit : hits)
			System.out.println("        " + hit);
	}

	private static String orEmpty(String value) {
		return value.isEmpty() ? "leer" : value;
	}
}
