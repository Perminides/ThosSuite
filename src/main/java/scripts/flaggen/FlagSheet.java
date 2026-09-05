package scripts.flaggen;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Der gemeinsame Zugriff auf das Flaggen-Blatt für alles, was daneben liegt.
 *
 * <p>Ein Blatt, eine Zeile pro Flagge: <i>Systematik</i>. Links die Hintergrund-Attribute, rechts
 * die Zusatzelemente. Es wird als CSV exportiert und neben den Skripten abgelegt; die eingecheckte
 * Datei ist die Schnittstelle zum Generator und die Stelle, an der {@code git diff} eine
 * Datenänderung zeigt.</p>
 *
 * <p><b>Nichts wird über Spaltenpositionen gelesen.</b> Der Kopf ist die Zeile, die irgendwo
 * "Signatur" trägt; wo der Attributblock anfängt, wird gesucht — es ist die Stelle, ab der sich die
 * Signatur aus den folgenden Spalten wieder zusammensetzen lässt. Alles andere hängt an seiner
 * Überschrift. Damit brechen eingefügte, verschobene oder gelöschte Spalten nichts. Genau das ist
 * beim Aufräumen zweimal passiert.</p>
 *
 * <p>Beim Auflösen über Überschriften: Ein Blatt kann eine Überschrift doppelt tragen (früher
 * standen "3W" und "5W" zweimal drin, einmal im Attributblock und einmal in alten Arbeitsspalten).
 * Deshalb liefert {@link #value} für Attribute die Position aus dem gefundenen Block und nicht die
 * letzte gleichnamige Spalte.</p>
 */
public class FlagSheet {

	private static final String SHEET = "1FX8SgpOr9G_Ss030KkQDAtOUE3AbEHuMPfxpl3PBQdE";
	private static final int GID = 184257391;   // Systematik. Eine Id, keine Position — Verschieben ändert sie nicht.
	private static final Path FILE = Path.of("docs", "flaggen", "systematik.csv");

	private final List<List<String>> rows;
	private final List<List<String>> flags = new ArrayList<>();
	private final List<String> withoutSignature = new ArrayList<>();
	private final int headerRow;
	private final int signatureColumn;
	private final int numberColumn;
	private final int nameColumn;
	private final int attributeStart;
	private final int attributeCount;
	private final List<String> attributeNames = new ArrayList<>();
	private final Map<String, Integer> otherColumns = new LinkedHashMap<>();

	/** Holt das Blatt und schreibt es neben die Skripte. */
	public static FlagSheet fetch() {
		String url = "https://docs.google.com/spreadsheets/d/" + SHEET + "/export?format=csv&gid=" + GID;
		try {
			HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
			HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
			HttpResponse<String> response = client.send(request,
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() != 200)
				throw new RuntimeException("Das Blatt antwortet mit " + response.statusCode() + ": " + url);
			Files.writeString(FILE, response.body(), StandardCharsets.UTF_8);
			return read();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Das Blatt ließ sich nicht holen: " + url, e);
		}
	}

	/** Liest die vorhandene Datei, ohne zu holen. */
	public static FlagSheet read() {
		try {
			return new FlagSheet(Files.readString(FILE, StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException("Keine " + FILE.toAbsolutePath() + " — erst holen", e);
		}
	}

	private FlagSheet(String csv) {
		rows = parse(csv);
		headerRow = findHeaderRow();
		signatureColumn = indexOf(rows.get(headerRow), "Signatur");
		// Signatur, ID und Name werden schon beim Sammeln der Datenzeilen gebraucht, also vor dem
		// Attributblock. Aufgelöst werden sie trotzdem über die Überschrift, nicht über die Position.
		numberColumn = column("Land-ID");
		nameColumn = column("Name");

		// Datenzeile = Name und Signatur. Ein Bildpfad taugt NICHT als Kriterium: Neu angelegte
		// Länder haben noch keinen und wären still verschwunden statt aufzufallen.
		for (List<String> row : rows.subList(headerRow + 1, rows.size())) {
			if (cell(row, nameColumn).isEmpty())
				continue;
			if (cell(row, signatureColumn).isEmpty())
				withoutSignature.add(cell(row, nameColumn));
			else
				flags.add(row);
		}
		if (flags.isEmpty())
			throw new RuntimeException("Keine Datenzeilen — ist der richtige Tab geladen?");

		attributeCount = signature(flags.get(0)).split("\\|").length;
		attributeStart = findAttributeBlock();
		List<String> header = rows.get(headerRow);
		for (int i = attributeStart; i < attributeStart + attributeCount; i++)
			attributeNames.add(cell(header, i));
		for (int i = 0; i < header.size(); i++) {
			boolean inBlock = i >= attributeStart && i < attributeStart + attributeCount;
			String name = cell(header, i);
			if (!name.isEmpty() && !inBlock)
				otherColumns.put(name, i);
		}
	}

	/**
	 * Zerlegt die Datei. <b>Kein quotefähiger Leser</b>: Das Blatt wird bewusst ohne Kommas in den
	 * Zellen geführt, damit hier ein {@code split} reicht. Taucht doch eines auf, fliegt es — sonst
	 * verschöben sich ab dieser Zeile still alle Spalten.
	 */
	private static List<List<String>> parse(String csv) {
		List<List<String>> result = new ArrayList<>();
		for (String line : csv.replace("\r\n", "\n").split("\n")) {
			if (line.indexOf('"') >= 0)
				throw new RuntimeException("Anführungszeichen in der Datei — eine Zelle enthält ein Komma: " + line);
			result.add(List.of(line.split(",", -1)));
		}
		return result;
	}

	/** Die Spalte mit dieser Überschrift. Fehlt sie, ist das Blatt umbenannt worden — das fliegt. */
	private int column(String name) {
		int index = indexOf(rows.get(headerRow), name);
		if (index < 0)
			throw new RuntimeException("Die Spalte '" + name + "' gibt es im Blatt nicht: "
					+ String.join(", ", rows.get(headerRow)));
		return index;
	}

	private int findHeaderRow() {
		for (int i = 0; i < rows.size(); i++)
			if (indexOf(rows.get(i), "Signatur") >= 0)
				return i;
		throw new RuntimeException("Keine Kopfzeile gefunden — keine Zelle heißt 'Signatur'");
	}

	/** Die Spalte, ab der sich die Signatur aus den folgenden n Spalten ergibt. */
	private int findAttributeBlock() {
		List<List<String>> sample = flags.subList(0, Math.min(20, flags.size()));
		for (int start = 0; start + attributeCount <= rows.get(headerRow).size(); start++) {
			boolean fits = true;
			for (List<String> row : sample) {
				if (row.size() < start + attributeCount)
					continue;
				StringBuilder built = new StringBuilder();
				for (int i = start; i < start + attributeCount; i++)
					built.append(i > start ? "|" : "").append(cell(row, i));
				if (!signature(row).equals(built.toString())) {
					fits = false;
					break;
				}
			}
			if (fits)
				return start;
		}
		throw new RuntimeException("Attributblock nicht gefunden — passt die Signatur nicht mehr zu ihren Spalten?");
	}

	/**
	 * Der Wert einer Spalte, über ihre Überschrift. Attribute stechen gleichnamige Altspalten.
	 *
	 * <p>Ein unbekannter Name fliegt. Er ist fast immer eine umbenannte Spalte, und ein still
	 * geliefertes "" ließe jede Regel darauf als "nicht gesetzt" durchgehen.</p>
	 */
	public String value(List<String> row, String column) {
		int index = attributeNames.indexOf(column);
		if (index >= 0)
			return cell(row, attributeStart + index);
		Integer other = otherColumns.get(column);
		if (other == null)
			throw new IllegalArgumentException("Spalte '" + column + "' gibt es im Blatt nicht. Vorhanden: "
					+ String.join(", ", attributeNames) + ", " + String.join(", ", otherColumns.keySet()));
		return cell(row, other);
	}

	/** Gesetzt heißt: nicht leer und nicht 'x'. Beides bedeutet "hier wird nicht gefragt". */
	public static boolean isSet(String value) {
		return !value.isEmpty() && !value.equals("x");
	}

	public List<List<String>> flags()      { return flags; }
	public List<String> withoutSignature() { return withoutSignature; }
	public List<String> attributeNames()   { return attributeNames; }

	public String number(List<String> row)    { return cell(row, numberColumn); }
	public String country(List<String> row)   { return cell(row, nameColumn); }
	public String signature(List<String> row) { return cell(row, signatureColumn); }

	public List<String> attributes(List<String> row) {
		List<String> result = new ArrayList<>();
		for (int i = attributeStart; i < attributeStart + attributeCount; i++)
			result.add(cell(row, i));
		return result;
	}

	private static String cell(List<String> row, int index) {
		return index >= 0 && index < row.size() ? row.get(index).trim() : "";
	}

	private static int indexOf(List<String> row, String value) {
		for (int i = 0; i < row.size(); i++)
			if (row.get(i).trim().equals(value))
				return i;
		return -1;
	}

	/** Jede Zeile mit allen über die Überschrift aufgelösten Werten — zum Vergleichen. */
	private void dump() {
		List<String> columns = new ArrayList<>(attributeNames);
		columns.addAll(otherColumns.keySet());
		for (List<String> row : flags) {
			StringBuilder line = new StringBuilder(number(row) + ";" + country(row) + ";" + signature(row));
			for (String column : columns)
				line.append(";").append(column).append("=").append(value(row, column));
			System.out.println(line);
		}
	}

	/**
	 * Holt das Blatt. Mit {@code --offline} wird nur die vorhandene Datei gelesen, mit
	 * {@code --dump} jede Zeile ausgeschrieben.
	 */
	public static void main(String[] args) {
		List<String> options = List.of(args);
		FlagSheet sheet = options.contains("--offline") || options.contains("--dump") ? read() : fetch();
		if (options.contains("--dump")) {
			sheet.dump();
			return;
		}
		System.out.printf("%s: %d Flaggen, %d Attribute (Kopfzeile %d, Attributblock ab Spalte %d)%n",
				FILE, sheet.flags.size(), sheet.attributeCount, sheet.headerRow, sheet.attributeStart);
		if (!sheet.withoutSignature.isEmpty())
			System.out.println("  ! ohne Signatur: " + String.join(", ", sheet.withoutSignature));
	}
}
