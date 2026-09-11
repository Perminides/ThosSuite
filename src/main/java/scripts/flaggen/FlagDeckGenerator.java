package scripts.flaggen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
	/** Die dritte Antwort der Kreuz-und-Diagonale-Frage. Steht dreimal, deshalb als Konstante. */
	private static final String KEIN_KREUZ = "Keine Diagonale oder Kreuz";

	private static final List<String> DREIECK_FORMEN = List.of(
			"Kein Dreieck",
			"Dreieck nur in der linken Hälfte",
			"Eher ein Trapez als ein Dreieck",
			"Dreieck bis zum rechten Rand",
			"Das Dreiecksgebilde geht in eine waagerechte Spur bis zum rechten Rand über");

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
			"Vogel", "vogel", "Sonne", "sonne", "Union Jack", "union-jack",
			"Dreizack", "dreizack", "Muster", "muster", "Drache", "drache");

	/** Das Rasterfeld als Behälter — der äußerste, den jedes Element durchläuft. */
	private static final String SEGMENT = "Segment";

	/**
	 * <b>Alle Größenfaktoren, und zwar alle.</b> Ein Behälter, eine Zeile, ein Wert je Kinderzahl
	 * (Index = Anzahl − 1). Die gezeichnete Größe einer Figur ist das Produkt dieser Faktoren vom
	 * Rasterfeld nach innen, mal den Koordinaten ihrer Datei:
	 *
	 * <pre>
	 * Burundi     Kreis     im Segment, allein         0,8
	 *             Sterne    im Kreis, allein           0,8 · 0,875   = 0,7
	 * Brasilien   Raute     im Segment, allein         0,8
	 *             Kreis     in der Raute, allein       0,8 · 1,12    = 0,896
	 *             Schrift   im Kreis, zu zweit         0,8 · 1,12 · 0,625 = 0,56
	 * </pre>
	 *
	 * <p>Das Rasterfeld steht als {@code Segment} mit in der Tabelle: Es ist der äußerste Behälter,
	 * und seine Zahlen tragen zweierlei — den Platz, den sich n Figuren im 60 breiten Feld teilen
	 * müssen, und die Luft zum Feldrand. Ohne die Luft wären es 1,0 · 1,0 · 0,7 · 0,55. Beides in
	 * einer Zahl heißt auch: Die Luft ist je Anzahl einstellbar, ohne eine Datei anzufassen.</p>
	 *
	 * <p>Beim Kreis ist die Grenze ausrechenbar: Ein Element bringt seinen Kasten von 40 × 40 mit,
	 * und dessen äußere Ecke muss innerhalb des Radius bleiben. Bei einem Kind ist das die halbe
	 * Diagonale, {@code 40 · k / √2 ≤ 25}, also {@code k ≤ 0,884}. Eingetragen ist 0,875. Bei
	 * mehreren kommt der Versatz dazu und die Grenzen sinken.</p>
	 *
	 * <p>Eine Elementdatei <b>darf</b> 60 × 40 groß sein — die Rechnung geht trotzdem von 40 aus,
	 * sonst würde der schlechteste Fall alle schmalen Figuren mitverkleinern. Ein Element, das
	 * breiter als 40 ist, gehört deshalb nicht in einen Behälter.</p>
	 *
	 * <p>Die Raute ist nicht gerechnet, sondern am Bild gefunden — ihre Ecken laufen spitz zu, da
	 * gilt die Kastenregel nicht.</p>
	 */
	private static final Map<String, double[]> CONTAINERS = Map.of(
			SEGMENT, new double[] {0.8, 0.8, 0.56, 0.44},
			"Raute", new double[] {1.12, 1.12, 0.784, 0.616},
			"Kreis", new double[] {0.875, 0.625, 0.394, 0.309});

	/**
	 * Wo die Geschwister im Kasten ihres Behälters sitzen, je nach Anzahl. Keine Größen — Orte.
	 * Sie skalieren mit dem Kasten, also mit dem Faktor des Behälters.
	 */
	private static final Map<Integer, double[]> OFFSETS = Map.of(
			1, new double[] {0},
			2, new double[] {-10, 10},
			3, new double[] {-20, 0, 20},
			4, new double[] {-22.5, -7.5, 7.5, 22.5});

	/**
	 * Der Zahlenblock der Kartenkarten: {@code 10000 + Karten-Id}. Ein eigener Block, weil die
	 * handgeschriebenen Zusatzfragen in der zweiten Deck-Datei ihre Nummern von Hand bekommen — der
	 * Generator kann nicht wissen, welche dort frei ist, und zwei Karten mit derselben Id teilten
	 * sich den Lernfortschritt.
	 */
	private static final int MAP_CARD_BASE = 10000;

	/**
	 * Streumuster für „verstreut" (Ortswert 9): drei Rasterfelder, die gestreut <i>aussehen</i> —
	 * keine Reihe, keine Diagonale, nichts aneinandergrenzend.
	 *
	 * <p>Eine Tabelle statt einer Rechnung, weil Arithmetik wie {@code (id * 3) % 9} zwar ebenso
	 * deterministisch wäre, aber Tripel erzeugt, die zufällig auf einer Linie liegen — und drei
	 * Sterne in einer Reihe sehen nach Absicht aus, nicht nach Streuung. Hier hat ein Mensch
	 * entschieden.</p>
	 *
	 * <p>Gewählt wird über die Karten-Id, nicht gewürfelt: Dieselbe Flagge bekommt bei jedem Lauf
	 * dieselben Felder, und der {@code git diff} der Deck-Datei bleibt still.</p>
	 */
	private static final List<int[]> VERSTREUT = List.of(
			new int[] {0, 5, 7}, new int[] {1, 3, 8}, new int[] {2, 3, 7},
			new int[] {1, 5, 6}, new int[] {0, 5, 6}, new int[] {2, 4, 7});

	private final FlagSheet sheet;
	private final Map<String, Integer> areas = new LinkedHashMap<>();
	private final Map<String, List<String>> sketchColors = new LinkedHashMap<>();
	/** Flaggenbilder, die es noch nicht gibt. Gemeldet am Ende, nicht abgebrochen. */
	private final Set<String> missing = new LinkedHashSet<>();
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
			einhaengen(generated, id, card(row));
			einhaengen(generated, MAP_CARD_BASE + id, mapCard(row));
		} catch (RuntimeException e) {
			throw new RuntimeException(sheet.country(row) + " (Id " + id + "): " + e.getMessage(), e);
		}
	}

	private void einhaengen(Map<Integer, String> generated, int id, List<String> steps) {
		check(steps);
		if (generated.put(id, String.join(";", steps)) != null)
			throw new RuntimeException("die Id " + id + " gibt es zweimal");
	}

	// ---- Die Karte ------------------------------------------------------------

	/** Die vollständige Zeile: Id, Bemerkung, Label, dann die Schritte. */
	private List<String> card(List<String> row) {
		List<String> steps = new ArrayList<>(List.of(String.valueOf(id(row)), remark(row), "Flagge",
				"Mark:" + shape(row)));

		ask(steps, "Welche Form hat die Flagge?",
				answer(rectangular(row), "Rechteckig", "Nicht rechteckig", "Quadratisch"));
		ask(steps, "Hat die Flagge einen Rahmen?", answer(frame(row) ? "Ja, sie hat einen Rahmen" : "Kein Rahmen",
				"Ja, sie hat einen Rahmen", "Kein Rahmen"));

		// Kreuz und Diagonale vorweg — sonst würde ihr linker Arm mit einem Dreieck von links verwechselt.
		String typeCell = sheet.value(row, "Hintergrundtyp");
		String type = untolerated(typeCell);
		// Der Zweig haengt am Typ, nicht am Antworttext: Sonst bricht ein Umbenennen der Option
		// still den Ablauf, statt nur die Anzeige zu aendern.
		boolean geteilt = type.equals("2") || type.equals("3");
		String vorweg = type.equals("2") ? "Kreuz" : type.equals("3") ? "Diagonale" : KEIN_KREUZ;
		ask(steps, "Teilt ein Kreuz oder eine Diagonale die Flagge, wenn Du Zusatzelemente und Rahmen ignorierst?",
				answer(vorweg, "Kreuz", "Diagonale", KEIN_KREUZ));
		if (!geteilt)
			ask(steps, "Entferne gedanklich eine Dreiecksstruktur von links, eine Gösch, alle "
					+ "Zusatzelemente und einen Rahmen. Was beschreibt nun den Hintergrund am besten?",
					answer(BACKGROUNDS.get(type), backgroundTolerated(typeCell),
							FILL_BACKGROUNDS.toArray(new String[0])));
		branchQuestions(steps, row, type);

		Canvas canvas = new Canvas(steps);
		List<Fill> fills = new ArrayList<>();
		String background = branchSketch(row, type);
		paint(background, fills, canvas.background(background), colors(sheet.value(row, "Hintergrundfarben")));

		// Ein Sonderhintergrund ist eine handgemachte Datei, die alles enthalten kann — auch eine
		// Gösch oder etwas Dreiecksartiges. Ihn zusätzlich nach diesen Attributen zu fragen, führt
		// zwangsläufig in Widersprüche: Bei Antigua schiebt sich von links sichtbar eine Spitze ins
		// Bild, im Blatt steht trotzdem 0. Wer richtig hinsieht, bekäme falsch. Also nicht fragen.
		if (!type.equals("7")) {
			// Gösch nach der Göschfrage auflegen (Leinwand-Silhouette, cell = -1).
			boolean goesch = sheet.value(row, "Gösch?").equals("1");
			ask(steps, "Hat die Flagge eine Gösch?", answer(goesch ? "Ja, sie hat einen Gösch" : "Kein Gösch", "Ja, sie hat einen Gösch", "Kein Gösch"));
			if (goesch)
				paint("goesch", fills, canvas.overlay("goesch", "-1"), colors(sheet.value(row, "Gösch Farbe")));

			// Bei einer Schräge ist die untere linke Hälfte selbst ein Dreieck von links; wer richtig
			// hinsieht, bekäme falsch. Beim senkrechten Kreuz gilt das nicht — dort trägt aber keine
			// Flagge ein Dreieck, und das Blatt sagt im ganzen Zweig `x`. Also überall nicht fragen.
			if (type.equals("2") || type.equals("3")) {
				if (FlagSheet.isSet(sheet.value(row, "Dreieck von links?")))
					throw new RuntimeException("Kreuz oder Diagonale mit einem Dreieck von links — die"
							+ " Frage entfällt hier, das Dreieck würde also still verschwinden");
			} else {
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
			}
		}

		elementFills(steps, canvas, elements(row), fills, id(row));
		fillAreas(steps, fills);
		add(steps, "Image:" + image(row));
		add(steps, "Pause:"); // Zeit, die echte Flagge anzusehen
		// Dasselbe noch einmal als Abspann: Auch wer die Karte reisst, soll die Flagge sehen.
		add(steps, "<OnFail>Image:" + image(row));
		add(steps, "Pause:");
		prependHint(steps, row);
		return steps;
	}

	/**
	 * Die Gegenrichtung: die echte Flagge zeigen und das Land auf der Karte anklicken lassen.
	 *
	 * <p>Zwei Schritte, mehr braucht sie nicht. Sie hängt an denselben Ableitungen wie die
	 * Flaggenkarte — {@link #image} für die Datei, {@link #shape} für die Fläche auf der Weltkarte.</p>
	 */
	private List<String> mapCard(List<String> row) {
		return List.of(String.valueOf(MAP_CARD_BASE + id(row)), remark(row), "Flagge zu Karte",
				"Image:" + image(row), "Click:" + shape(row));
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

	/**
	 * Was in E1 steht, wenn die Flagge keine Zusatzelemente hat — der Antworttext selbst und kein
	 * Elementname. Die Elementfrage entfällt nie, sie hat dann eben diese Antwort. E2 bis E4 bleiben
	 * dabei auf {@code x}: Die sprechen über einen unbelegten Platz, E1 über die Antwort.
	 */
	private static final String KEINE = "Keine";

	/**
	 * Die Elemente einer Zeile, dazu die Namen, die bei der Elementfrage der ganzen Zeile durchgehen
	 * sollen. Getrennt, weil {@link #KEINE} kein Element ist, seine Klammer aber eine Toleranz trägt:
	 * {@code Keine (Emblem)} duldet, wer bei Bolivien das Staatswappen erwartet.
	 */
	private record Elemente(List<Element> liste, List<String> toleriert) {}

	private Elemente elements(List<String> row) {
		List<Element> result = new ArrayList<>();
		List<String> toleriert = List.of();
		boolean keine = false;
		for (int slot = 1; slot <= 4; slot++) {
			String name = sheet.value(row, "E" + slot);
			if (!FlagSheet.isSet(name))
				continue;
			String position = sheet.value(row, "E" + slot + " Position");
			if (untolerated(name).equals(KEINE)) {
				if (slot != 1)
					throw new RuntimeException("'" + KEINE + "' steht nur in E1, hier aber in E" + slot);
				if (!position.equals("x"))
					throw new RuntimeException("'" + KEINE + "' hat keinen Ort, in 'E1 Position' steht: " + position);
				keine = true;
				toleriert = bracket(name);
				continue;
			}
			if (position.equals("x"))
				continue; // Kein Ort, kein Sketch, keine Frage — Ort und Element stehen immer gemeinsam auf 'x'
			result.add(new Element(untolerated(name), bracket(name), position,
					sheet.value(row, "E" + slot + " Farbe"), sheet.value(row, "E" + slot + " Anzahl")));
		}
		if (keine && !result.isEmpty())
			throw new RuntimeException("'" + KEINE + "' und daneben ein echtes Element: " + result.get(0).name());
		return new Elemente(result, toleriert);
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
	private void elementFills(List<String> steps, Canvas canvas, Elemente elemente, List<Fill> fills, int id) {
		List<Element> elements = elemente.liste();
		List<String> names = new ArrayList<>();
		for (Element element : elements)
			if (!names.contains(element.name()))
				names.add(element.name());
		add(steps, "Output:Welche Zusatzelemente siehst Du?");
		List<String> correct = names.isEmpty() ? List.of(KEINE) : names;
		// Toleriert: falsch, aber ohne Abbruch. Wer Ägyptens Adler für ein Emblem hält, liegt nicht
		// wirklich daneben. Sie kommen aus der Klammer hinter dem Elementnamen — und bei einer Flagge
		// ohne Elemente aus der Klammer hinter dem 'Keine'.
		List<String> tolerated = new ArrayList<>();
		List<List<String>> quellen = new ArrayList<>();
		quellen.add(elemente.toleriert());
		for (Element element : elements)
			quellen.add(element.tolerated());
		for (List<String> quelle : quellen)
			for (String name : quelle) {
				if (!ELEMENT_POOL.contains(name))
					throw new RuntimeException("Kein Elementname in der Toleranzklammer: " + name);
				if (!correct.contains(name) && !tolerated.contains(name))
					tolerated.add(name);
			}

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
						answer(colors(element.color()).liste().size() == 2 ? "Ja, der Kreis ist geteilt" : "Ungeteilter Kreis",
						"Ja, der Kreis ist geteilt", "Ungeteilter Kreis"));
		});

		shuffled(steps, elements, element ->
				ask(steps, "Wo " + verb(element) + " " + word(element) + "?", position(element)));

		for (Layout layout : layout(elements, id)) {
			List<Integer> areas = new ArrayList<>();
			for (String placement : layout.placements())
				areas.addAll(canvas.overlay(layout.sketch(), placement));
			if (paintFixed(steps, layout, areas))
				continue;
			// Drei Kopien sind unsere Zeichenentscheidung, nicht seine Daten: Sie werden gemeinsam
			// schraffiert und gemeinsam gefärbt, so als stünde ein '&' im Blatt.
			Farben farben = colors(layout.element().color());
			if (layout.placements().size() > 1 && !farben.liste().isEmpty()) {
				if (farben.liste().size() > 1)
					throw new RuntimeException(layout.element().name()
							+ " ist verstreut und trägt mehr als eine Farbe: " + layout.element().color());
				farben = new Farben(farben.liste(), true);
			}
			paint(layout.sketch(), fills, areas, farben);
		}
	}

	/**
	 * Färbt eine Figur, deren Farben in ihrer Strukturdatei stehen — sofort, noch bevor die erste
	 * Farbfrage kommt. Antwort: ob sie eine solche Figur war und damit erledigt ist.
	 *
	 * <p>Der Zeitpunkt ist die halbe Aussage. Der Union Jack erscheint fertig gefärbt zusammen mit
	 * den anderen Zusatzelementen; wer die Karte lernt, sieht daran sofort, dass nach ihm nicht mehr
	 * gefragt wird. Käme die Füllung später, sähe sie aus wie eine übersprungene Frage.</p>
	 *
	 * <p>Ist die Figur verstreut, liegt sie mehrfach auf der Leinwand. Dann trägt jede Kopie dieselbe
	 * Farbfolge, und gleichfarbige Flächen aller Kopien werden in einem Schritt gefüllt.</p>
	 */
	private boolean paintFixed(List<String> steps, Layout layout, List<Integer> areas) {
		List<String> fixed = colorsOf("elements", layout.sketch());
		if (fixed.isEmpty())
			return false;
		if (FlagSheet.isSet(layout.element().color()))
			throw new RuntimeException(layout.sketch() + " trägt seine Farben selbst, im Blatt steht"
					+ " trotzdem eine: " + layout.element().color());
		for (int i = 0; i < fixed.size(); i++) {
			List<Integer> gleiche = new ArrayList<>();
			for (int k = i; k < areas.size(); k += fixed.size())
				gleiche.add(areas.get(k));
			add(steps, "SketchImageFill:" + areaList(gleiche) + "," + fixed.get(i));
		}
		return true;
	}

	/**
	 * Die tolerierten Hintergrundtypen aus der Klammer, als Antworttexte. Belarus steht als
	 * {@code 0 (5)} im Blatt: zwei waagerechte Streifen — wer sein Ornamentband aber für einen Teil
	 * des Hintergrunds hält, liegt nicht wirklich daneben.
	 *
	 * <p>Kreuz und Diagonale dürfen nicht in der Klammer stehen. Über die beiden entscheidet die
	 * Frage davor, und die wüsste von der Toleranz nichts — die Karte bräche still an der falschen
	 * Stelle ab.</p>
	 */
	private static List<String> backgroundTolerated(String cell) {
		List<String> result = new ArrayList<>();
		for (String value : bracket(cell)) {
			if (value.equals("2") || value.equals("3"))
				throw new RuntimeException("Kreuz und Diagonale gehen als tolerierter Hintergrundtyp nicht: " + cell);
			String text = BACKGROUNDS.get(value);
			if (text == null)
				throw new RuntimeException("Kein Hintergrundtyp mit dem Wert " + value + ": " + cell);
			result.add(text);
		}
		return result;
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

	/**
	 * Dateiname und Platzierungen einer Figur: alles, was hinter {@code SketchImageAdd:} steht.
	 * Mehrere, wenn das Element verstreut ist — dann steht dieselbe Figur in drei Feldern.
	 */
	private record Layout(Element element, String sketch, List<String> placements) {}

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
	private List<Layout> layout(List<Element> elements, int id) {
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

		// Der Faktor ist das laufende Produkt der Behaelter von aussen nach innen. Das Rasterfeld
		// zaehlt als aeusserster Behaelter mit, deshalb faengt die Kette nicht bei 1 an.
		double[] faktor = new double[elements.size()];
		double[] kasten = new double[elements.size()];      // der Kasten, in dem die Figur sitzt
		for (int i = 0; i < elements.size(); i++) {
			kasten[i] = parent[i] < 0 ? 1.0 : faktor[parent[i]];
			String behaelter = parent[i] < 0 ? SEGMENT : elements.get(parent[i]).name();
			int anzahl = Math.min(gruppe(elements, parent, i).size(), 4);
			faktor[i] = kasten[i] * CONTAINERS.get(behaelter)[anzahl - 1];
		}

		for (int i = 0; i < elements.size(); i++) {
			Element element = elements.get(i);
			List<Integer> group = gruppe(elements, parent, i);
			double size = faktor[i];
			double offset = OFFSETS.get(Math.min(group.size(), 4))[group.indexOf(i)] * kasten[i];

			if (untolerated(element.position()).equals(VERSTREUT_ORT)) {
				List<String> placements = new ArrayList<>();
				for (int cell : streufelder(id, belegteFelder(elements)))
					placements.add(cell + "," + number(size));
				// Verstreut heißt: dieselbe Figur mehrfach. Der Sammelglyph des Sterns stünde dann
				// drei Mal da — drei Haufen statt drei Sternen. Also der einzelne.
				result.add(new Layout(element, sketchOf(element, true), placements));
				continue;
			}
			// Ein Ornamentband besetzt eine ganze Spalte, keinen Punkt. Bei ihm sind die Werte der
			// Toleranzklammer deshalb Platzierungen und nicht bloß geduldete Klicks.
			if (element.name().equals("Muster") && !bracket(element.position()).isEmpty()) {
				List<String> felder = new ArrayList<>(bracket(element.position()));
				felder.add(untolerated(element.position()));
				List<String> placements = new ArrayList<>();
				for (int feld = 0; feld <= 8; feld++)      // aufsteigend, damit die Flächen von oben zählen
					if (felder.contains(String.valueOf(feld)))
						placements.add(feld + "," + number(size));
				result.add(new Layout(element, sketchOf(element, false), placements));
				continue;
			}
			String placement = untolerated(element.position()) + "," + number(size);
			if (group.size() > 1)
				placement += "," + number(offset) + ",0";
			result.add(new Layout(element, sketchOf(element, false), List.of(placement)));
		}
		return result;
	}

	/** Die Figuren, die sich denselben Kasten teilen: gleicher Behälter, gleiches Rasterfeld. */
	private static List<Integer> gruppe(List<Element> elements, int[] parent, int i) {
		List<Integer> result = new ArrayList<>();
		for (int j = 0; j < elements.size(); j++)
			if (parent[j] == parent[i]
					&& elements.get(j).position().equals(elements.get(i).position()))
				result.add(j);
		return result;
	}

	/** Der Ortswert, der „über die ganze Flagge verteilt" bedeutet. */
	private static final String VERSTREUT_ORT = "9";

	/** Die Rasterfelder, in denen schon etwas liegt — verstreute Elemente zählen nicht mit. */
	private static Set<Integer> belegteFelder(List<Element> elements) {
		Set<Integer> belegt = new HashSet<>();
		for (Element element : elements) {
			String ort = untolerated(element.position());
			if (!ort.equals(VERSTREUT_ORT))
				belegt.add(Integer.parseInt(ort));
		}
		return belegt;
	}

	/**
	 * Drei freie Felder aus {@link #VERSTREUT} — das erste Muster ab {@code id}, das kein belegtes
	 * Feld trifft. Passt keins, fliegt es: Bei höchstens drei belegten Feldern und sechs Mustern
	 * kommt das praktisch nicht vor, und wenn doch, will man ein Muster ergänzen statt still etwas
	 * Schlechteres zu malen.
	 */
	private static int[] streufelder(int id, Set<Integer> belegt) {
		for (int i = 0; i < VERSTREUT.size(); i++) {
			int[] muster = VERSTREUT.get(Math.floorMod(id + i, VERSTREUT.size()));
			boolean frei = true;
			for (int cell : muster)
				if (belegt.contains(cell))
					frei = false;
			if (frei)
				return muster;
		}
		throw new RuntimeException("Kein Streumuster passt, belegt sind " + belegt);
	}

	/**
	 * Sterne haben drei Bilder: einer, zwei, mehr als zwei. Alles andere hat genau eins.
	 *
	 * <p>{@code einzeln} zwingt zum Einzelglyph — beim Verstreuten, wo dieselbe Figur mehrfach
	 * gezeichnet wird und der Sammelglyph zu drei Haufen würde.</p>
	 */
	private static String sketchOf(Element element, boolean einzeln) {
		if (element.name().equals("Stern")) {
			if (einzeln)
				return "stern";
			int count = element.count().isEmpty() || element.count().equals("x")
					? 1 : Integer.parseInt(element.count());
			// Bis fuenf zeigt die Skizze die echte Anzahl -- so weit erfasst man sie auf einen Blick.
			// Darueber liest man ohnehin nur "viele", und der Haufen sagt genau das.
			return switch (count) {
				case 1 -> "stern";
				case 2 -> "stern-zwei";
				case 3 -> "stern-drei";
				case 4 -> "stern-vier";
				case 5 -> "stern-fuenf";
				default -> "stern-haufen";
			};
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
			// Die Zelle darf eine Toleranzklammer tragen: `Hellblau (Blau)`. Gemalt wird die Farbe
			// davor — SketchColor kennt den Klammertext nicht.
			String farbe = untolerated(fill.color());
			add(steps, "SketchImageMark:" + areaList(fill.areas()));
			add(steps, answer(farbe, bracket(fill.color()), COLORS.toArray(new String[0])));
			add(steps, "SketchImageFill:" + areaList(fill.areas()) + "," + farbe);
		});
	}

	/** Die Flächennummern einer Füllung, mit {@code |} getrennt: {@code 3|4}. */
	private static String areaList(List<Integer> areas) {
		List<String> parts = new ArrayList<>();
		for (int area : areas)
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
	private int id(List<String> row) {
		return (version(row) - 1) * 1000 + Integer.parseInt(sheet.number(row));
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
	 * Der Hinweis, sonst leer. Er trennt zwei Flaggen desselben Landes („Flagge bis 2021"). Zwei
	 * Umbrüche dahinter setzen ihn als eigenen Absatz ab; sie stehen als {@code <br />} da, weil in
	 * eine Deck-Zelle kein echter Zeilenumbruch passt — das Fragefeld versteht {@code <br />},
	 * {@code <b>} und {@code <i>}.
	 */
	private String hint(List<String> row) {
		String text = sheet.value(row, "Hinweistext");
		return FlagSheet.isSet(text) ? "<i>" + text + "</i><br /><br />" : "";
	}

	/**
	 * Stellt den Hinweis <b>jeder</b> Frage der Karte voran, nicht nur der ersten.
	 *
	 * <p>Er sagt, welche der Flaggen eines Landes gemeint ist. Stünde er nur einmal, müsste man ihn
	 * über ein Dutzend Fragen hinweg im Kopf behalten — und die Antwort auf Frage neun hängt genauso
	 * an ihm wie die auf Frage eins.</p>
	 *
	 * <p>Ein Nachlauf über die fertige Zeile statt eines Zusatzes an jeder Frage: So kann keine Frage
	 * ihn vergessen, auch keine, die es hier noch gar nicht gibt.</p>
	 */
	private void prependHint(List<String> steps, List<String> row) {
		String hint = hint(row);
		if (hint.isEmpty())
			return;
		for (int i = 3; i < steps.size(); i++) {   // 0 bis 2 sind Id, Bemerkung und Label
			String step = steps.get(i);
			int start = 0;
			while (start < step.length() && step.charAt(start) == '<') {   // über die Marker hinweg
				int close = step.indexOf('>', start);
				if (close < 0)
					break;
				start = close + 1;
			}
			if (!step.startsWith("Output:", start))
				continue;
			int text = start + "Output:".length();
			steps.set(i, step.substring(0, text) + hint + step.substring(text));
		}
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
			else if (step.startsWith("SketchImageAdd:")) {
				String[] teile = step.substring("SketchImageAdd:".length()).split(",");
				// Der Renderer wirft bei allem ausserhalb von -1..8 — das gehoert hierhin und nicht
				// mitten in eine Session. -1 ist der Leinwand-Modus von Goesch und Dreieck.
				int cell = Integer.parseInt(teile[1].trim());
				if (cell < -1 || cell > 8)
					throw new RuntimeException(step + ": Rasterfeld " + cell + " liegt ausserhalb von -1..8");
				available += areasOf("elements", teile[0]);
			}
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

	private int areasOf(String subfolder, String sketch) {
		return areas.computeIfAbsent(subfolder + "/" + sketch, key -> read(subfolder, sketch).size());
	}

	/**
	 * Die festen Farben einer Strukturdatei, in Flächenreihenfolge — leer, wenn sie keine trägt.
	 *
	 * <p>Ein {@code farbe} in den {@code properties} heißt: Diese Figur sieht immer gleich aus, ihre
	 * Farbe ist keine Frage. Der Union Jack ist der Fall — er wird gefärbt, sobald er auftaucht, und
	 * kommt in keiner Farbfrage vor. Die Suite liest die Eigenschaft nicht, sie ist eine Abmachung
	 * zwischen der Datei und diesem Generator.</p>
	 */
	private List<String> colorsOf(String subfolder, String sketch) {
		return sketchColors.computeIfAbsent(subfolder + "/" + sketch,
				key -> readColors(subfolder, sketch));
	}

	private static List<String> readColors(String subfolder, String sketch) {
		JsonNode features = tree(subfolder, sketch).get("features");
		List<String> result = new ArrayList<>();
		for (JsonNode feature : features) {
			JsonNode color = feature.path("properties").path("farbe");
			if (color.isMissingNode())
				continue;
			if (!COLORS.contains(color.asText()))
				throw new RuntimeException(sketch + ": '" + color.asText() + "' ist keine Skizzenfarbe");
			result.add(color.asText());
		}
		// Halb gefärbt gibt es nicht: Sonst hinge an einer vergessenen Zeile in der Datei, ob eine
		// Fläche gefragt wird oder nicht, und das sähe man erst mitten in einer Session.
		if (!result.isEmpty() && result.size() != features.size())
			throw new RuntimeException(sketch + ": " + result.size() + " Farben, aber "
					+ features.size() + " Flächen — entweder alle oder keine");
		return result;
	}

	/** Die Flächen einer Strukturdatei — gezählt wird nur, wie viele es sind. */
	private static List<double[]> read(String subfolder, String sketch) {
		List<double[]> result = new ArrayList<>();
		for (JsonNode feature : tree(subfolder, sketch).get("features")) {
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
	}

	private static JsonNode tree(String subfolder, String sketch) {
		Path file = SKETCHES.resolve(subfolder).resolve(sketch + ".geojson");
		try {
			return new ObjectMapper().readTree(file.toFile());
		} catch (IOException e) {
			throw new RuntimeException("Strukturdatei fehlt: " + file, e);
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
	 * Die richtige Antwort mit {@code +}, alle anderen nackt.
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
		return "MC:+" + correct + "|" + String.join("|", wrong);
	}

	/**
	 * Wie {@link #answer}, aber mit tolerierten Zweitantworten — ein Klick darauf gilt als falsch,
	 * bricht die Karte aber nicht ab.
	 *
	 * <p>Dafür muss die <b>Präfixform</b> her, die Altform kennt kein {@code ~}. Kein {@code =}
	 * dabei: Die Reihenfolge soll weiter gemischt werden, anders als bei {@link #fixedOrder}. Ohne
	 * Klammer bleibt es bei der Altform — es soll sich nur ändern, wo wirklich eine Toleranz steht.</p>
	 */
	private static String answer(String correct, List<String> tolerated, String... options) {
		if (tolerated.isEmpty())
			return answer(correct, options);
		List<String> alle = List.of(options);
		if (!alle.contains(correct))
			throw new IllegalArgumentException("Die richtige Antwort '" + correct
					+ "' steht nicht unter ihren Optionen: " + String.join("|", alle));
		for (String value : tolerated)
			if (!alle.contains(value))
				throw new IllegalArgumentException("Die tolerierte Antwort '" + value
						+ "' steht nicht unter ihren Optionen: " + String.join("|", alle));

		List<String> parts = new ArrayList<>();
		parts.add("+" + correct);
		for (String value : tolerated)
			if (!value.equals(correct))
				parts.add("~" + value);
		for (String option : options)
			if (!option.equals(correct) && !tolerated.contains(option))
				parts.add(option);
		return "MC:" + String.join("|", parts);
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
