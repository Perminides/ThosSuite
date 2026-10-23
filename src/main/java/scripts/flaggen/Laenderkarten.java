package scripts.flaggen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Die Karten, die am Land hängen und nicht an der Flagge: Karte, Nachbarländer, Hauptstadt, größte
 * Städte. Dazu die Wortlisten, gegen die ihre Antworten geprüft werden.
 */
final class Laenderkarten {

	static final String LAENDER = "Länder";
	static final String STAEDTE = "Städte";
	static final String HAUPTSTADT = "Hauptstadt";
	static final String RANG_SPALTE = "Größte Städte";
	/** Städte, die nur in die Liste gehören und keine Karte bekommen — für Handkarten außerhalb der Ränge. */
	static final String WEITERE_STAEDTE = "Weitere Städte";

	/**
	 * Welche Spalten in welche Wortliste gehen, in dieser Reihenfolge. Eine neue Spalte, deren Input
	 * gegen eine Liste prüft, braucht hier ihren Eintrag — sonst kennt die Liste ihre Werte nicht.
	 */
	private static final List<Map.Entry<String, List<String>>> LISTEN_SPALTEN = List.of(
			Map.entry(STAEDTE, List.of(HAUPTSTADT, RANG_SPALTE, WEITERE_STAEDTE)));

	/** Wohin die Weltkarte bei der Nachbarfrage zentriert — eine Stelle ohne Land, sonst sähe man die Nachbarn. */
	private static final String KARTE_ABSEITS = "Mark:empty:808:2144";

	private static final List<String> RAENGE = List.of("größte", "zweitgrößte", "drittgrößte", "viertgrößte",
			"fünftgrößte", "sechstgrößte", "siebtgrößte", "achtgrößte", "neuntgrößte", "zehntgrößte",
			"elftgrößte", "zwölftgrößte", "dreizehntgrößte", "vierzehntgrößte", "fünfzehntgrößte",
			"sechzehntgrößte", "siebzehntgrößte", "achtzehntgrößte", "neunzehntgrößte", "zwanzigstgrößte");

	private final FlagSheet sheet;
	private final Nachbarlaender nachbarn;
	private final Map<String, List<List<String>>> wordLists = new LinkedHashMap<>();
	private final List<String> hinweise = new ArrayList<>();

	Laenderkarten(FlagSheet sheet) {
		this.sheet = sheet;
		this.nachbarn = new Nachbarlaender(sheet);
		if (!nachbarn.probleme().isEmpty())
			throw new RuntimeException("Spalte " + Nachbarlaender.SPALTE + " ist nicht in sich geschlossen:\n  "
					+ String.join("\n  ", nachbarn.probleme()));
		List<List<String>> laender = new ArrayList<>();
		for (String land : nachbarn.wortliste())
			laender.add(List.of(land));
		wordLists.put(LAENDER, laender);
		for (Map.Entry<String, List<String>> liste : LISTEN_SPALTEN)
			wordLists.put(liste.getKey(), sammle(liste.getValue()));
	}

	/** Alle Wortlisten, nach Namen — für den Parser beim Prüfen und für die Datei. */
	Map<String, List<List<String>>> wordLists() {
		return wordLists;
	}

	/** Was beim Sammeln zusammengelegt wurde und einen Blick wert ist — kein Fehler, nur ein Hinweis. */
	List<String> hinweise() {
		return hinweise;
	}

	// ---- Karten ---------------------------------------------------------------

	/**
	 * Die Gegenrichtung zur Flaggenkarte: die echte Flagge zeigen und das Land auf der Karte anklicken
	 * lassen. Zwei Schritte, mehr braucht sie nicht.
	 */
	void kartenkarte(Zeile z, Kartenbauer k) {
		k.add("Image:" + z.image());
		k.add("Click:" + z.shape());
	}

	/**
	 * Ein Land, dessen Nachbarn alle toleriert sind, bekommt keine Nachbarkarte — Collect braucht
	 * mindestens eine nötige Antwort. In der Wortliste steht es trotzdem.
	 */
	boolean hatPflichtNachbarn(Zeile z) {
		for (Nachbarlaender.Nachbar n : nachbarn.von(z.land()))
			if (!n.toleriert())
				return true;
		return false;
	}

	/**
	 * Die Nachbarländer: Flagge zeigen, Nachbarn sammeln. Die Karte steht dafür abseits, wo kein Land
	 * zu sehen ist ({@link #KARTE_ABSEITS}); erst im Abspann wird das Land markiert.
	 */
	void nachbarkarte(Zeile z, Kartenbauer k) {
		List<String> antworten = new ArrayList<>();
		for (Nachbarlaender.Nachbar n : nachbarn.von(z.land()))
			antworten.add((n.toleriert() ? "~" : "") + n.name());
		k.add(KARTE_ABSEITS);
		k.add("Image:" + z.image());
		k.add("Output:Welche <b>Nachbarländer</b> hat das Land?");
		k.add("Collect:" + LAENDER + ":" + String.join("|", antworten));
		k.add("<OnFail>Mark:" + z.shape());
		k.add("Pause:");
	}

	/**
	 * Ein einfaches Feld: Land markieren, Flagge zeigen, mit Namen fragen, gegen die Liste prüfen. Die
	 * Zelle ist der Input-Inhalt ({@code Pretoria,Tshwane|Kapstadt}), in die Karte kommt je Antwort die
	 * erste Schreibweise; jedes andere Wort der Liste ist falsch. {@code %s} in der Frage ist der
	 * Ländername. Einen Abspann gibt es nicht.
	 */
	static BiConsumer<Zeile, Kartenbauer> feld(String spalte, String frage, String liste) {
		return (z, k) -> {
			List<String> antworten = new ArrayList<>();
			for (String teil : z.value(spalte).split("\\|"))
				if (!teil.isBlank())
					antworten.add(ersteSchreibweise(teil));
			k.add("Mark:" + z.shape());
			k.add("Image:" + z.image());
			k.add("Output:" + frage.formatted(z.land()));
			k.add("Input:" + liste + ":" + String.join("|", antworten));
		};
	}

	/**
	 * Die größten Städte: ein Input je Rang, in Rangfolge. Steht vor einer Stadt {@code ~}, liegt sie
	 * knapp hinter der davor — bei der Frage nach der einen ist die andere toleriert, in beide Richtungen.
	 * Ein Fehler beendet die Karte wie bei der Flaggenkarte.
	 */
	void rangkarte(Zeile z, Kartenbauer k) {
		List<String> staedte = new ArrayList<>(), knapp = new ArrayList<>();
		for (String teil : z.value(RANG_SPALTE).split("\\|")) {
			String t = teil.trim();
			if (t.isEmpty() || t.startsWith("-"))
				throw new RuntimeException(RANG_SPALTE + ": leerer Rang oder '-' in " + z.value(RANG_SPALTE));
			knapp.add(t.startsWith("~") ? "~" : "");
			staedte.add(ersteSchreibweise(t.startsWith("~") ? t.substring(1) : t));
		}
		if (!knapp.get(0).isEmpty())
			throw new RuntimeException(RANG_SPALTE + ": die größte Stadt kann nicht knapp hinter einer anderen liegen");
		if (staedte.size() > RAENGE.size())
			throw new RuntimeException(RANG_SPALTE + ": mehr als " + RAENGE.size() + " Ränge");

		k.add("Mark:" + z.shape());
		k.add("Image:" + z.image());
		for (int i = 0; i < staedte.size(); i++) {
			String frage = i == 0
					? "<b>" + z.land() + "</b>: Welches ist die <b>größte</b> Stadt?"
					: "Und die <b>" + RAENGE.get(i) + "</b>?";
			String input = staedte.get(i);
			if (!knapp.get(i).isEmpty())
				input += "|~" + staedte.get(i - 1);
			if (i + 1 < staedte.size() && !knapp.get(i + 1).isEmpty())
				input += "|~" + staedte.get(i + 1);
			k.ask(frage, "Input:" + STAEDTE + ":" + input);
		}
	}

	// ---- Wortlisten -----------------------------------------------------------

	/**
	 * Eine Liste aus mehreren Spalten aller Länder. Der Schlüssel eines Eintrags ist seine
	 * <b>Hauptform</b>, die erste Schreibweise: Was mit derselben Hauptform beginnt, ist ein Eintrag,
	 * seine Schreibweisen werden vereinigt, die zuerst gesehenen vorn ({@code Kiew,Kyjiw} und
	 * {@code Kiew,Kyiv} ergeben {@code Kiew,Kyjiw,Kyiv}). Nebenschreibweisen dürfen mehrere Einträge
	 * teilen — Libyen {@code Tripolis}, Libanon {@code Tripoli,Tripolis}.
	 *
	 * <p>Vereinigt sich dabei dieselbe Hauptform aus verschiedenen Ländern mit verschiedenen
	 * Schreibweisen ({@code Valencia,València} in Spanien, {@code Valencia} in Venezuela), steht das als
	 * Hinweis in {@link #hinweise}: Die Karte des einen Landes nimmt dann auch die Schreibweisen des
	 * anderen an. Soll das nicht sein, braucht eine der beiden Städte eine eigene Hauptform.</p>
	 */
	private List<List<String>> sammle(List<String> spalten) {
		Map<String, List<String>> eintraege = new LinkedHashMap<>();             // Hauptform klein → Schreibweisen
		Map<String, Map<String, Set<String>>> herkunft = new HashMap<>();       // Hauptform klein → Land → Schreibweisen
		for (String spalte : spalten)
			for (List<String> row : sheet.countries()) {
				String zelle = sheet.value(row, spalte).trim();
				if (zelle.isEmpty())
					continue;
				if (!sheet.hauptzeile(row))
					throw new RuntimeException(sheet.country(row) + ": " + spalte + " nur in der Hauptzeile");
				if (zelle.contains(":") || zelle.contains("\\"))
					throw new RuntimeException(sheet.country(row) + ": " + spalte + " ohne ':' und '\\' — "
							+ "die Liste setzt der Generator selbst davor: " + zelle);
				for (String antwort : zelle.split("\\|")) {
					String a = antwort.trim();
					if (a.startsWith("-"))
						continue;   // ein Stoppwort von Hand ist kein Wert der Liste
					List<String> varianten = new ArrayList<>();
					for (String v : (a.startsWith("~") ? a.substring(1) : a).split(","))
						if (!v.isBlank())
							varianten.add(v.trim());
					if (varianten.isEmpty())
						continue;
					String schluessel = varianten.get(0).toLowerCase();
					List<String> eintrag = eintraege.computeIfAbsent(schluessel, k -> new ArrayList<>());
					for (String v : varianten)
						if (eintrag.stream().noneMatch(v::equalsIgnoreCase))
							eintrag.add(v);
					Set<String> klein = herkunft.computeIfAbsent(schluessel, k -> new LinkedHashMap<>())
							.computeIfAbsent(sheet.country(row), k -> new HashSet<>());
					for (String v : varianten)
						klein.add(v.toLowerCase());
				}
			}
		for (Map.Entry<String, Map<String, Set<String>>> e : herkunft.entrySet()) {
			Map<String, Set<String>> laender = e.getValue();
			if (laender.size() > 1 && new HashSet<>(laender.values()).size() > 1)
				hinweise.add(String.join(",", eintraege.get(e.getKey())) + ": Schreibweisen aus "
						+ String.join(" und ", laender.keySet()) + " vereinigt");
		}
		return new ArrayList<>(eintraege.values());
	}

	/**
	 * Eine Antwort der Zelle, wie sie in die Karte kommt: Rolle und erste Schreibweise. Die übrigen
	 * Schreibweisen holt der Parser aus der Liste.
	 */
	private static String ersteSchreibweise(String teil) {
		String t = teil.trim();
		String rolle = t.startsWith("~") || t.startsWith("-") ? t.substring(0, 1) : "";
		if (rolle.equals("-"))
			return t;   // ein Stoppwort von Hand steht in keiner Liste, es bleibt, wie es ist
		return rolle + t.substring(rolle.length()).split(",")[0].trim();
	}
}
