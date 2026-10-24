package app.learn.anki.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.learn.anki.model.WordLists;
import app.shared.Config;

/**
 * Die Wortlisten, aus denen ein Collect-Step seine Vorschläge holt. Sie liegen neben den Decks,
 * eine Datei je Schreiber: {@code listen.csv} pflegt der Mensch, {@code listen-generiert.csv} schreibt
 * der Flaggen-Generator bei jedem Lauf neu. Fehlt eine Datei, ist sie leer.
 *
 * <p>Eine Zeile je Wert: {@code <liste>;<hauptform>,<schreibweise>,…}, Kopfzeile wie bei den Decks. Die
 * Hauptform ist der Schlüssel und in ihrer Liste eindeutig; weitere Schreibweisen dürfen bei mehreren
 * Einträgen stehen (Libyen {@code Tripolis}, Libanon {@code Tripoli,Tripolis}).
 * Eine Liste gehört genau einer Datei — derselbe Name in beiden bricht ab, statt dass eine die andere
 * still überschreibt.</p>
 */
class CsvWordListSource {

	// Wie beim Deck-Dateinamen: eine mit ';' getrennte Sammlung von Dateien.
	private static final String FILES = "listen.csv;listen-generiert.csv";

	/** Listenname → Einträge, je Eintrag seine Schreibweisen in Dateireihenfolge. */
	WordLists loadAll() {
		Path folder = Config.getPath("deckFolder");
		Map<String, List<List<String>>> lists = new LinkedHashMap<>();
		Map<String, String> fileOfList = new HashMap<>();
		for (String name : FILES.split(";")) {
			Path file = folder.resolve(name.trim());
			if (!Files.exists(file))
				continue;
			for (Map.Entry<String, List<List<String>>> list : read(file).entrySet()) {
				String other = fileOfList.putIfAbsent(list.getKey(), name);
				if (other != null)
					throw new RuntimeException("Liste '" + list.getKey() + "' steht in " + other + " und in " + name);
				lists.put(list.getKey(), List.copyOf(list.getValue()));
			}
		}
		return new WordLists(lists);
	}

	private Map<String, List<List<String>>> read(Path file) {
		try {
			List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
			Map<String, List<List<String>>> lists = new LinkedHashMap<>();
			Map<String, Set<String>> seen = new HashMap<>();
			for (String line : lines.subList(Math.min(1, lines.size()), lines.size())) { // Kopfzeile überspringen
				if (line.isBlank())
					continue;
				String[] cells = line.split(";", -1);
				if (cells.length < 2 || cells[0].isBlank())
					throw new RuntimeException("Zeile ohne Liste oder Wert in " + file.getFileName() + ": " + line);
				// Drei Spalten: Liste, Hauptform, Alternativen. Die Hauptform steht allein, damit man
				// ihr ansieht, dass sie der Schlüssel ist — in der Karte steht nur sie.
				List<String> variants = new ArrayList<>(CardParser.parseVariants(cells[1]));
				if (variants.size() > 1)
					throw new RuntimeException("Die Hauptform steht allein in der zweiten Spalte, Alternativen in die"
							+ " dritte: " + file.getFileName() + ": " + line);
				if (cells.length > 2)
					variants.addAll(CardParser.parseVariants(cells[2]));
				if (variants.isEmpty())
					throw new RuntimeException("Zeile ohne Wert in " + file.getFileName() + ": " + line);
				String list = cells[0].trim();
				// Die Hauptform ist der Schlüssel des Eintrags, die übrigen dürfen auch anderswo stehen.
				if (!seen.computeIfAbsent(list, _ -> new HashSet<>()).add(variants.get(0).toLowerCase()))
					throw new RuntimeException("'" + variants.get(0) + "' steht doppelt in Liste '" + list + "', " + file.getFileName());
				lists.computeIfAbsent(list, _ -> new ArrayList<>()).add(variants);
			}
			return lists;
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Lesen von " + file, e);
		}
	}
}
