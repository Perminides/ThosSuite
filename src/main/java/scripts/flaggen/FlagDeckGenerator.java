package scripts.flaggen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.learn.anki.model.Card;

/**
 * Erzeugt aus dem Systematik-Blatt die Zeilen des Flaggen-Decks.
 *
 * <p>Kein Sonderweg in der Suite: Was hier herauskommt, ist eine ganz normale Deck-Zeile, die der
 * bestehende Parser liest. Die ganze Ableitung — welche Frage wann kommt, welcher Sketch dazu
 * gehört, wie groß ein Element gezeichnet wird — steckt <b>hier</b> und nicht im Anwendungscode.
 * Deshalb stehen die Regeln als Tabellen am Kopf der Klasse und nicht als {@code if} im Rumpf.</p>
 *
 * <p>Aufruf: {@code java scripts.flaggen.FlagDeckGenerator [--offline] [--trocken]}. Erzeugt werden
 * alle Zeilen mit {@code Generieren = 1}; die Deck-Datei entsteht dabei <b>vollständig neu</b> und
 * nach Id sortiert — was nicht aus dem Blatt kommt, überlebt den Lauf nicht. Handgeschriebene
 * Zusatzfragen gehören deshalb in die zweite Deck-Datei. Mit {@code --trocken} wird nur geprüft und
 * nichts geschrieben.</p>
 *
 * <p>Jede erzeugte Zeile läuft durch zwei Prüfungen: durch den echten {@link Card}-Parser, und
 * durch einen Flächenlauf, der mitzählt, wie viele Flächen die Skizze zu jedem Zeitpunkt hat —
 * ein {@code Mark} oder {@code Fill} auf eine Fläche, die es noch nicht gibt, fiele sonst erst
 * mitten in einer Lern-Session auf.</p>
 */
public class FlagDeckGenerator {

	private static final Path DATA = Path.of(System.getProperty("user.home"),
			"Documents", "Gedächtnis Lernen und so", "ThosSuite", "data");
	private static final Path DECK = DATA.resolve("decks").resolve("flaggenDeckJavaFX.csv");
	private static final Path SKETCHES = DATA.resolve("sketches");

	// ---- Antwortvokabulare ----------------------------------------------------

	private static final List<String> COLORS =
			List.of("Rot", "Blau", "Hellblau", "Grün", "Gelb", "Orange", "Weiß", "Schwarz");

	/** Index = Wert der Positionsspalte. Gefragt wird die Richtung vom Mittelpunkt, nicht ein Feld. */
	private static final List<String> POSITIONS = List.of(
			"Links oben vom Zentrum", "Oben vom Zentrum", "Rechts oben vom Zentrum",
			"Links vom Zentrum", "Zentriert", "Rechts vom Zentrum",
			"Links unten vom Zentrum", "Unten vom Zentrum", "Rechts unten vom Zentrum", "Verstreut");

	/** Index = Wert der Spalte „Dreieck von links?". Wert 0 heißt „kein Dreieck". */
	private static final List<String> DREIECK_FORMEN = List.of(
			"Nein",
			"Ja und zwar nur in der linken Hälfte",
			"Ja, aber es ist eher ein Trapez als ein Dreieck",
			"Ja und zwar bis zum rechten Rand",
			"Ja, aber dieses Dreiecksgebilde geht in eine waagerechte Spur bis zum rechten Rand über");

	/** Feste Anzeige-Reihenfolge der Dreiecksfrage: Werte 0, 1, 3, 2, 4. */
	private static final List<String> DREIECK_ANZEIGE = List.of(
			DREIECK_FORMEN.get(0), DREIECK_FORMEN.get(1), DREIECK_FORMEN.get(3),
			DREIECK_FORMEN.get(2), DREIECK_FORMEN.get(4));

	/** Index = Wert der Spalte Hintergrundtyp. Die 6 ist bewusst frei. */
	private static final Map<String, String> BACKGROUNDS = ordered(
			"0", "Waagerechte Streifen", "1", "Senkrechte Streifen", "2", "Kreuz mit vier Quadranten",
			"3", "Diagonale Teilung", "4", "Einfarbige Fläche",
			"5", "Senkrechtes Band mit waagerechten Streifen", "7", "Anderes");

	/** Die Optionen der Fill-Frage — ohne Kreuz und Diagonale, die vorweg geklärt sind. */
	private static final List<String> FILL_BACKGROUNDS = List.of(
			"Waagerechte Streifen", "Senkrechte Streifen", "Einfarbige Fläche",
			"Senkrechtes Band mit waagerechten Streifen", "Anderes");

	/**
	 * Der Pool der Streifenzahlen: 2 bis 9 als Bereich — die 8 kommt nie vor und ist ein reiner
	 * Ablenker, damit eine falsche Vorstellung ausdrückbar bleibt — dazu die großen, die es
	 * wirklich gibt (Liberia, die USA, Malaysia).
	 */
	private static final List<String> STRIPE_COUNTS =
			List.of("2", "3", "4", "5", "6", "7", "8", "9", "11", "13", "14");

	/**
	 * Die Breiten-Abfolgen der fünf waagerechten Streifen, von oben nach unten. Der Wert der Spalte
	 * {@code 5W} steht 1:1 als Antwort und im Sketch-Namen; die Zahlen sind das Verhältnis, schematisch,
	 * nicht maßstabsgetreu. Der Pool ist eindeutig — gleiche Abfolgen stehen nur einmal.
	 */
	private static final List<String> FIVE_WIDTHS = List.of(
			"3-1-2-1-3", "3-1-1-1-2", "1-1-2-1-1", "1-1-1-1-1", "1-2-3-2-1", "2-1-2-1-2", "2-1-3-1-2");

	/** Vorkommende Anzahlen — der Pool, aus dem die Ablenker der Anzahlfrage gezogen werden. */
	private static final List<String> COUNTS =
			List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "12", "15", "27", "50");

	/** Der Ablenkerpool der Elementfrage. {@link #ELEMENT_PINNED} steht davon immer sichtbar dabei. */
	private static final List<String> ELEMENT_POOL = List.of("Keine", "Stern", "Mond", "Sonne",
			"Kreis", "Vogel", "Emblem", "Kreuz", "Krone", "Landumriss");

	/** Immer sichtbare Ablenker der Elementfrage — außer sie sind selbst die richtige Antwort. */
	private static final List<String> ELEMENT_PINNED = List.of("Keine", "Stern");

	// ---- Sprache --------------------------------------------------------------

	/** Artikel und Mehrzahl je Elementnamen — nur fürs Fragestellen. */
	private static final Map<String, String[]> WORDS = orderedWords(
			new Object[][] {
				{"Stern", new String[] {"der Stern", "die Sterne"}},
				{"Mond", new String[] {"der Mond", "die Monde"}},
				{"Sonne", new String[] {"die Sonne", "die Sonnen"}},
				{"Kreis", new String[] {"der Kreis", "die Kreise"}},
				{"Raute", new String[] {"die Raute", "die Rauten"}},
				{"Schrift", new String[] {"die Schrift", "die Schriften"}},
				{"Vogel", new String[] {"der Vogel", "die Vögel"}},
				{"Emblem", new String[] {"das Emblem", "die Embleme"}},
				{"Kreuz", new String[] {"das Kreuz", "die Kreuze"}},
				{"Krone", new String[] {"die Krone", "die Kronen"}},
				{"Landumriss", new String[] {"der Landumriss", "die Landumrisse"}},
				{"Union Jack", new String[] {"der Union Jack", "die Union Jacks"}},
				{"Muster", new String[] {"das Muster", "die Muster"}},
				{"Drache", new String[] {"der Drache", "die Drachen"}},
				{"Gebäude", new String[] {"das Gebäude", "die Gebäude"}},
				{"Blatt", new String[] {"das Blatt", "die Blätter"}},
				{"Zweig", new String[] {"der Zweig", "die Zweige"}},
				{"Machete", new String[] {"die Machete", "die Macheten"}},
				{"Hacke", new String[] {"die Hacke", "die Hacken"}},
				{"Hand", new String[] {"die Hand", "die Hände"}},
				{"Zahnrad", new String[] {"das Zahnrad", "die Zahnräder"}},
				{"Dreizack", new String[] {"der Dreizack", "die Dreizacke"}},
				{"Nuss", new String[] {"die Nuss", "die Nüsse"}},
				{"Blume", new String[] {"die Blume", "die Blumen"}},
				{"Chakra", new String[] {"das Chakra", "die Chakras"}},
				{"Baum", new String[] {"der Baum", "die Bäume"}},
				{"Hut", new String[] {"der Hut", "die Hüte"}},
				{"Löwe", new String[] {"der Löwe", "die Löwen"}},
				{"Schwert", new String[] {"das Schwert", "die Schwerter"}},
			});

	// ---- Skizze ---------------------------------------------------------------

	/** Elementname → Datei. Der Stern hängt zusätzlich an der Anzahl, siehe {@link #sketchOf}. */
	private static final Map<String, String> ELEMENT_FILES = ordered(
			"Kreis", "kreis", "Raute", "raute", "Schrift", "schrift-t", "Mond", "sichel",
			"Hand", "hand", "Machete", "machete", "Zahnrad", "cog", "Emblem", "emblem",
			"Vogel", "vogel");

	/**
	 * Grundgröße einzelner Elemente, ohne Eintrag 1,0. Manche Figuren sind von Natur aus groß —
	 * Brasiliens Raute spannt fast die halbe Flagge, ein Stern tut das nie.
	 *
	 * <p>Sie wirkt auf das Element selbst <b>und</b> auf alles, was darin liegt: Wächst die Raute,
	 * wächst der Kreis darin mit, sonst verschöben sich die Verhältnisse.</p>
	 */
	private static final Map<String, Double> ELEMENT_SIZE = Map.of("Raute", 2d);

	/**
	 * Behälter und der Faktor, mit dem ihr Inhalt gezeichnet wird — je nach <b>Anzahl der Kinder</b>,
	 * denn mit jedem weiteren rückt der Inhalt nach außen.
	 *
	 * <p>Beim Kreis ist die Grenze ausrechenbar: Ein Element bringt seinen Kasten von 40 × 40 mit,
	 * und dessen äußere Ecke muss innerhalb des Radius bleiben. Bei einem Kind ist das die halbe
	 * Diagonale, {@code 40k/√2 ≤ 20}, also {@code k ≤ 0,707}. Bei mehreren kommt der Versatz dazu
	 * und die Grenzen sinken auf 0,52 · 0,48 · 0,49. Eingetragen ist jeweils etwas darunter.</p>
	 *
	 * <p>Die Raute ist nicht gerechnet, sondern am Bild gefunden — ihre Ecken laufen spitz zu, da
	 * gilt die Kastenregel nicht.</p>
	 */
	private static final Map<String, double[]> CONTAINERS = Map.of(
			"Raute", new double[] {0.7, 0.7, 0.7, 0.7},
			"Kreis", new double[] {0.7, 0.5, 0.45, 0.45});

	/**
	 * Geschwister im selben Feld: Größe und Mittelpunkte, je nach Anzahl. Keine Messung der
	 * einzelnen Datei — wir verlassen uns darauf, dass eine Figur in 40 × 40 passt.
	 *
	 * <p>Die Größe trägt zweierlei in einer Zahl: den Platz, den sich n Figuren im 60 breiten Feld
	 * teilen müssen, und die Luft zum Feldrand — eine Figur, die oben genau anstößt, sieht im Gösch
	 * schlecht aus. Ohne die Luft wären es 1,0 · 1,0 · 0,7 · 0,55. Dass beides in einer Zahl steht,
	 * heißt auch: Die Luft ist je Anzahl einstellbar.</p>
	 */
	private static final Map<Integer, double[]> SIBLINGS = Map.of(
			1, new double[] {0.8,  0},
			2, new double[] {0.8,  -10, 10},
			3, new double[] {0.56, -20, 0, 20},
			4, new double[] {0.44, -22.5, -7.5, 7.5, 22.5});

	private final FlagSheet sheet;
	private final Map<String, Integer> areas = new LinkedHashMap<>();
	/** Flaggenbilder, die es noch nicht gibt. Gemeldet am Ende, nicht abgebrochen. */
	private final List<String> missing = new ArrayList<>();
	/** {@code <ShuffleEnd>} gehoert an den Schritt NACH dem Block — er selbst liegt ausserhalb. */
	private String pending = "";

	private FlagDeckGenerator(FlagSheet sheet) {
		this.sheet = sheet;
	}

	public static void main(String[] args) throws IOException {
		List<String> options = List.of(args);
		FlagSheet sheet = options.contains("--offline") ? FlagSheet.read() : FlagSheet.fetch();
		FlagDeckGenerator generator = new FlagDeckGenerator(sheet);

		// Sortiert nach Id — in dieser Reihenfolge entsteht die Deck-Datei.
		Map<Integer, String> generated = new TreeMap<>();
		for (List<String> row : sheet.flags())
			if (sheet.value(row, "Generieren").equals("1"))
				generator.generate(generated, row);
		System.out.println(generated.size() + " Karten erzeugt und geprüft");
		generator.missing.forEach(file -> System.out.println("  ! Flaggenbild fehlt: " + file));

		if (options.contains("--trocken")) {
			generated.values().forEach(System.out::println);
			return;
		}
		write(generated);
	}

	/**
	 * Eine Zeile zur Karte machen, prüfen und einhängen. Jede Meldung bekommt hier ihr Land — ohne
	 * das steht da nur „für 'Löwe' fehlt der Artikel" und man sucht die Zeile in 214 anderen.
	 */
	private void generate(Map<Integer, String> generated, List<String> row) {
		int id = id(row);
		try {
			List<String> steps = card(row);
			check(steps);
			if (generated.put(id, String.join(";", steps)) != null)
				throw new RuntimeException("die Id " + id + " gibt es zweimal im Blatt");
		} catch (RuntimeException e) {
			throw new RuntimeException(sheet.country(row) + " (Id " + id + "): " + e.getMessage(), e);
		}
	}

	// ---- Die Karte ------------------------------------------------------------

	/** Die vollständige Zeile: Id, Bemerkung, Label, dann die Schritte. */
	private List<String> card(List<String> row) {
		List<String> steps = new ArrayList<>(List.of(String.valueOf(id(row)), remark(row), "Flagge",
				"Mark:" + shape(row)));

		// Der Hinweis trennt zwei Flaggen desselben Landes und muss deshalb vor der ersten Antwort
		// stehen. Er hängt an der ersten Frage statt an einem eigenen Schritt — das spart einen Klick.
		ask(steps, hint(row) + "Welche Form hat die Flagge?",
				answer(rectangular(row), "Rechteckig", "Nicht rechteckig", "Quadratisch"));
		ask(steps, "Hat die Flagge einen Rahmen?", answer(frame(row) ? "Ja" : "Nein", "Ja", "Nein"));

		// Kreuz und Diagonale vorweg — sonst würde ihr linker Arm mit einem Dreieck von links verwechselt.
		String type = sheet.value(row, "Hintergrundtyp");
		String vorweg = type.equals("2") ? "Kreuz" : type.equals("3") ? "Diagonale" : "Nein";
		ask(steps, "Teilt ein Kreuz oder eine Diagonale die Flagge, wenn Du Zusatzelemente und Rahmen ignorierst?",
				answer(vorweg, "Kreuz", "Diagonale", "Nein"));
		if (vorweg.equals("Nein"))
			ask(steps, "Entferne gedanklich eine Dreiecksstruktur von links, einen Gösch, alle "
					+ "Zusatzelemente und einen Rahmen. Was beschreibt nun den Hintergrund am besten?",
					answer(BACKGROUNDS.get(type), FILL_BACKGROUNDS.toArray(new String[0])));
		branchQuestions(steps, row, type);

		Canvas canvas = new Canvas(steps);
		List<Fill> fills = new ArrayList<>();
		String background = branchSketch(row, type);
		paint(background, fills, canvas.background(background), colors(sheet.value(row, "Hintergrundfarben")));

		// Gösch nach der Göschfrage auflegen (Leinwand-Silhouette, cell = -1).
		boolean goesch = sheet.value(row, "Gösch?").equals("1");
		ask(steps, "Hat die Flagge einen Gösch?", answer(goesch ? "Ja" : "Nein", "Ja", "Nein"));
		if (goesch)
			paint("goesch", fills, canvas.overlay("goesch", "-1"), colors(sheet.value(row, "Gösch Farbe")));

		// Dreieck nach der Dreieckfrage auflegen.
		String dreieck = sheet.value(row, "Dreieck von links?");
		int dreieckForm = FlagSheet.isSet(dreieck) ? Integer.parseInt(dreieck) : 0;
		ask(steps, "Schiebt sich eine dreiecksähnliche Form von ganz links in die Flagge?",
				fixedOrder(DREIECK_FORMEN.get(dreieckForm), DREIECK_ANZEIGE.toArray(new String[0])));
		if (dreieckForm != 0) {
			ask(steps, "Die Dreiecksform(en) bestehen aus wie vielen Farben?",
					fixedOrder(sheet.value(row, "Die Dreiecksform(en) bestehen aus wie vielen Farben?"),
							"1", "2", "3", "4"));
			String dreieckSketch = "dreieck-" + dreieckForm;
			paint(dreieckSketch, fills, canvas.overlay(dreieckSketch, "-1"),
					colors(sheet.value(row, "Dreieck Farbe")));
		}

		elementFills(steps, canvas, elements(row), fills);
		fillAreas(steps, fills);
		add(steps, "Image:" + image(row));
		add(steps, "Pause:"); // Zeit, die echte Flagge anzusehen
		return steps;
	}

	/** Die Folgefragen des Zweigs — je Hintergrundtyp die Spalten, die er nach sich zieht. */
	private void branchQuestions(List<String> steps, List<String> row, String type) {
		switch (type) {
			case "0" -> {
				ask(steps, "Wie viele waagerechte Streifen?",
						fixedOrder(sheet.value(row, "W-Streifen"), STRIPE_COUNTS.toArray(new String[0])));
				if (sheet.value(row, "W-Streifen").equals("3"))
					ask(steps, "Wie sind die Streifen verteilt?", coded(sheet.value(row, "3W"),
							"alle gleich breit", "mittlerer breiter", "mittlerer schmaler",
							"oberster breiter", "unterster breiter"));
				if (sheet.value(row, "W-Streifen").equals("5"))
					ask(steps, "Welche Abfolge beschreibt die Breite der Streifen von oben nach unten am besten?",
							answer(sheet.value(row, "5W"), FIVE_WIDTHS.toArray(new String[0])));
			}
			case "1" -> {
				ask(steps, "Wie viele senkrechte Streifen?",
						fixedOrder(sheet.value(row, "S-Streifen"), "2", "3", "4", "5"));
				ask(steps, "Wie sind sie verteilt?", coded(sheet.value(row, "S-Anordnung"),
						"gleichmäßig breit", "mittlerer breiter", "rechter breiter", "linker breiter"));
			}
			case "2" -> {
				ask(steps, "Welche Form hat das Kreuz?", coded(sheet.value(row, "Kreuzausrichtung"),
						"senkrecht", "diagonal", "beides"));
				ask(steps, "Welche Form haben die Arme?", coded(sheet.value(row, "Kreuzarme"),
						"uni", "drei parallele Farben", "fimbriert", "nicht sichtbar"));
			}
			case "3" -> {
				ask(steps, "Wie läuft die Diagonale?", coded(sheet.value(row, "Diagonal Richtung"),
						"steigend", "fallend", "strahlenförmig aus einer Ecke"));
				ask(steps, "Wie viele diagonale Bänder laufen durch?",
						coded(sheet.value(row, "Diagonal Anzahl Streifen"),
								"kein Band, die Flächen stoßen aneinander", "1", "2", "3", "4"));
			}
			case "5" -> ask(steps, "Wie viele waagerechte Streifen liegen neben dem Band?",
					fixedOrder(sheet.value(row, "SW Streifen"), "2", "3", "4", "5"));
			default -> { }
		}
	}

	// ---- Elemente -------------------------------------------------------------

	/**
	 * Ein Zusatzelement. {@code tolerated} sind Namen, die beim Anklicken durchgehen sollen, ohne
	 * richtig zu sein — Ägyptens Adler ist ein {@code Vogel}, wer ihn für ein {@code Emblem} hält,
	 * liegt nicht wirklich daneben. Sie stehen im Blatt als Klammer hinter dem Namen, genau wie bei
	 * den Ortsangaben: {@code Vogel (Emblem)}.
	 */
	private record Element(String name, List<String> tolerated, String position, String color,
			String count) {}

	private List<Element> elements(List<String> row) {
		List<Element> result = new ArrayList<>();
		for (int slot = 1; slot <= 4; slot++) {
			String name = sheet.value(row, "E" + slot);
			if (!FlagSheet.isSet(name))
				continue;
			String position = sheet.value(row, "E" + slot + " Position");
			if (position.equals("x"))
				continue; // Kein Ort, kein Sketch, keine Frage — Ort und Element stehen immer gemeinsam auf 'x'
			result.add(new Element(untolerated(name), bracket(name), position,
					sheet.value(row, "E" + slot + " Farbe"), sheet.value(row, "E" + slot + " Anzahl")));
		}
		return result;
	}

	/**
	 * Erst alle Elemente anhaken, dann je Element Anzahl und Ort, dann alles auf einmal zeichnen.
	 * Gefärbt wird nicht hier, sondern am Ende der Karte zusammen mit den Hintergrundflächen.
	 *
	 * <p>Die Fragen stehen in zwei Shuffle-Blöcken: erst alle Attribute (Anzahl, geteilt), dann alle
	 * Orte. Sonst verriete die Reihenfolge, welches Element im Blatt zuerst steht. Getrennt statt je
	 * Element beisammen, weil die Anzahl vor den Ort gehört (Numerus) — und so ist der ganze erste
	 * Block garantiert vor dem zweiten.</p>
	 *
	 * <p>Hängt die Füll-Paare der Elemente an {@code fills} an: je Elementfläche eines, sofern eine
	 * Farbe dasteht. Ein Element mit weniger Farben als Flächen (das ungefärbte Emblem) lässt seine
	 * überzähligen Flächen aus — sie bleiben grau —, rückt die Flächennummer aber trotzdem vor.</p>
	 */
	private void elementFills(List<String> steps, Canvas canvas, List<Element> elements, List<Fill> fills) {
		List<String> names = new ArrayList<>();
		for (Element element : elements)
			if (!names.contains(element.name()))
				names.add(element.name());
		add(steps, "Output:Welche Zusatzelemente siehst Du?");
		List<String> correct = names.isEmpty() ? List.of("Keine") : names;
		// Toleriert: falsch, aber ohne Abbruch. Wer Ägyptens Adler für ein Emblem hält, liegt nicht
		// wirklich daneben. Sie kommen aus der Klammer hinter dem Elementnamen.
		List<String> tolerated = new ArrayList<>();
		for (Element element : elements)
			for (String name : element.tolerated())
				if (!correct.contains(name) && !tolerated.contains(name))
					tolerated.add(name);

		// Ein Text darf nur einmal in der Frage stehen, sonst lehnt der MC-Parser sie ab. Deshalb
		// prüft jede Runde gegen alles bereits Vergebene.
		List<String> vergeben = new ArrayList<>(correct);
		vergeben.addAll(tolerated);
		List<String> options = new ArrayList<>();
		for (String name : correct)
			options.add("+" + name);
		for (String pinned : ELEMENT_PINNED)
			if (!vergeben.contains(pinned))
				options.add("-" + pinned); // immer sichtbar, außer schon anders vergeben
		for (String name : tolerated)
			options.add("~" + name);
		for (String pool : ELEMENT_POOL)
			if (!ELEMENT_PINNED.contains(pool) && !vergeben.contains(pool))
				options.add(pool);
		add(steps, "MC+:" + String.join("|", options));

		// Erst alle Attribut-Fragen (Anzahl, geteilt) in einem Shuffle, dann alle Ortsfragen in einem
		// zweiten. So steht die Anzahl immer vor dem Ort (Numerus), und in keinem der Blöcke verrät die
		// Reihenfolge, welches Element im Blatt zuerst steht. Ein Element ohne Attributfrage taucht im
		// ersten Block gar nicht auf — ein leeres Segment würde die Input-Prüfung reißen.
		List<Element> withAttribute = new ArrayList<>();
		for (Element element : elements)
			if (FlagSheet.isSet(element.count()) || element.name().equals("Kreis"))
				withAttribute.add(element);
		shuffled(steps, withAttribute, element -> {
			if (FlagSheet.isSet(element.count()))
				ask(steps, "Wie viele " + WORDS.get(element.name())[1].substring(4) + "?",
						fixedOrder(element.count(), COUNTS.toArray(new String[0])));
			// Bei jedem Kreis gleich gefragt — die konstante Frage leakt nichts und stoppt die stille
			// Annahme "ungeteilt". Geteilt ist er genau dann, wenn zwei Farben im Blatt stehen.
			if (element.name().equals("Kreis"))
				ask(steps, "Ist der Kreis geteilt?",
						answer(colors(element.color()).liste().size() == 2 ? "Ja" : "Nein", "Ja", "Nein"));
		});

		shuffled(steps, elements, element ->
				ask(steps, "Wo " + verb(element) + " " + word(element) + "?", position(element)));

		for (Layout layout : layout(elements))
			paint(layout.sketch(), fills, canvas.overlay(layout.sketch(), layout.placement()),
					colors(layout.element().color()));
	}

	/**
	 * Die Ortsfrage. Steht im Blatt eine Toleranzklammer, tragen deren Werte ein {@code ~}: Ein Klick
	 * darauf gilt als falsch, bricht die Karte aber nicht ab.
	 */
	private static String position(Element element) {
		List<String> tolerated = new ArrayList<>();
		for (String value : bracket(element.position()))
			tolerated.add(POSITIONS.get(Integer.parseInt(value)));
		return fixedOrder(POSITIONS.get(Integer.parseInt(untolerated(element.position()))), tolerated,
				POSITIONS.toArray(new String[0]));
	}

	/** Dateiname und Platzierung einer Figur: alles, was hinter {@code SketchImageAdd:} steht. */
	private record Layout(Element element, String sketch, String placement) {}

	/**
	 * Was gemeinsam gefragt und gefärbt wird: eine oder mehrere Flächen und ihre Farbe. Mehrere sind
	 * ein Element, dessen Farbe dem Ganzen gilt. Ungefüllte Flächen stehen gar nicht drin.
	 */
	private record Fill(List<Integer> areas, String color) {}

	/**
	 * Der Zeichenstapel. Wer eine Skizze auflegt, bekommt von hier die Nummern ihrer Flächen — sonst
	 * liefe neben der Zeichenreihenfolge ein Zähler her, den man an drei Stellen weiterdrehen müsste,
	 * und ein {@code Fill} auf die falsche Fläche sieht man erst mitten in einer Session.
	 *
	 * <p>Die Reihenfolge ist Hintergrund → Gösch → Dreieck → Elemente. Jede Fläche behält ihre eigene
	 * Nummer, auch wenn sie ungefärbt bleibt (das Emblem) — sonst verschöbe sie die folgenden.</p>
	 */
	private final class Canvas {

		private final List<String> steps;
		private int next;

		private Canvas(List<String> steps) {
			this.steps = steps;
		}

		/** Der Hintergrund. Er setzt die Leinwand zurück, gezählt wird wieder ab 0. */
		private List<Integer> background(String sketch) {
			add(steps, "SketchImage:" + sketch);
			next = 0;
			return claim(areasOf("backgrounds", sketch));
		}

		/** Eine Silhouette obendrauf. {@code placement} ist alles hinter dem Dateinamen. */
		private List<Integer> overlay(String sketch, String placement) {
			add(steps, "SketchImageAdd:" + sketch + "," + placement);
			return claim(areasOf("elements", sketch));
		}

		private List<Integer> claim(int count) {
			List<Integer> result = new ArrayList<>();
			for (int i = 0; i < count; i++)
				result.add(next++);
			return result;
		}
	}

	/**
	 * Ordnet Farben und Flächen einander zu, in Flächenreihenfolge. Wo keine Farbe steht, bleibt die
	 * Fläche grau und wird nicht gefragt — so das ungefärbte Emblem. Mehr Farben als Flächen ist
	 * dagegen immer ein Fehler im Blatt.
	 *
	 * <p>Trägt die Zelle ein {@code &}, gilt die eine Farbe dem ganzen Element: Alle seine Flächen
	 * werden gemeinsam hervorgehoben, einmal gefragt und gemeinsam gefüllt.</p>
	 */
	private static void paint(String sketch, List<Fill> fills, List<Integer> areas, Farben farben) {
		List<String> colors = farben.liste();
		if (farben.fuerAlleFlaechen()) {
			fills.add(new Fill(areas, colors.get(0)));
			return;
		}
		if (colors.size() > areas.size())
			throw new RuntimeException(sketch + ": " + colors.size() + " Farben ("
					+ String.join("|", colors) + "), aber nur " + areas.size() + " Flächen");
		for (int i = 0; i < colors.size(); i++)
			fills.add(new Fill(List.of(areas.get(i)), colors.get(i)));
	}

	/** Die Farben einer Zelle und ob sie dem ganzen Element gelten. */
	private record Farben(List<String> liste, boolean fuerAlleFlaechen) {}

	/**
	 * Die Farben einer Zelle. Leer und {@code x} heißen beide: keine Farbe.
	 *
	 * <p>Ein führendes {@code &} heißt: Die eine Farbe gilt dem <b>ganzen Element</b> und nicht
	 * seiner ersten Fläche. Das Emblem besteht aus zwei Flächen, die zusammen ein Wappen ergeben —
	 * gefragt wird es als Ganzes. Die Entscheidung trifft das Blatt, nicht der Generator; er sieht
	 * das Zeichen und gehorcht.</p>
	 */
	private static Farben colors(String cell) {
		if (!FlagSheet.isSet(cell))
			return new Farben(List.of(), false);
		boolean fuerAlle = cell.startsWith("&");
		List<String> liste = split(fuerAlle ? cell.substring(1) : cell);
		if (fuerAlle && liste.size() != 1)
			throw new RuntimeException("'&' meint eine Farbe fürs ganze Element, hier stehen "
					+ liste.size() + ": " + cell);
		return new Farben(liste, fuerAlle);
	}

	/**
	 * Größe und Versatz je Element. Ein Element ohne Behälter füllt sein Feld; was in einem Behälter
	 * liegt, erbt dessen Faktor. Geschwister werden nebeneinandergelegt und dabei zur Mitte hin
	 * zusammengeschoben — im Behälter stärker als im freien Feld.
	 */
	private List<Layout> layout(List<Element> elements) {
		List<Layout> result = new ArrayList<>();

		// Jedes Element haengt am letzten Behaelter davor, der im selben Feld liegt.
		int[] parent = new int[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			parent[i] = -1;
			for (int p = i - 1; p >= 0; p--)
				if (CONTAINERS.containsKey(elements.get(p).name())
						&& elements.get(p).position().equals(elements.get(i).position())) {
					parent[i] = p;
					break;
				}
		}

		// "Verfuegbar" ist der Faktor des Kastens, den sich die Geschwister teilen: 1 im Rasterfeld,
		// sonst die Kette der Behaelterfaktoren. Der Faktor eines Behaelters haengt daran, wie viele
		// Kinder er traegt -- deshalb erst zaehlen, dann rechnen.
		double[] available = new double[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			if (parent[i] < 0) {
				available[i] = ELEMENT_SIZE.getOrDefault(elements.get(i).name(), 1.0);
				continue;
			}
			int kinder = 0;
			for (int j = 0; j < elements.size(); j++)
				if (parent[j] == parent[i])
					kinder++;
			available[i] = available[parent[i]]
					* CONTAINERS.get(elements.get(parent[i]).name())[Math.min(kinder, 4) - 1];
		}

		for (int i = 0; i < elements.size(); i++) {
			Element element = elements.get(i);
			List<Integer> group = new ArrayList<>();
			for (int j = 0; j < elements.size(); j++)
				if (parent[j] == parent[i] && elements.get(j).position().equals(element.position()))
					group.add(j);
			double[] regel = SIBLINGS.get(Math.min(group.size(), 4));
			double size = available[i] * regel[0];
			double offset = regel[1 + group.indexOf(i)] * available[i];

			String placement = untolerated(element.position()) + "," + number(size);
			if (group.size() > 1)
				placement += "," + number(offset) + ",0";
			result.add(new Layout(element, sketchOf(element), placement));
		}
		return result;
	}

	/** Sterne haben drei Bilder: einer, zwei, mehr als zwei. Alles andere hat genau eins. */
	private static String sketchOf(Element element) {
		if (element.name().equals("Stern")) {
			int count = element.count().isEmpty() || element.count().equals("x")
					? 1 : Integer.parseInt(element.count());
			return count == 1 ? "stern" : count == 2 ? "stern-zwei" : "stern-haufen";
		}
		// Zwei Farben heißt: der Kreis ist geteilt — dann die zweiflächige Halbscheiben-Datei.
		if (element.name().equals("Kreis") && colors(element.color()).liste().size() == 2)
			return "geteilter-kreis";
		String file = ELEMENT_FILES.get(element.name());
		if (file == null)
			throw new RuntimeException("Für '" + element.name() + "' gibt es noch keine Elementdatei");
		return file;
	}

	// ---- Skizze und Farben ----------------------------------------------------

	/**
	 * Der reine Hintergrund-Dateiname aus den Attributen des Zweigs — wie {@code waagerecht-3}, nur mit
	 * Wörtern. Gösch und Dreieck stehen <b>nicht</b> im Namen: sie werden als Silhouetten aus
	 * {@code elements/} zur Laufzeit oben aufgelegt ({@code SketchImageAdd}), nicht eingebacken.
	 */
	private String branchSketch(List<String> row, String type) {
		return switch (type) {
			// Die Verteilung gehoert in den Namen: Sie veraendert die Breiten, und eine Skizze mit
			// gleichen Streifen widerspraeche der Antwort "Mitte breiter". Gefragt wird sie nur bei
			// drei und fuenf waagerechten Streifen -- sonst gibt es keinen Wert und keinen Zusatz.
			case "0" -> "waagerecht-" + sheet.value(row, "W-Streifen")
					+ (sheet.value(row, "W-Streifen").equals("3") ? "-" + sheet.value(row, "3W") : "")
					+ (sheet.value(row, "W-Streifen").equals("5") ? "-" + sheet.value(row, "5W") : "");
			case "1" -> "senkrecht-" + sheet.value(row, "S-Streifen") + "-" + sheet.value(row, "S-Anordnung");
			case "2" -> "kreuz-" + word(sheet.value(row, "Kreuzausrichtung"), "senkrecht", "diagonal", "beides")
					+ "-" + word(sheet.value(row, "Kreuzarme"), "uni", "dreifarbig", "fimbriert", "unsichtbar");
			case "3" -> "diagonal-" + word(sheet.value(row, "Diagonal Richtung"), "steigend", "fallend", "faecher")
					+ "-" + sheet.value(row, "Diagonal Anzahl Streifen");
			case "4" -> "uni";
			case "5" -> "sw-" + sheet.value(row, "SW Streifen");
			case "7" -> "spezial-" + sheet.value(row, "Spezial");
			default -> throw new RuntimeException("Kein Sketch-Name für Hintergrundtyp " + type);
		};
	}

	/**
	 * Je Fläche eine Farbfrage, in zufälliger Reihenfolge. Bei einer einzigen Fläche gibt es nichts
	 * zu mischen — dann bleibt die Klammer weg.
	 */
	private void fillAreas(List<String> steps, List<Fill> fills) {
		if (fills.isEmpty())
			return;
		add(steps, "Output:Welche Farbe hat die schraffierte Fläche?");
		shuffled(steps, fills, fill -> {
			add(steps, "SketchImageMark:" + areaList(fill));
			add(steps, answer(fill.color(), COLORS.toArray(new String[0])));
			add(steps, "SketchImageFill:" + areaList(fill) + "," + fill.color());
		});
	}

	/** Die Flächennummern einer Füllung, mit {@code |} getrennt: {@code 3|4}. */
	private static String areaList(Fill fill) {
		List<String> parts = new ArrayList<>();
		for (int area : fill.areas())
			parts.add(String.valueOf(area));
		return String.join("|", parts);
	}

	// ---- Ableitungen ----------------------------------------------------------

	/** Nepal ist weder das eine noch das andere, die Schweiz und der Vatikan sind quadratisch. */
	private String rectangular(List<String> row) {
		return switch (sheet.value(row, "Rechtwinklig?")) {
			case "1" -> "Rechteckig";
			case "2" -> "Quadratisch";
			default -> "Nicht rechteckig";
		};
	}

	private boolean frame(List<String> row) {
		return sheet.value(row, "Rahmen?").equals("1");
	}

	/**
	 * Die Fläche, die auf der Weltkarte markiert wird. Fast immer ist das der deutsche Name; nur wo
	 * die Karte ihn nicht kennt, steht in {@code ShapeId} eine Ausnahme (Abchasien, Singapur).
	 */
	private String shape(List<String> row) {
		String value = sheet.value(row, "ShapeId");
		return FlagSheet.isSet(value) ? value : sheet.country(row);
	}

	/**
	 * Die Karten-Id: die Spalte {@code ID}, unverändert. Sie wird von Hand vergeben und trägt den
	 * Lernfortschritt — der Generator rechnet nichts daran, damit sie sich nie verschiebt. Zwei
	 * Zeilen mit derselben Id fliegen in {@link #generate}.
	 */
	private int id(List<String> row) {
		return Integer.parseInt(sheet.number(row));
	}

	/** Die wievielte Flagge dieses Landes. Leer und {@code x} heißen beide: die erste. */
	private int version(List<String> row) {
		String value = sheet.value(row, "Version");
		return FlagSheet.isSet(value) ? Integer.parseInt(value) : 1;
	}

	/** Spalte 2 der Deck-Datei. Die Suite liest sie nicht — sie macht den {@code git diff} lesbar. */
	private String remark(List<String> row) {
		return sheet.country(row) + (version(row) > 1 ? " (" + version(row) + ")" : "");
	}

	/**
	 * Der Hinweis vor der ersten Frage, sonst leer. Er trennt zwei Flaggen desselben Landes
	 * („Flagge bis 2021"). Der Umbruch steht als {@code <br />} da: In eine Deck-Zelle passt kein
	 * echter Zeilenumbruch, das Fragefeld versteht aber {@code <br />}, {@code <b>} und {@code <i>}.
	 */
	private String hint(List<String> row) {
		String text = sheet.value(row, "Hinweistext");
		return FlagSheet.isSet(text) ? "<i>" + text + "</i><br />" : "";
	}

	/**
	 * Die echte Flagge als SVG, benannt nach dem deutschen Namen und ab der zweiten Flagge mit der
	 * Version dahinter: {@code Afghanistan2.svg}. Den Ordner kennt der Lader; dass die Datei da ist,
	 * prüfen wir trotzdem — sonst bliebe die Karte am Ende einfach leer.
	 */
	private String image(List<String> row) {
		String file = sheet.country(row) + (version(row) > 1 ? String.valueOf(version(row)) : "") + ".svg";
		if (!Files.exists(DATA.resolve("images").resolve("svg").resolve(file)))
			missing.add(file);
		return file;
	}

	// ---- Prüfung --------------------------------------------------------------

	/** Der echte Parser und ein Flächenlauf — beide finden anderes. */
	private void check(List<String> steps) {
		try {
			new Card(steps);
		} catch (RuntimeException e) {
			throw new RuntimeException("die erzeugte Zeile ist nicht lesbar", e);
		}
		int available = 0;
		Set<Integer> filled = new HashSet<>();
		for (String raw : steps) {
			String step = withoutMarkers(raw);
			if (step.startsWith("SketchImage:"))
				available = areasOf("backgrounds", step.substring("SketchImage:".length()));
			else if (step.startsWith("SketchImageAdd:"))
				available += areasOf("elements", step.substring("SketchImageAdd:".length()).split(",")[0]);
			else if (step.startsWith("SketchImageMark:") || step.startsWith("SketchImageFill:")) {
				for (String value : step.substring(step.indexOf(':') + 1).split(",")[0].split("\\|")) {
					int area = Integer.parseInt(value.trim());
					if (area >= available)
						throw new RuntimeException(step + ", aber es gibt erst " + available + " Flächen");
					if (step.startsWith("SketchImageFill:") && !filled.add(area))
						throw new RuntimeException("Fläche " + area + " wird doppelt gefüllt");
				}
			}
		}
	}

	/**
	 * Der Schritt ohne seine Shuffle-Marker, in derselben Reihenfolge abgeschnitten wie im Parser.
	 * Nicht bis zum letzten {@code >} springen: ein Fragetext darf {@code <br />} enthalten.
	 */
	private static String withoutMarkers(String raw) {
		String step = raw;
		for (String marker : List.of("<ShuffleStart>", "<ShuffleBreak>", "<ShuffleEnd>"))
			if (step.startsWith(marker))
				step = step.substring(marker.length());
		return step;
	}

	// ---- Dateien --------------------------------------------------------------

	/**
	 * Die Deck-Datei entsteht komplett neu, nach Id sortiert. Was nicht aus dem Blatt kommt, überlebt
	 * den Lauf nicht — handgeschriebene Zusatzfragen gehören in die zweite Deck-Datei. Deshalb steht
	 * hier auch, wie viele Karten vorher drin standen: Ein versehentlich zurückgesetztes
	 * {@code Generieren} soll auffallen und nicht still Karten löschen.
	 */
	private static void write(Map<Integer, String> generated) throws IOException {
		long before = Files.exists(DECK)
				? Files.readAllLines(DECK, StandardCharsets.UTF_8).stream().filter(line -> !line.isBlank()).count() - 1
				: 0;
		int width = generated.values().stream().mapToInt(line -> line.split(";", -1).length).max().orElse(3);
		List<String> header = new ArrayList<>(List.of("Index", "Bemerkung", "Label"));
		for (int i = 1; i <= width - 3; i++)
			header.add("Step" + i);

		List<String> lines = new ArrayList<>(List.of(String.join(";", header)));
		lines.addAll(generated.values());
		Files.writeString(DECK, "\uFEFF" + String.join("\r\n", lines) + "\r\n", StandardCharsets.UTF_8);
		System.out.println(generated.size() + " Karten geschrieben, vorher waren es " + before + " — " + DECK);
	}

	private int areasOf(String subfolder, String sketch) {
		return areas.computeIfAbsent(subfolder + "/" + sketch, key -> read(subfolder, sketch).size());
	}

	/** Die Flächen einer Strukturdatei — gezählt wird nur, wie viele es sind. */
	private static List<double[]> read(String subfolder, String sketch) {
		try {
			JsonNode root = new ObjectMapper().readTree(
					SKETCHES.resolve(subfolder).resolve(sketch + ".geojson").toFile());
			List<double[]> result = new ArrayList<>();
			for (JsonNode feature : root.get("features")) {
				JsonNode geometry = feature.get("geometry");
				if (geometry.get("type").asText().equals("Point")) {
					double x = geometry.get("coordinates").get(0).asDouble();
					double radius = feature.get("properties").get("radius").asDouble();
					result.add(new double[] {x - radius, x + radius});
					continue;
				}
				double min = Double.MAX_VALUE;
				double max = -Double.MAX_VALUE;
				for (JsonNode polygon : geometry.get("coordinates"))
					for (JsonNode ring : polygon)
						for (JsonNode point : ring) {
							min = Math.min(min, point.get(0).asDouble());
							max = Math.max(max, point.get(0).asDouble());
						}
				result.add(new double[] {min, max});
			}
			return result;
		} catch (IOException e) {
			throw new RuntimeException("Strukturdatei fehlt: "
					+ SKETCHES.resolve(subfolder).resolve(sketch + ".geojson"), e);
		}
	}

	// ---- Kleinkram ------------------------------------------------------------

	/**
	 * Ein Schritt, mit dem wartenden Shuffle-Marker davor.
	 *
	 * <p>Das Semikolon trennt die Spalten der Deck-Datei. Stünde eines in einem Fragetext, verschöben
	 * sich ab da alle Schritte der Karte — und zwar still, denn die Zeile bliebe lesbar.</p>
	 */
	private void add(List<String> steps, String step) {
		if (step.indexOf(';') >= 0)
			throw new RuntimeException("Ein Semikolon zerlegt die Deck-Zeile: " + step);
		steps.add(pending + step);
		pending = "";
	}

	/**
	 * Ein Shuffle-Block: je Eintrag ein Segment, gemischt wird die Reihenfolge der Segmente. Bei
	 * einem einzigen Eintrag gibt es nichts zu mischen — dann bleiben die Marker ganz weg.
	 */
	private <T> void shuffled(List<String> steps, List<T> items, Consumer<T> segment) {
		if (items.size() < 2) {
			items.forEach(segment);
			return;
		}
		// Ein wartendes <ShuffleEnd> kann nicht auf demselben Schritt stehen wie der neue Start: Der
		// Parser prüft auf Start, bevor er das End abschneidet. Also erst allein hinausschreiben.
		if (!pending.isEmpty())
			add(steps, "");
		for (int i = 0; i < items.size(); i++) {
			pending = i == 0 ? "<ShuffleStart>" : "<ShuffleBreak>";
			segment.accept(items.get(i));
		}
		pending = "<ShuffleEnd>";
	}

	private void ask(List<String> steps, String question, String mc) {
		add(steps, "Output:" + question);
		add(steps, mc);
	}

	/**
	 * Die richtige Antwort vor dem Sternchen, alle anderen dahinter.
	 *
	 * <p>Sie muss <b>buchstabengleich</b> unter den Optionen stehen — sonst bliebe sie in der
	 * Ablenkerliste stehen und stünde zweimal in der Frage, einmal als richtig und einmal als
	 * falsch. Das faellt beim Lesen der Zeile nicht auf, deshalb faellt es hier auf.</p>
	 */
	private static String answer(String correct, String... options) {
		List<String> wrong = new ArrayList<>(List.of(options));
		if (!wrong.remove(correct))
			throw new IllegalArgumentException("Die richtige Antwort '" + correct
					+ "' steht nicht unter ihren Optionen: " + String.join("|", wrong));
		return "MC:" + correct + "*" + String.join("|", wrong);
	}

	private static String fixedOrder(String correct, String... options) {
		return fixedOrder(correct, List.of(), options);
	}

	/**
	 * Wie {@link #answer}, aber in fester Reihenfolge: {@code =} hält die Optionen, wie sie hier
	 * stehen, die richtige trägt ihr {@code +}. Für Sätze mit natürlicher Ordnung — Richtungen, Zahlen.
	 *
	 * <p>Tolerierte Antworten tragen ein {@code ~}: Ein Klick darauf gilt als falsch, bricht die Karte
	 * aber nicht ab. Sie müssen genauso buchstabengleich unter den Optionen stehen wie die richtige.</p>
	 */
	private static String fixedOrder(String correct, List<String> tolerated, String... options) {
		List<String> parts = new ArrayList<>();
		boolean found = false;
		for (String option : options)
			if (option.equals(correct)) {
				parts.add("+" + option);
				found = true;
			} else if (tolerated.contains(option)) {
				parts.add("~" + option);
			} else {
				parts.add(option);
			}
		if (!found)
			throw new IllegalArgumentException("Die richtige Antwort '" + correct
					+ "' steht nicht unter ihren Optionen: " + String.join("|", options));
		for (String value : tolerated)
			if (!List.of(options).contains(value))
				throw new IllegalArgumentException("Die tolerierte Antwort '" + value
						+ "' steht nicht unter ihren Optionen: " + String.join("|", options));
		return "MC:=" + String.join("|", parts);
	}

	/**
	 * Eine kodierte Spalte: Der Wert ist der Index seiner Antwort. Die Ziffer darf niemals in die
	 * Frage geraten — sie steht im Blatt, weil dort alle Attribute Zahlen sind.
	 */
	private static String coded(String value, String... options) {
		return answer(word(value, options), options);
	}

	/** Ein Wert der Spalte, übersetzt in seinen Antworttext. */
	private static String word(String value, String... words) {
		return words[Integer.parseInt(value)];
	}

	private static String word(Element element) {
		String[] forms = WORDS.get(element.name());
		if (forms == null)
			throw new RuntimeException("Für '" + element.name() + "' fehlt der Artikel");
		return plural(element) ? forms[1] : forms[0];
	}

	private static String verb(Element element) {
		return plural(element) ? "liegen" : "liegt";
	}

	private static boolean plural(Element element) {
		return FlagSheet.isSet(element.count()) && !element.count().equals("1");
	}

	/** Alles vor einer Toleranzklammer: {@code 4(9|5)} ist die 4. */
	private static String untolerated(String value) {
		int open = value.indexOf('(');
		return open < 0 ? value.trim() : value.substring(0, open).trim();
	}

	/**
	 * Die Werte in der Toleranzklammer: {@code 4(9|5)} lässt die 9 und die 5 durchgehen. Getrennt wird
	 * wie überall im Blatt mit {@code |} — ein Komma ginge nicht, das trennt dort die Spalten.
	 */
	private static List<String> bracket(String value) {
		int open = value.indexOf('(');
		if (open < 0)
			return List.of();
		int close = value.lastIndexOf(')');
		if (close < open)
			throw new RuntimeException("Die Toleranzklammer ist nicht geschlossen: " + value);
		return split(value.substring(open + 1, close));
	}

	private static List<String> split(String value) {
		List<String> result = new ArrayList<>();
		for (String part : value.split("\\|"))
			if (!part.isBlank())
				result.add(part.trim());
		return result;
	}

	/** Zahlen kurz halten: 1 statt 1.0, 0.55 statt 0.5500000001. */
	private static String number(double value) {
		String text = String.format(java.util.Locale.ROOT, "%.3f", value);
		text = text.replaceAll("0+$", "");
		return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
	}

	private static Map<String, String> ordered(String... pairs) {
		Map<String, String> map = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
			map.put(pairs[i], pairs[i + 1]);
		return map;
	}

	private static Map<String, String[]> orderedWords(Object[][] pairs) {
		Map<String, String[]> map = new LinkedHashMap<>();
		for (Object[] pair : pairs)
			map.put((String) pair[0], (String[]) pair[1]);
		return map;
	}
}
