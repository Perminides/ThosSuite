package scripts.flaggen;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Die Spalte {@code Nachbarländer}: {@code Namibia|Sambia|~Republik Kongo|…}, nur in der Hauptzeile
 * eines Landes. {@code ~} heißt toleriert — eine Grenze über Exklave, Überseegebiet oder Anspruch.
 *
 * <p>Generator und {@link FlagSheetCheck} lesen sie beide hierüber, damit die Regeln nur einmal
 * dastehen. Die wichtigste: Die Spalte ist in sich geschlossen. Jeder genannte Nachbar hat selbst
 * eine gefüllte Zeile und nennt das Land zurück, mit demselben {@code ~}. Erst dann ist die
 * Vereinigung aller Nennungen genau die Menge der gefüllten Zeilen — und die wird zur Wortliste.</p>
 */
final class Nachbarlaender {

	static final String SPALTE = "Nachbarländer";

	record Nachbar(String name, boolean toleriert) {}

	/** Land → seine Nachbarn, in der Reihenfolge der Zelle. Nur Länder mit gefüllter Zelle. */
	private final Map<String, List<Nachbar>> perLand = new LinkedHashMap<>();
	private final List<String> probleme = new ArrayList<>();

	Nachbarlaender(FlagSheet sheet) {
		Set<String> namen = new HashSet<>();
		for (List<String> row : sheet.countries())
			namen.add(sheet.country(row));

		for (List<String> row : sheet.countries()) {
			String land = sheet.country(row);
			String zelle = sheet.value(row, SPALTE).trim();
			if (zelle.isEmpty())
				continue;
			if (!sheet.hauptzeile(row)) {
				probleme.add(land + " (Version " + sheet.value(row, "Version") + "): Nachbarn nur in der Hauptzeile");
				continue;
			}
			List<Nachbar> nachbarn = new ArrayList<>();
			Set<String> gesehen = new HashSet<>();
			for (String teil : zelle.split("\\|")) {
				String t = teil.trim();
				boolean toleriert = t.startsWith("~");
				String name = toleriert ? t.substring(1).trim() : t;
				if (name.isEmpty())
					probleme.add(land + ": leerer Eintrag in '" + zelle + "'");
				else if (!namen.contains(name))
					probleme.add(land + ": '" + name + "' steht nicht in der Spalte Name");
				else if (name.equals(land))
					probleme.add(land + ": nennt sich selbst");
				else if (!gesehen.add(name))
					probleme.add(land + ": '" + name + "' doppelt");
				else
					nachbarn.add(new Nachbar(name, toleriert));
			}
			if (perLand.put(land, nachbarn) != null)
				probleme.add(land + ": mehrere Hauptzeilen mit Nachbarn");
		}

		// Geschlossen: jede Nennung hat eine gefüllte Zeile, die zurücknennt — mit demselben ~.
		for (Map.Entry<String, List<Nachbar>> land : perLand.entrySet())
			for (Nachbar n : land.getValue()) {
				List<Nachbar> gegenseite = perLand.get(n.name());
				if (gegenseite == null) {
					probleme.add(land.getKey() + " nennt " + n.name() + ", dessen Zeile ist leer");
					continue;
				}
				Nachbar zurueck = null;
				for (Nachbar g : gegenseite)
					if (g.name().equals(land.getKey()))
						zurueck = g;
				if (zurueck == null)
					probleme.add(land.getKey() + " nennt " + n.name() + ", aber nicht umgekehrt");
				else if (zurueck.toleriert() != n.toleriert() && land.getKey().compareTo(n.name()) < 0)
					probleme.add(land.getKey() + "–" + n.name() + ": ~ nur auf einer Seite");
			}

		// Ausdrücklich, auch wenn es aus dem Obigen folgt: Vereinigung = gefüllte Zeilen.
		Set<String> genannt = new TreeSet<>();
		for (List<Nachbar> nachbarn : perLand.values())
			for (Nachbar n : nachbarn)
				genannt.add(n.name());
		for (String land : perLand.keySet())
			if (!genannt.contains(land))
				probleme.add(land + " hat Nachbarn, wird aber selbst nirgends genannt");
		for (String name : genannt)
			if (!perLand.containsKey(name))
				probleme.add(name + " wird genannt, hat aber keine gefüllte Zeile");
	}

	List<String> probleme() {
		return probleme;
	}

	/** Die Nachbarn eines Landes; leer, wenn die Zelle leer ist. */
	List<Nachbar> von(String land) {
		return perLand.getOrDefault(land, List.of());
	}

	/** Die Wortliste: alle gefüllten Zeilen, alphabetisch — nach der Prüfung dieselben wie alle Nennungen. */
	List<String> wortliste() {
		List<String> liste = new ArrayList<>(perLand.keySet());
		liste.sort(Collator.getInstance(Locale.GERMAN));
		return liste;
	}
}
