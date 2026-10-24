package app.learn.anki.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Die Wortlisten der Decks: je Liste ihre Einträge, jeder mit der Hauptform vorn und den weiteren
 * Schreibweisen dahinter.
 *
 * <p>Zwei Nutzer mit verschiedenen Fragen: Der {@code CardParser} löst beim Einlesen Hauptformen zu
 * Einträgen auf, der {@code CardProgress} fragt bei <b>jedem Tastendruck</b>, ob ein Wort zur Liste
 * gehört. Deshalb liegen neben den Einträgen zwei kleingeschriebene Register — sonst liefe jeder
 * Anschlag linear durch alle Einträge.</p>
 */
public final class WordLists {

	/** Listenname → Einträge, Hauptform zuerst. Die Reihenfolge ist die der Datei. */
	private final Map<String, List<List<String>>> entries;
	/** Listenname → Hauptform (klein) → Eintrag. */
	private final Map<String, Map<String, List<String>>> byMainForm = new HashMap<>();
	/** Listenname → alle Schreibweisen (klein), auch die der Nebenformen. */
	private final Map<String, Set<String>> spellings = new HashMap<>();

	public WordLists(Map<String, List<List<String>>> lists) {
		Map<String, List<List<String>>> copy = new LinkedHashMap<>();
		for (Map.Entry<String, List<List<String>>> list : lists.entrySet()) {
			List<List<String>> eintraege = List.copyOf(list.getValue());
			copy.put(list.getKey(), eintraege);
			Map<String, List<String>> haupt = new HashMap<>();
			Set<String> alle = new HashSet<>();
			for (List<String> eintrag : eintraege) {
				haupt.put(eintrag.get(0).toLowerCase(), eintrag);
				for (String schreibweise : eintrag)
					alle.add(schreibweise.toLowerCase());
			}
			byMainForm.put(list.getKey(), haupt);
			spellings.put(list.getKey(), alle);
		}
		entries = Map.copyOf(copy);
	}

	/** Leer — für Aufrufer, die ohne Listen auskommen; eine Karte mit Liste bricht dann ab. */
	public static WordLists leer() {
		return new WordLists(Map.of());
	}

	public boolean knowsList(String list) {
		return entries.containsKey(list);
	}

	public Set<String> listNames() {
		return entries.keySet();
	}

	public List<List<String>> entriesOf(String list) {
		return entries.getOrDefault(list, List.of());
	}

	/** Der Eintrag zu einer Hauptform, oder {@code null} — Groß- und Kleinschreibung zählt nicht. */
	public List<String> entryFor(String list, String mainForm) {
		Map<String, List<String>> haupt = byMainForm.get(list);
		return haupt == null ? null : haupt.get(mainForm.toLowerCase());
	}

	/** Ob das Wort irgendeine Schreibweise eines Eintrags dieser Liste ist. */
	public boolean contains(String list, String word) {
		Set<String> alle = spellings.get(list);
		return alle != null && alle.contains(word.toLowerCase());
	}

	/** Die Hauptformen einer Liste, in der Reihenfolge der Datei — die Vorschläge von Collect. */
	public List<String> mainForms(String list) {
		List<String> result = new ArrayList<>();
		for (List<String> eintrag : entriesOf(list))
			result.add(eintrag.get(0));
		return result;
	}
}
