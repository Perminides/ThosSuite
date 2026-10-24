package scripts.flaggen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Die beiden Dateien, die dem Generator gehören: die Deck-Datei und die generierten Wortlisten. */
final class DeckDatei {

	private static final Path DECK = FlagDeckGenerator.DATA.resolve("decks").resolve("flaggenDeckJavaFX.csv");
	/** Gehört dem Generator allein und entsteht jeden Lauf neu — Handgepflegtes steht in listen.csv. */
	private static final Path LISTEN = FlagDeckGenerator.DATA.resolve("decks").resolve("listen-generiert.csv");

	private DeckDatei() {}

	/**
	 * Die Deck-Datei entsteht komplett neu, nach Id sortiert. Was nicht aus dem Blatt kommt, überlebt
	 * den Lauf nicht — handgeschriebene Zusatzfragen gehören in die zweite Deck-Datei. Deshalb steht
	 * hier auch, wie viele Karten vorher drin standen: Eine versehentlich gelöschte Zeile im Blatt
	 * soll auffallen und nicht still Karten löschen.
	 */
	static void write(Map<Integer, String> generated) throws IOException {
		Map<Integer, String> before = readDeck();
		int width = generated.values().stream().mapToInt(line -> line.split(";", -1).length).max().orElse(3);
		List<String> header = new ArrayList<>(List.of("Index", "Bemerkung", "Label"));
		for (int i = 1; i <= width - 3; i++)
			header.add("Step" + i);

		List<String> neu = new ArrayList<>(), fort = new ArrayList<>(), anders = new ArrayList<>();
		for (Map.Entry<Integer, String> karte : generated.entrySet()) {
			String alt = before.get(karte.getKey());
			if (alt == null)
				neu.add(String.valueOf(karte.getKey()));
			else if (!alt.equals(karte.getValue()))
				anders.add(String.valueOf(karte.getKey()));
		}
		for (int id : before.keySet())
			if (!generated.containsKey(id))
				fort.add(String.valueOf(id));

		if (neu.isEmpty() && fort.isEmpty() && anders.isEmpty()) {
			System.out.println(generated.size() + " Karten, unveraendert \u2014 nichts geschrieben.");
			return;
		}
		// Gesichert wird nur, wenn etwas verloren gehen kann. Kamen bloss Karten dazu, steckt die
		// alte Fassung vollstaendig in der neuen \u2014 zum Zurueckgehen muesste man nur die neuen Ids
		// wegnehmen, und die stehen im Bericht darunter.
		Path sicherung = anders.isEmpty() && fort.isEmpty() ? null : sichere();

		List<String> lines = new ArrayList<>(List.of(String.join(";", header)));
		lines.addAll(generated.values());
		Files.writeString(DECK, "\uFEFF" + String.join("\r\n", lines) + "\r\n", StandardCharsets.UTF_8);

		System.out.println(generated.size() + " Karten geschrieben, vorher waren es " + before.size()
				+ (sicherung == null ? "" : "     Sicherung: " + sicherung.getFileName()));
		System.out.println("   neu         " + liste(neu));
		System.out.println("   entfallen   " + liste(fort));
		System.out.println("   geaendert   " + liste(anders));
	}

	/**
	 * Die Wortlisten des Generators — {@code Länder} aus den Nachbarn, dazu die Listen der Felder —, die
	 * ganze Datei neu, eine Zeile je Wert, je Liste alphabetisch nach der ersten Schreibweise. Unverändert
	 * bleibt sie liegen, damit ihr Zeitstempel nichts vortäuscht.
	 */
	static void writeWordLists(Map<String, List<List<String>>> listen) throws IOException {
		List<String> lines = new ArrayList<>(List.of("Liste;Hauptform;Alternativen"));
		StringBuilder bericht = new StringBuilder();
		Collator deutsch = Collator.getInstance(Locale.GERMAN);
		for (Map.Entry<String, List<List<String>>> liste : listen.entrySet()) {
			List<List<String>> sortiert = new ArrayList<>(liste.getValue());
			sortiert.sort((a, b) -> deutsch.compare(a.get(0), b.get(0)));
			for (List<String> eintrag : sortiert)
				lines.add(liste.getKey() + ";" + eintrag.get(0) + ";"
						+ String.join(",", eintrag.subList(1, eintrag.size())));
			bericht.append(" ").append(liste.getKey()).append(" ").append(liste.getValue().size());
		}
		String inhalt = "\uFEFF" + String.join("\r\n", lines) + "\r\n";
		if (Files.exists(LISTEN) && Files.readString(LISTEN, StandardCharsets.UTF_8).equals(inhalt)) {
			System.out.println("Wortlisten unveraendert:" + bericht);
			return;
		}
		Files.writeString(LISTEN, inhalt, StandardCharsets.UTF_8);
		System.out.println("Wortlisten geschrieben:" + bericht);
	}

	/** Die Karten der bestehenden Deck-Datei, nach Id. Leer, wenn es sie noch nicht gibt. */
	private static Map<Integer, String> readDeck() throws IOException {
		Map<Integer, String> result = new LinkedHashMap<>();
		if (!Files.exists(DECK))
			return result;
		boolean kopfzeile = true;
		for (String line : Files.readAllLines(DECK, StandardCharsets.UTF_8)) {
			line = line.replace("\uFEFF", "");
			if (line.isBlank())
				continue;
			if (kopfzeile) {                    // die Kopfzeile allein ist keine Aenderung
				kopfzeile = false;
				continue;
			}
			result.put(Integer.parseInt(line.substring(0, line.indexOf(';'))), line);
		}
		return result;
	}

	/**
	 * Legt die bestehende Deck-Datei als hochgezaehlte Kopie ab und liefert deren Pfad.
	 *
	 * <p>Aufgeraeumt wird nie. Weil nur bei einem echten Eingriff gesichert wird, ist die Reihe der
	 * Dateien eine Liste dieser Eingriffe und kein Protokoll jedes Laufs.</p>
	 */
	private static Path sichere() throws IOException {
		Path ordner = DECK.getParent().resolve("backups");
		Files.createDirectories(ordner);
		String stamm = DECK.getFileName().toString().replace(".csv", "");
		int naechste = 1;
		try (var vorhandene = Files.list(ordner)) {
			for (Path datei : vorhandene.toList()) {
				String name = datei.getFileName().toString();
				if (!name.startsWith(stamm + "-") || !name.endsWith(".csv"))
					continue;
				// Nur reine Ziffern zaehlen mit: In dem Ordner darf auch etwas von Hand liegen.
				String nummer = name.substring(stamm.length() + 1, name.length() - 4);
				if (!nummer.isEmpty() && nummer.chars().allMatch(Character::isDigit))
					naechste = Math.max(naechste, Integer.parseInt(nummer) + 1);
			}
		}
		Path ziel = ordner.resolve(String.format("%s-%04d.csv", stamm, naechste));
		Files.copy(DECK, ziel);
		return ziel;
	}

	/** Eine Id-Liste fuer den Bericht, nach zwanzig abgeschnitten. */
	private static String liste(List<String> ids) {
		if (ids.isEmpty())
			return "0";
		String gezeigt = String.join(", ", ids.subList(0, Math.min(20, ids.size())));
		return ids.size() + "     " + gezeigt + (ids.size() > 20 ? ", \u2026" : "");
	}
}
