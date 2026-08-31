package scripts.flaggen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * alle Zeilen mit {@code Generieren = 1}; vorhandene Karten derselben Id werden ersetzt, alle
 * anderen Zeilen der Datei bleiben stehen. Mit {@code --trocken} wird nur geprüft und nichts
 * geschrieben.</p>
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

	/** Index = Wert der Spalte Hintergrundtyp. Die 6 ist bewusst frei. */
	private static final Map<String, String> BACKGROUNDS = ordered(
			"0", "Waagerechte Streifen", "1", "Senkrechte Streifen", "2", "Kreuz mit vier Quadranten",
			"3", "Diagonale Teilung", "4", "Einfarbige Fläche",
			"5", "Senkrechtes Band mit waagerechten Streifen", "7", "Anderes");

	/**
	 * Der Pool der Streifenzahlen: 2 bis 9 als Bereich — die 8 kommt nie vor und ist ein reiner
	 * Ablenker, damit eine falsche Vorstellung ausdrückbar bleibt — dazu die großen, die es
	 * wirklich gibt (Liberia, die USA, Malaysia).
	 */
	private static final List<String> STRIPE_COUNTS =
			List.of("2", "3", "4", "5", "6", "7", "8", "9", "11", "13", "14");

	/** Vorkommende Anzahlen — der Pool, aus dem die Ablenker der Anzahlfrage gezogen werden. */
	private static final List<String> COUNTS =
			List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "12", "15", "27", "50");

	/** Die Ablenker der Elementfrage, in fester Reihenfolge. {@code Keine} steht immer dabei. */
	private static final List<String> ELEMENT_POOL = List.of("Keine", "Stern", "Mond", "Sonne",
			"Kreis", "Vogel", "Emblem", "Kreuz", "Krone", "Landumriss");

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
			"Hand", "hand", "Machete", "machete", "Zahnrad", "cog");

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
	/** {@code <ShuffleEnd>} gehoert an den Schritt NACH dem Block — er selbst liegt ausserhalb. */
	private String pending = "";

	private FlagDeckGenerator(FlagSheet sheet) {
		this.sheet = sheet;
	}

	public static void main(String[] args) throws IOException {
		List<String> options = List.of(args);
		FlagSheet sheet = options.contains("--offline") ? FlagSheet.read() : FlagSheet.fetch();
		FlagDeckGenerator generator = new FlagDeckGenerator(sheet);

		Map<String, String> generated = new LinkedHashMap<>();
		for (List<String> row : sheet.flags())
			if (sheet.value(row, "Generieren").equals("1")) {
				List<String> steps = generator.card(row);
				generator.check(sheet.country(row), steps);
				generated.put(sheet.number(row), String.join(";", steps));
			}
		System.out.println(generated.size() + " Karten erzeugt und geprüft");

		if (options.contains("--trocken")) {
			generated.values().forEach(System.out::println);
			return;
		}
		write(generated);
	}

	// ---- Die Karte ------------------------------------------------------------

	/** Die vollständige Zeile: Id, Bemerkung, Label, dann die Schritte. */
	private List<String> card(List<String> row) {
		List<String> steps = new ArrayList<>(List.of(sheet.number(row), "", "Flagge",
				"Mark:" + sheet.value(row, "ID")));

		ask(steps, "Welche Form hat die Flagge?", answer(rectangular(row), "Rechteckig", "Weder noch", "Quadratisch"));
		ask(steps, "Hat die Flagge einen Rahmen?", answer(frame(row) ? "Ja" : "Nein", "Ja", "Nein"));
		ask(steps, "Hat die Flagge einen Gösch?",
				answer(sheet.value(row, "Gösch?").equals("1") ? "Ja" : "Nein", "Ja", "Nein"));
		ask(steps, "Entferne Rahmen und Gösch gedanklich. Ragt ein Dreieck ganz vom linken Rand herein?",
				answer(FlagSheet.isSet(sheet.value(row, "Dreieck von links?"))
						&& !sheet.value(row, "Dreieck von links?").equals("0") ? "Ja" : "Nein", "Ja", "Nein"));

		String type = sheet.value(row, "Hintergrundtyp");
		ask(steps, "Entferne auch das Dreieck und die Zusatzelemente. Was beschreibt den Hintergrund am besten?",
				answer(BACKGROUNDS.get(type), BACKGROUNDS.values().toArray(new String[0])));
		branchQuestions(steps, row, type);

		add(steps, "SketchImage:" + backgroundSketch(row, type));

		List<Element> elements = elements(row);
		List<String> colors = new ArrayList<>(split(sheet.value(row, "Hintergrundfarben")));
		if (!elements.isEmpty())
			colors.addAll(elementSteps(steps, elements));
		fillAreas(steps, 0, colors);
		add(steps, "Image:" + image(row));
		return steps;
	}

	/** Die Folgefragen des Zweigs — je Hintergrundtyp die Spalten, die er nach sich zieht. */
	private void branchQuestions(List<String> steps, List<String> row, String type) {
		switch (type) {
			case "0" -> {
				ask(steps, "Wie viele waagerechte Streifen?",
						answer(sheet.value(row, "W-Streifen"), STRIPE_COUNTS.toArray(new String[0])));
				if (sheet.value(row, "W-Streifen").equals("3"))
					ask(steps, "Wie sind die Streifen verteilt?", coded(sheet.value(row, "3W"),
							"alle gleich breit", "mittlerer breiter", "mittlerer schmaler",
							"oberster breiter", "unterster breiter"));
				if (sheet.value(row, "W-Streifen").equals("5"))
					ask(steps, "Wie sind die Streifen verteilt?", coded(sheet.value(row, "5W"),
							"2 und 4 dünn, Mitte nicht breiter", "alle gleich",
							"Mitte breiter, 2 und 4 nicht dünn", "Mitte breiter und 2 und 4 dünn",
							"oberster am breitesten", "unterster am breitesten"));
			}
			case "1" -> {
				ask(steps, "Wie viele senkrechte Streifen?",
						answer(sheet.value(row, "S-Streifen"), "2", "3", "4", "5"));
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
					answer(sheet.value(row, "SW Streifen"), "2", "3", "4", "5"));
			default -> { }
		}
	}

	// ---- Elemente -------------------------------------------------------------

	private record Element(String name, String position, String color, String count) {}

	private List<Element> elements(List<String> row) {
		List<Element> result = new ArrayList<>();
		for (int slot = 1; slot <= 4; slot++) {
			String name = sheet.value(row, "E" + slot);
			if (!FlagSheet.isSet(name))
				continue;
			result.add(new Element(name, sheet.value(row, "E" + slot + " Position"),
					sheet.value(row, "E" + slot + " Farbe"), sheet.value(row, "E" + slot + " Anzahl")));
		}
		return result;
	}

	/**
	 * Erst alle Elemente anhaken, dann Ort und Anzahl je Element, dann alles auf einmal zeichnen,
	 * dann die Farben. Die Reihenfolge ist Absicht: Wer den Ort noch nicht beantwortet hat, soll das
	 * Element nicht schon an seinem Platz sehen.
	 */
	/**
	 * Erst alle Elemente anhaken, dann je Element Anzahl und Ort, dann alles auf einmal zeichnen.
	 * Gefärbt wird nicht hier, sondern am Ende der Karte zusammen mit den Hintergrundflächen.
	 *
	 * <p>Die Elementblöcke stehen in einem Shuffle: Sonst verriete ihre Reihenfolge, welches Element
	 * im Blatt zuerst steht. Anzahl und Ort eines Elements bleiben dabei im selben Segment — sie
	 * gehören zusammen, und der Ort braucht die Anzahl für den richtigen Numerus.</p>
	 *
	 * @return die Farben der Elemente, in der Reihenfolge ihrer Flächen
	 */
	private List<String> elementSteps(List<String> steps, List<Element> elements) {
		List<String> names = new ArrayList<>();
		for (Element element : elements)
			if (!names.contains(element.name()))
				names.add(element.name());
		List<String> wrong = new ArrayList<>(ELEMENT_POOL);
		wrong.removeAll(names);
		add(steps, "Output:Welche Zusatzelemente siehst Du?");
		add(steps, "MC:" + String.join("|", names) + "*" + String.join("|", wrong));

		for (int i = 0; i < elements.size(); i++) {
			Element element = elements.get(i);
			if (elements.size() > 1)
				pending = i == 0 ? "<ShuffleStart>" : "<ShuffleBreak>";
			if (FlagSheet.isSet(element.count()))
				ask(steps, "Wie viele " + WORDS.get(element.name())[1].substring(4) + "?",
						answer(element.count(), COUNTS.toArray(new String[0])));
			ask(steps, "Wo " + verb(element) + " " + word(element) + "?",
					answer(POSITIONS.get(Integer.parseInt(untolerated(element.position()))),
							POSITIONS.toArray(new String[0])));
		}
		if (elements.size() > 1)
			pending = "<ShuffleEnd>";

		List<String> colors = new ArrayList<>();
		for (Layout layout : layout(elements)) {
			add(steps, "SketchImageAdd:" + layout.step());
			colors.addAll(split(layout.element().color()));
		}
		return colors;
	}

	private record Layout(Element element, String step) {}

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
				available[i] = 1;
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

			String step = sketchOf(element) + "," + untolerated(element.position()) + "," + number(size);
			if (group.size() > 1)
				step += "," + number(offset) + ",0";
			result.add(new Layout(element, step));
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
		String file = ELEMENT_FILES.get(element.name());
		if (file == null)
			throw new RuntimeException("Für '" + element.name() + "' gibt es noch keine Elementdatei");
		return file;
	}

	// ---- Skizze und Farben ----------------------------------------------------

	/**
	 * Der Dateiname folgt aus den Attributen des Zweigs — wie {@code waagerecht-3}, nur mit Wörtern.
	 *
	 * <p>Gösch und Dreieck werden nach §5 in die Datei <b>hineingezeichnet</b> und nicht zur Laufzeit
	 * angehängt. Sie gehören deshalb in den Namen: {@code waagerecht-7-goesch} ist eine andere Datei
	 * als {@code waagerecht-7}, und sie trägt eine Fläche mehr.</p>
	 */
	private String backgroundSketch(List<String> row, String type) {
		return branchSketch(row, type) + (sheet.value(row, "Gösch?").equals("1") ? "-goesch" : "");
	}

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
	private void fillAreas(List<String> steps, int firstArea, List<String> colors) {
		if (colors.isEmpty())
			return;
		add(steps, "Output:Welche Farbe hat die markierte Fläche?");
		for (int i = 0; i < colors.size(); i++) {
			if (colors.size() > 1)
				pending = i == 0 ? "<ShuffleStart>" : "<ShuffleBreak>";
			add(steps, "SketchImageMark:" + (firstArea + i));
			add(steps, answer(colors.get(i), COLORS.toArray(new String[0])));
			add(steps, "SketchImageFill:" + (firstArea + i) + "," + colors.get(i));
		}
		if (colors.size() > 1)
			pending = "<ShuffleEnd>";
	}

	// ---- Ableitungen ----------------------------------------------------------

	/** Nepal ist weder das eine noch das andere, die Schweiz und der Vatikan sind quadratisch. */
	private String rectangular(List<String> row) {
		return switch (sheet.value(row, "Rechtwinklig?")) {
			case "1" -> "Rechteckig";
			case "2" -> "Quadratisch";
			default -> "Weder noch";
		};
	}

	private boolean frame(List<String> row) {
		return sheet.value(row, "Rahmen?").equals("1");
	}

	/** Der Dateiname folgt aus dem englischen Namen: {@code Brazil} -> {@code brazil-flag-square-small.png}. */
	private String image(List<String> row) {
		return sheet.english(row).toLowerCase(java.util.Locale.ROOT).replace(' ', '-')
				+ "-flag-square-small.png";
	}

	// ---- Prüfung --------------------------------------------------------------

	/** Der echte Parser und ein Flächenlauf — beide finden anderes. */
	private void check(String country, List<String> steps) {
		try {
			new Card(steps);
		} catch (RuntimeException e) {
			throw new RuntimeException(country + ": die erzeugte Zeile ist nicht lesbar", e);
		}
		int available = 0;
		for (String raw : steps) {
			String step = raw.substring(raw.lastIndexOf('>') + 1);
			if (step.startsWith("SketchImage:"))
				available = areasOf(step.substring("SketchImage:".length()));
			else if (step.startsWith("SketchImageAdd:"))
				available += areasOf(step.substring("SketchImageAdd:".length()).split(",")[0]);
			else if (step.startsWith("SketchImageMark:") || step.startsWith("SketchImageFill:")) {
				int area = Integer.parseInt(step.substring(step.indexOf(':') + 1).split(",")[0]);
				if (area >= available)
					throw new RuntimeException(country + ": " + step + ", aber es gibt erst "
							+ available + " Flächen");
			}
		}
	}

	// ---- Dateien --------------------------------------------------------------

	/** Vorhandene Karten derselben Id werden ersetzt, alles andere bleibt stehen. */
	private static void write(Map<String, String> generated) throws IOException {
		List<String> lines = new ArrayList<>();
		List<String> old = List.of(Files.readString(DECK, StandardCharsets.UTF_8)
				.replace("﻿", "").split("\r\n"));
		// Erkannt wird eine Karte an ihrer Id ODER an ihrem Mark: — sonst stuende eine
		// handgeschriebene Karte nach dem ersten Lauf doppelt im Deck, einmal unter ihrer alten
		// und einmal unter ihrer Id aus dem Blatt.
		Map<String, String> byMark = new LinkedHashMap<>();
		for (Map.Entry<String, String> card : generated.entrySet())
			byMark.put(card.getValue().split(";")[3], card.getKey());
		int replaced = 0;
		for (String line : old) {
			if (line.isBlank())
				continue;
			String[] parts = line.split(";");
			String id = parts[0];
			if (!generated.containsKey(id) && parts.length > 3)
				id = byMark.getOrDefault(parts[3], id);
			if (generated.containsKey(id)) {
				lines.add(generated.remove(id));
				replaced++;
			} else
				lines.add(line);
		}
		lines.addAll(generated.values());
		int width = lines.stream().mapToInt(line -> line.split(";", -1).length).max().orElse(3);
		List<String> header = new ArrayList<>(List.of("Index", "Bemerkung", "Label"));
		for (int i = 1; i <= width - 3; i++)
			header.add("Step" + i);
		lines.set(0, String.join(";", header));
		Files.writeString(DECK, "﻿" + String.join("\r\n", lines) + "\r\n", StandardCharsets.UTF_8);
		System.out.println(replaced + " ersetzt, " + generated.size() + " neu — " + DECK);
	}

	private int areasOf(String sketch) {
		return areas.computeIfAbsent(sketch, name -> read(name).size());
	}

	/** Die Flächen einer Strukturdatei — gezählt wird nur, wie viele es sind. */
	private static List<double[]> read(String sketch) {
		try {
			JsonNode root = new ObjectMapper().readTree(SKETCHES.resolve(sketch + ".geojson").toFile());
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
			throw new RuntimeException("Strukturdatei fehlt: " + SKETCHES.resolve(sketch + ".geojson"), e);
		}
	}

	// ---- Kleinkram ------------------------------------------------------------

	private void add(List<String> steps, String step) {
		steps.add(pending + step);
		pending = "";
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

	/** Alles vor einer Toleranzklammer: {@code 4(9)} ist die 4, die die 9 durchgehen lässt. */
	private static String untolerated(String value) {
		int bracket = value.indexOf('(');
		return bracket < 0 ? value.trim() : value.substring(0, bracket).trim();
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

	private static Map<String, Double> orderedFactors(Object... pairs) {
		Map<String, Double> map = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
			map.put((String) pairs[i], (Double) pairs[i + 1]);
		return map;
	}

	private static Map<String, String[]> orderedWords(Object[][] pairs) {
		Map<String, String[]> map = new LinkedHashMap<>();
		for (Object[] pair : pairs)
			map.put((String) pair[0], (String[]) pair[1]);
		return map;
	}
}
