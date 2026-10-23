package scripts.flaggen;

import static scripts.flaggen.Antworten.answer;
import static scripts.flaggen.Antworten.coded;
import static scripts.flaggen.Antworten.fixedOrder;
import static scripts.flaggen.Antworten.word;
import static scripts.flaggen.Elementlayout.CONTAINERS;
import static scripts.flaggen.Elementlayout.VERSTREUT_ORT;
import static scripts.flaggen.Elementlayout.felder;
import static scripts.flaggen.Elementlayout.layout;
import static scripts.flaggen.Zeile.bracket;
import static scripts.flaggen.Zeile.split;
import static scripts.flaggen.Zeile.tolerated;
import static scripts.flaggen.Zeile.untolerated;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import scripts.flaggen.Elementlayout.Layout;

/**
 * Die Flaggenkarte: aus den Attributen einer Zeile die Fragen zur Flagge und die Skizze, die dabei
 * entsteht — Form, Weiche, Hintergrund, Gösch, Dreieck, Rahmen, Zusatzelemente, Farben, zum Schluss
 * die echte Flagge.
 *
 * <p>Die ganze Ableitung — welche Frage wann kommt, welcher Sketch dazu gehört — steckt hier und nicht
 * im Anwendungscode. Deshalb stehen die Regeln als Tabellen am Kopf der Klasse und nicht als
 * {@code if} im Rumpf; wie groß ein Element gezeichnet wird, steht im {@link Elementlayout}.</p>
 */
final class Flaggenkarte {

	/** Index = Wert der Positionsspalte. Gefragt wird die Richtung vom Mittelpunkt, nicht ein Feld. */
	private static final List<String> POSITIONS = List.of(
			"Links oben vom Zentrum", "Oben vom Zentrum", "Rechts oben vom Zentrum",
			"Links vom Zentrum", "Zentriert", "Rechts vom Zentrum",
			"Links unten vom Zentrum", "Unten vom Zentrum", "Rechts unten vom Zentrum", "Verstreut");

	/** Die dritte Antwort der Kreuz-und-Diagonale-Frage. Steht dreimal, deshalb als Konstante. */
	// Das „nur" trägt die Rangfolge: Wer Diagonale oder Kreuz und darin ein Dreieck sieht, sieht nicht nur eins.
	// „Form" statt „Dreieck": Kuwaits Trapez und das Band der VAE gehören genauso hierher.
	private static final String FORM_VON_LINKS = "Nur eine Form von links";
	private static final String NICHTS_DAVON = "Nichts davon";
	// „Kein Viereck" statt „Nicht rechteckig": Ein Quadrat ist keins, damit waren beim Vatikan zwei
	// Antworten wahr. Nepal ist die einzige Flagge, die wirklich kein Viereck ist.
	private static final String RECHTECKIG = "Rechteckig";
	private static final String QUADRATISCH_ANTWORT = "Quadratisch";
	private static final String KEIN_VIERECK = "Kein Viereck";
	// „Durchgehend": Das Kreuz in Griechenlands Gösch ist keins, das die Flagge teilt.
	private static final String KREUZ_DURCHGEHEND = "Durchgehendes Kreuz";
	private static final String DIAGONALE_DURCHGEHEND = "Durchgehende Diagonale";
	private static final String GOESCH = "Gösch";
	// Eigene Antwort der Weiche statt einer Form von links: Der Streifen der ZAR kommt aus der Mitte.
	private static final String STREIFEN_MITTE = "Senkrechter Streifen in der Mitte";

	/** Index = Wert der Spalte „Form?". Wert 0 heißt „kein Dreieck". */
	private static final List<String> DREIECK_FORMEN = List.of(
			"Kein Dreieck",
			"Dreieck nur in der linken Hälfte",
			"Trapez",
			"Dreieck bis zum rechten Rand",
			"Liegendes Y",
			"Einfarbiger Streifen am Mast",
			"Gemusterter Streifen am Mast",
			STREIFEN_MITTE);

	/** Wie die Folgefragen die gewählte Form nennen, Index wie DREIECK_FORMEN: Werfall, dann Wenfall. */
	private static final String[] FORM_WER = {null, "das Dreieck", "das Trapez", "das Dreieck", "das Y",
			"der Streifen", "der gemusterte Streifen", "der senkrechte Streifen"};
	private static final String[] FORM_WEN = {null, "das Dreieck", "das Trapez", "das Dreieck", "das Y",
			"den Streifen", "den gemusterten Streifen", "den senkrechten Streifen"};

	/**
	 * Feste Anzeige-Reihenfolge der Formfrage: Werte 0, 1, 3, 2, 4, 5, 6. Die 7 fehlt, weil die
	 * Weiche sie selbst beantwortet — als Option wäre sie hier nie richtig.
	 */
	private static final List<String> DREIECK_ANZEIGE = List.of(
			DREIECK_FORMEN.get(0), DREIECK_FORMEN.get(1), DREIECK_FORMEN.get(3),
			DREIECK_FORMEN.get(2), DREIECK_FORMEN.get(4), DREIECK_FORMEN.get(5), DREIECK_FORMEN.get(6));

	/**
	 * Index = Wert der Spalte Hintergrundtyp. Die 6 ist bewusst frei, die 5 gibt es nicht mehr: Das
	 * Band am Mast ist eine Form von links ("Form?" = 5) über waagerechten Streifen.
	 */
	private static final Map<String, String> BACKGROUNDS = ordered(
			"0", "Waagerechte Streifen", "1", "Senkrechte Streifen", "2", "Kreuz mit vier Quadranten",
			"3", "Diagonale Teilung", "4", "Einfarbige Fläche", "7", "Anderes");

	/** Die Optionen der Fill-Frage — ohne Kreuz und Diagonale, die vorweg geklärt sind. */
	private static final List<String> FILL_BACKGROUNDS = List.of(
			"Waagerechte Streifen", "Senkrechte Streifen", "Einfarbige Fläche", "Anderes");

	/**
	 * Der Pool der Streifenzahlen: 2 bis 9 als Bereich — die 8 kommt nie vor und ist ein reiner
	 * Ablenker, damit eine falsche Vorstellung ausdrückbar bleibt — dazu die großen, die es
	 * wirklich gibt (Liberia, die USA, Malaysia).
	 */
	private static final List<String> STRIPE_COUNTS =
			List.of("2", "3", "4", "5", "6", "7", "8", "9", "11", "13", "14");

	/**
	 * Die Breiten-Abfolgen der fünf waagerechten Streifen, von oben nach unten. Der Wert der Spalte
	 * {@code 5W} steht 1:1 als Antwort und im Sketch-Namen; die Zahlen sind nur eine Rangfolge (3 > 2 > 1),
	 * gezeichnet wird nach Aussehen, siehe {@code GEZEICHNET} in build-streifen-sketch.py. Der Pool ist eindeutig — gleiche Abfolgen stehen nur einmal.
	 */
	private static final List<String> FIVE_WIDTHS = List.of(
			"3-1-2-1-3", "3-1-1-1-2", "1-1-2-1-1", "1-1-1-1-1", "1-2-3-2-1", "2-1-2-1-2", "2-1-3-1-2");

	/** Vorkommende Anzahlen — der Pool, aus dem die Ablenker der Anzahlfrage gezogen werden. */
	private static final List<String> COUNTS =
			List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "12", "15", "27", "50");

	/** Immer sichtbare Ablenker der Elementfrage — außer sie sind selbst die richtige Antwort. */
	private static final List<String> ELEMENT_PINNED = List.of("Keine", "Stern", "Emblem");

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
				// Ein Name für beides: Zyperns Ölzweige und Kanadas Ahornblatt sind in der Skizze
				// dasselbe Grün, die Unterscheidung wäre an der Flagge nicht zu sehen.
				{"Blatt / Zweig", new String[] {"das Blatt oder der Zweig", "die Blätter oder Zweige"}},
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
				// Schwert, Machete, Dolch: in Skizzengröße alles eine Klinge mit Griff.
				{"Klingenwaffe", new String[] {"die Klingenwaffe", "die Klingenwaffen"}},
				{"Gewehr", new String[] {"das Gewehr", "die Gewehre"}},
				{"Buch", new String[] {"das Buch", "die Bücher"}},
				{"Stoßzahn", new String[] {"der Stoßzahn", "die Stoßzähne"}},
				{"Flagge", new String[] {"die Flagge", "die Flaggen"}},
				{"Kanu", new String[] {"das Kanu", "die Kanus"}},
			});

	/**
	 * Anhang an den Hintergrund einer quadratischen Flagge. Die Rasterfelder bleiben die der
	 * Leinwand; die linke und rechte Spalte hätten ihre Mitte auf der Kante des Quadrats und rücken
	 * deshalb auf dessen Drittel ({@link #imQuadrat}).
	 */
	static final String QUADRATISCH = "-quadratisch";

	/** Index = Wert der Spalte Spezial. Kurz, weil man sie auf einen Blick lesen muss. */
	private static final String[] SPEZIAL_BESCHREIBUNGEN = {
			"2 Dreiecke, in der Mitte Meer",        // 0 Antigua und Barbuda
			"Gezackte senkrechte Linie",            // 1 Bahrain, Katar
			"Dreieck von oben",                     // 2 Bosnien und Herzegowina
			"Zwei Kästen. Der linke schmaler, der rechte breiter",   // 3 Sri Lanka
			"Wellen",                               // 4 Kiribati
			"Strahlen vom Mittelpunkt"};            // 5 Nordmazedonien

	/** Die Skizze für alles, was kein Rechteck ist — heute nur Nepal. */
	static final String NICHT_RECHTECKIG = "nicht-rechteckig";

	private final Skizzen skizzen;

	Flaggenkarte(Skizzen skizzen) {
		this.skizzen = skizzen;
	}

	// ---- Die Karte ------------------------------------------------------------

	/** Die Flaggenkarte von der Markierung bis zum Abspann. */
	void bauen(Zeile z, Kartenbauer k) {
		k.add("Mark:" + z.shape());

		k.ask("Welche Form hat die Flagge?",
				answer(rectangular(z.plain("Rechtwinklig?")),
						tolerated(z.value("Rechtwinklig?"), Flaggenkarte::rectangular),
						RECHTECKIG, KEIN_VIERECK, QUADRATISCH_ANTWORT));

		// Frage 2 ist eine Weiche: Kreuz, Diagonale und Dreieck von links gliedern die Flagge, ihre
		// Einzelheiten fragen erst die Folgefragen. Kreuz und Diagonale vorweg, sonst würde ihr linker
		// Arm mit einem Dreieck von links verwechselt.
		boolean nichtRechteckig = rectangular(z.plain("Rechtwinklig?")).equals(KEIN_VIERECK);
		boolean quadratisch = rectangular(z.plain("Rechtwinklig?")).equals(QUADRATISCH_ANTWORT);
		String typeCell = z.value("Hintergrundtyp");
		String type = untolerated(typeCell);
		// Der Zweig haengt am Typ, nicht am Antworttext: Sonst bricht ein Umbenennen der Option
		// still den Ablauf, statt nur die Anzeige zu aendern.
		if (type.equals("5"))
			throw new RuntimeException("Hintergrundtyp 5 gibt es nicht mehr: Das Band am Mast ist "
					+ "'Form?' = 5 über waagerechten Streifen (Typ 0)");
		boolean geteilt = type.equals("2") || type.equals("3");
		String dreieck = z.plain("Form?");
		int dreieckForm = FlagSheet.isSet(dreieck) ? Integer.parseInt(dreieck) : 0;
		// Bei einer Schräge ist die untere linke Hälfte selbst ein Dreieck von links. Ein
		// Sonderhintergrund bringt alles Dreieckige in seiner Datei mit, siehe unten.
		if (dreieckForm != 0 && (geteilt || type.equals("7")))
			throw new RuntimeException("Kreuz, Diagonale oder Sonderhintergrund mit einem Dreieck von links — "
					+ "Frage 2 kennt nur eins davon, das Dreieck würde still verschwinden");
		// Die Gösch ist die fünfte Antwort der Weiche: Sie kommt nie zusammen mit den anderen vor,
		// deshalb ersetzt die Weiche die eigene Göschfrage. Eine Toleranz hätte hier keinen Ort.
		boolean goesch = z.strict("Gösch?").equals("1");
		if (goesch && (geteilt || dreieckForm != 0 || type.equals("7")))
			throw new RuntimeException("Eine Gösch zusammen mit Kreuz, Diagonale, einer Form von links oder "
					+ "einem Sonderhintergrund — die Weiche kennt nur eins davon");
		String dreieckSketch = null;
		String background;
		if (nichtRechteckig) {
			// Jede weitere Frage setzt ein Rechteck voraus: Bänder von Rand zu Rand, Ecken für die
			// Gösch, ein Dreieck vom ganzen linken Rand. Die Formfrage davor macht sie überflüssig.
			background = NICHT_RECHTECKIG;
		} else {
			String weiche = type.equals("2") ? KREUZ_DURCHGEHEND : type.equals("3") ? DIAGONALE_DURCHGEHEND
					: dreieckForm == 7 ? STREIFEN_MITTE : dreieckForm != 0 ? FORM_VON_LINKS
					: goesch ? GOESCH : NICHTS_DAVON;
			k.ask("Welche dieser Formen hat die Flagge?", answer(weiche, KREUZ_DURCHGEHEND,
					DIAGONALE_DURCHGEHEND, FORM_VON_LINKS, STREIFEN_MITTE, GOESCH, NICHTS_DAVON));

			// Form 7 hat nichts nachzufragen: Es gibt nur einen Streifen in der Mitte, die Weiche nennt ihn.
			if (dreieckForm == 7) {
				if (!z.plain("Form aus wie vielen Farben?").equals("1"))
					throw new RuntimeException("Der senkrechte Streifen in der Mitte hat eine Farbe — dafür gibt "
							+ "es weder Frage noch Skizze");
				dreieckSketch = "dreieck-7";
			} else if (dreieckForm != 0) {
				// „Kein Dreieck" hat Frage 2 schon ausgeschlossen.
				k.ask("Welche Form kommt von links?",
						fixedOrder(DREIECK_FORMEN.get(dreieckForm), tolerated(z.value("Form?"),
								code -> DREIECK_FORMEN.get(Integer.parseInt(code))),
								DREIECK_ANZEIGE.subList(1, DREIECK_ANZEIGE.size()).toArray(new String[0])));
				String farbenSpalte = "Form aus wie vielen Farben?";
				String anzahl = z.plain(farbenSpalte);
				// Beide Streifen am Mast sind einfarbig; beim gemusterten trägt die eine Fläche das Muster
				// als Umriss, das Rot der Ornamente ist dasselbe wie im Hintergrund.
				if (dreieckForm == 5 || dreieckForm == 6) {
					if (!anzahl.equals("1"))
						throw new RuntimeException("Ein Streifen am Mast mit mehr als einer Farbe — dafür gibt es "
								+ "weder Frage noch Skizze");
				} else
					k.ask("Aus wie vielen Farben besteht " + FORM_WER[dreieckForm] + "?",
							fixedOrder(anzahl, tolerated(z.value(farbenSpalte), code -> code), "1", "2", "3", "4"));
				// Mehr als eine Farbe heißt eigene Datei mit einer Fläche je Farbe: dreieck-3-4.
				dreieckSketch = "dreieck-" + dreieckForm + (anzahl.equals("1") ? "" : "-" + anzahl);
			}

			// Reicht das Dreieck bis zum rechten Rand, verdeckt es jede Grenze des Hintergrunds — übrig
			// bleiben ein Feld oben und eins unten. Nach dem, was darunter läge, wird nicht gefragt.
			boolean bisZumRand = dreieckForm == 3 || dreieckForm == 4;
			if (bisZumRand) {
				background = "waagerecht-2";
			} else {
				if (!geteilt)
					k.ask("Entferne gedanklich " + (dreieckForm != 0 ? FORM_WEN[dreieckForm] + ", "
							: goesch ? "die Gösch, " : "") + "alle Zusatzelemente und einen Rahmen. "
							+ "Was beschreibt nun den Hintergrund am besten?",
							answer(BACKGROUNDS.get(type), backgroundTolerated(typeCell),
									FILL_BACKGROUNDS.toArray(new String[0])));
				branchQuestions(k, z, type);
				background = branchSketch(z, type);
			}
		}

		// Ein Quadrat ist die rechteckige Skizze, in x auf die Mitte der Leinwand gestaucht. Gösch,
		// Dreieck und Rahmen liegen in Leinwand-Koordinaten und passten nicht darauf.
		if (quadratisch) {
			if (dreieckSketch != null || z.plain("Gösch?").equals("1") || frame(z))
				throw new RuntimeException("Eine quadratische Flagge mit Dreieck, Gösch oder Rahmen — dafür gibt es "
						+ "noch keine Skizze, die Overlays lägen neben dem Quadrat");
			background += QUADRATISCH;
		}

		Canvas canvas = new Canvas(k);
		List<Fill> fills = new ArrayList<>();
		List<Integer> hintergrund = canvas.background(background);
		Farben hintergrundFarben = colors(z.value("Hintergrundfarben"));
		// Ein Hintergrund wird immer ganz gefärbt; eine graue Fläche wäre ein Versehen im Blatt.
		if (!hintergrundFarben.fuerAlleFlaechen() && hintergrundFarben.liste().size() != hintergrund.size())
			throw new RuntimeException(background + ": " + hintergrundFarben.liste().size() + " Farben ("
					+ String.join("|", hintergrundFarben.liste()) + "), aber " + hintergrund.size()
					+ " Flächen — ein Hintergrund braucht für jede Fläche eine Farbe");
		paint(background, fills, hintergrund, hintergrundFarben);
		// Gefragt ist das Dreieck schon, aufgelegt wird es erst auf den fertigen Hintergrund.
		if (dreieckSketch != null)
			paint(dreieckSketch, fills, canvas.overlay(dreieckSketch, "-1"), colors(z.value("Form Farbe")));

		// Ein Sonderhintergrund ist eine handgemachte Datei, die alles enthalten kann — auch eine
		// Gösch oder einen Rahmen. Ihn zusätzlich nach diesen Attributen zu fragen, führt
		// zwangsläufig in Widersprüche: Wer richtig hinsieht, bekäme falsch. Also nicht fragen.
		if (!nichtRechteckig && !type.equals("7")) {
			// Gösch auflegen (Leinwand-Silhouette, cell = -1); gefragt hat die Weiche.
			if (goesch)
				paint("goesch", fills, canvas.overlay("goesch", "-1"), colors(z.value("Gösch Farbe")));

			// Rahmen zuletzt fragen und auflegen: Er umfasst die Flagge und liegt am Rand obenauf.
			boolean rahmen = frame(z);
			k.ask("Hat die Flagge einen Rahmen?", answer(rahmen(z.plain("Rahmen?")),
					tolerated(z.value("Rahmen?"), Flaggenkarte::rahmen), "Ja, sie hat einen Rahmen", "Kein Rahmen"));
			if (rahmen)
				paint("rahmen", fills, canvas.overlay("rahmen", "-1"), colors(z.value("Rahmen Farbe")));
		}

		elementFills(k, canvas, elements(z), fills, z.id(), background);
		fillAreas(k, fills);
		k.add("Image:" + z.image());
		k.add("Output:<b>" + z.land() + "</b>");
		k.add("Pause:"); // Zeit, die echte Flagge anzusehen
		// Dasselbe noch einmal als Abspann: Auch wer die Karte reisst, soll die Flagge sehen.
		k.add("<OnFail>Image:" + z.image());
		k.add("Output:<b>" + z.land() + "</b>");
		k.add("Pause:");
		prependHint(k.steps(), z);
		hinweisVorweg(k.steps(), z);
	}

	/** Die Folgefragen des Zweigs — je Hintergrundtyp die Spalten, die er nach sich zieht. */
	private void branchQuestions(Kartenbauer k, Zeile z, String type) {
		switch (type) {
			case "0" -> {
				k.ask("Wie viele waagerechte Streifen?", fixedOrder(z.plain("W-Streifen"),
						tolerated(z.value("W-Streifen"), code -> code), STRIPE_COUNTS.toArray(new String[0])));
				if (z.plain("W-Streifen").equals("3"))
					k.ask("Wie breit sind die Streifen?", coded(z.value("3W"),
							"gleich breit", "mittlerer breiter", "mittlerer schmaler",
							"oberster breiter", "unterster breiter"));
				if (z.plain("W-Streifen").equals("5"))
					k.ask("Welche Abfolge beschreibt die Breite der Streifen von oben nach unten am besten?",
							answer(z.plain("5W"), tolerated(z.value("5W"), code -> code),
									FIVE_WIDTHS.toArray(new String[0])));
			}
			case "1" -> {
				k.ask("Wie viele senkrechte Streifen?",
						fixedOrder(z.plain("S-Streifen"), tolerated(z.value("S-Streifen"), code -> code),
								"2", "3", "4", "5"));
				k.ask("Wie breit sind die Streifen?", coded(z.value("S-Anordnung"),
						z.plain("S-Streifen").equals("2") ? List.of("mittlerer breiter") : List.of(),
						"gleich breit", "mittlerer breiter", "rechter breiter", "linker breiter"));
			}
			case "2" -> {
				k.ask("Welche Form hat das Kreuz?", coded(z.value("Kreuzausrichtung"),
						"senkrecht", "diagonal", "beides"));
				k.ask("Welche Form haben die Arme?", coded(z.value("Kreuzarme"),
						"uni", "drei parallele Farben", "fimbriert", "nicht sichtbar"));
			}
			case "3" -> {
				// Der Richtungscode traegt zwei Achsen: 0/1 parallel steigend/fallend, 2 strahlenfoermig steigend.
				String richtung = z.value("Diagonal Richtung");
				k.ask("Wie läuft die Diagonale bzw. laufen die Diagonalen?",
						coded(richtung, "steigend", "fallend", "steigend"));
				String baender = z.value("Diagonal Anzahl Streifen");
				k.ask("Wie viele diagonale Bänder laufen durch?", coded(baender,
						"kein Band, die Flächen stoßen aneinander", "1", "2", "3", "4", "5"));
				// Ein einzelnes Band ist im Blatt nie keilfoermig, die Frage unterschiede dort nichts.
				if (Integer.parseInt(untolerated(baender)) >= 2)
					k.ask("Wie laufen die Bänder?", coded(richtung,
							"parallel zueinander", "parallel zueinander", "strahlenförmig aus einer Ecke"));
			}
			// Ein Sonderhintergrund hat keine Folgefragen wie Streifenzahl oder Kreuzform — also eine
			// Beschreibung. Der Code ist die Nummer der Skizze, Bahrain und Katar teilen sich eine.
			case "7" -> k.ask("Was beschreibt den Hintergrund am besten?",
					coded(z.value("Spezial"), List.of(), SPEZIAL_BESCHREIBUNGEN));
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
	record Element(String name, List<String> tolerated, String position, String color,
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

	private Elemente elements(Zeile z) {
		List<Element> result = new ArrayList<>();
		List<String> toleriert = List.of();
		boolean keine = false;
		for (int slot = 1; slot <= 4; slot++) {
			String name = z.value("E" + slot);
			if (!FlagSheet.isSet(name))
				continue;
			String position = z.value("E" + slot + " Position");
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
					z.value("E" + slot + " Farbe"), z.value("E" + slot + " Anzahl")));
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
	private void elementFills(Kartenbauer k, Canvas canvas, Elemente elemente, List<Fill> fills, int id,
			String background) {
		List<Element> elements = elemente.liste();
		List<String> names = new ArrayList<>();
		for (Element element : elements)
			if (!names.contains(element.name()))
				names.add(element.name());
		k.add("Output:Welche Zusatzelemente siehst Du?");
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
				if (!WORDS.containsKey(name) && !name.equals(KEINE))
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
		// Ein fester Ablenker bleibt fest, auch wenn er toleriert ist: Doppelrolle -~.
		for (String name : tolerated)
			options.add((ELEMENT_PINNED.contains(name) ? "-~" : "~") + name);
		// Jeder vorkommende Elementname ist ein möglicher Ablenker, sonst verrät schon seine Anwesenheit
		// die Antwort. WORDS kennt sie alle — ohne Artikel gäbe es keine Anzahl- und keine Ortsfrage.
		List<String> pool = new ArrayList<>();
		pool.add(KEINE);
		pool.addAll(WORDS.keySet());
		for (String name : pool)
			if (!ELEMENT_PINNED.contains(name) && !vergeben.contains(name))
				options.add(name);
		k.add("MC+:" + String.join("|", options));

		// Erst alle Attribut-Fragen (Anzahl, geteilt) in einem Shuffle, dann alle Ortsfragen in einem
		// zweiten. So steht die Anzahl immer vor dem Ort (Numerus), und in keinem der Blöcke verrät die
		// Reihenfolge, welches Element im Blatt zuerst steht. Ein Element ohne Attributfrage taucht im
		// ersten Block gar nicht auf — ein leeres Segment würde die Input-Prüfung reißen.
		// Steht dasselbe Element mehrfach im Blatt (Panama: ein blauer und ein roter Stern), wird es
		// gefragt wie eine Figur mit Anzahl — eine Anzahl, ein Ort als Mehrfachauswahl. Sonst verrieten
		// die fehlende Anzahlfrage und die doppelte Ortsfrage, dass hier etwas anders ist. Gezeichnet
		// und gefärbt wird weiter jeder Eintrag für sich.
		Map<String, List<Element>> gleiche = new LinkedHashMap<>();
		for (Element element : elements)
			gleiche.computeIfAbsent(element.name(), name -> new ArrayList<>()).add(element);
		List<List<Element>> figuren = new ArrayList<>(gleiche.values());
		for (List<Element> figur : figuren)
			if (figur.size() > 1)
				pruefeMehrfach(figur);

		List<List<Element>> withAttribute = new ArrayList<>();
		for (List<Element> figur : figuren)
			if (figur.size() > 1 || FlagSheet.isSet(figur.get(0).count()) || figur.get(0).name().equals("Kreis"))
				withAttribute.add(figur);
		k.shuffled(withAttribute, figur -> {
			Element element = figur.get(0);
			if (figur.size() > 1)
				k.ask("Wie viele " + WORDS.get(element.name())[1].substring(4) + "?",
						fixedOrder(String.valueOf(figur.size()), COUNTS.toArray(new String[0])));
			else if (FlagSheet.isSet(element.count()))
				k.ask("Wie viele " + WORDS.get(element.name())[1].substring(4) + "?",
						fixedOrder(untolerated(element.count()), tolerated(element.count(), code -> code),
								COUNTS.toArray(new String[0])));
			// Bei jedem Kreis gleich gefragt — die konstante Frage leakt nichts und stoppt die stille
			// Annahme "ungeteilt". Geteilt ist er genau dann, wenn zwei Farben im Blatt stehen.
			if (element.name().equals("Kreis"))
				k.ask("Ist der Kreis geteilt?",
						answer(colors(element.color()).liste().size() == 2 ? "Ja, der Kreis ist geteilt" : "Ungeteilter Kreis",
						"Ja, der Kreis ist geteilt", "Ungeteilter Kreis"));
		});

		k.shuffled(figuren, figur -> {
			Element element = figur.get(0);
			if (figur.size() > 1)
				k.ask("Wo liegen " + WORDS.get(element.name())[1] + "?", positionen(figur));
			else
				k.ask("Wo " + verb(element) + " " + word(element) + "?", position(element));
		});

		for (Layout layout : layout(elements, id, background)) {
			List<Integer> areas = new ArrayList<>();
			for (String placement : layout.placements())
				areas.addAll(canvas.overlay(layout.sketch(), placement));
			if (paintFixed(k, layout, areas))
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
	private boolean paintFixed(Kartenbauer k, Layout layout, List<Integer> areas) {
		List<String> fixed = skizzen.colorsOf("elements", layout.sketch());
		if (fixed.isEmpty())
			return false;
		if (FlagSheet.isSet(layout.element().color()))
			throw new RuntimeException(layout.sketch() + " trägt seine Farben selbst, im Blatt steht"
					+ " trotzdem eine: " + layout.element().color());
		for (int i = 0; i < fixed.size(); i++) {
			List<Integer> gleiche = new ArrayList<>();
			for (int j = i; j < areas.size(); j += fixed.size())
				gleiche.add(areas.get(j));
			k.add("SketchImageFill:" + areaList(gleiche) + "," + fixed.get(i));
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
		// Eine Figur über mehrere Felder wird gefragt wie mehrere Figuren: alle Felder sind richtig.
		if (felder(element.position()).size() > 1)
			return positionen(List.of(element));
		List<String> tolerated = new ArrayList<>();
		for (String value : bracket(element.position()))
			tolerated.add(POSITIONS.get(Integer.parseInt(value)));
		return fixedOrder(POSITIONS.get(Integer.parseInt(untolerated(element.position()))), tolerated,
				POSITIONS.toArray(new String[0]));
	}

	/**
	 * Die Ortsfrage für dasselbe Element an mehreren Stellen: eine Mehrfachauswahl in fester
	 * Reihenfolge, jede Stelle ist richtig, ihre Toleranzklammern gelten gemeinsam.
	 */
	private static String positionen(List<Element> figur) {
		List<String> richtig = new ArrayList<>();
		List<String> geduldet = new ArrayList<>();
		for (Element element : figur) {
			for (String feld : felder(element.position()))
				richtig.add(POSITIONS.get(Integer.parseInt(feld)));
			for (String value : bracket(element.position()))
				geduldet.add(POSITIONS.get(Integer.parseInt(value)));
		}
		List<String> parts = new ArrayList<>();
		for (String ort : POSITIONS)
			parts.add(richtig.contains(ort) ? "+" + ort : geduldet.contains(ort) ? "~" + ort : ort);
		return "MC+:=" + String.join("|", parts);
	}

	/**
	 * Mehrfach dasselbe Element geht nur als einzelne Figuren an verschiedenen Orten: ohne eigene
	 * Anzahl, nicht verstreut, kein Behälter. Alles andere hätte keine eindeutige Frage.
	 */
	private static void pruefeMehrfach(List<Element> figur) {
		Set<String> orte = new HashSet<>();
		for (Element element : figur) {
			String ort = untolerated(element.position());
			if (CONTAINERS.containsKey(element.name()) || ort.equals(VERSTREUT_ORT)
					|| (FlagSheet.isSet(element.count()) && !untolerated(element.count()).equals("1"))
					|| !orte.add(ort))
				throw new RuntimeException(element.name() + " steht mehrfach im Blatt — das geht nur als einzelne "
						+ "Figuren an verschiedenen Orten, ohne Anzahl, nicht verstreut und nicht als Behälter");
		}
	}

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

		private final Kartenbauer k;
		private int next;

		private Canvas(Kartenbauer k) {
			this.k = k;
		}

		/** Der Hintergrund. Er setzt die Leinwand zurück, gezählt wird wieder ab 0. */
		private List<Integer> background(String sketch) {
			k.add("SketchImage:" + sketch);
			next = 0;
			return claim(skizzen.areasOf("backgrounds", sketch));
		}

		/** Eine Silhouette obendrauf. {@code placement} ist alles hinter dem Dateinamen. */
		private List<Integer> overlay(String sketch, String placement) {
			k.add("SketchImageAdd:" + sketch + "," + placement);
			return claim(skizzen.areasOf("elements", sketch));
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
	record Farben(List<String> liste, boolean fuerAlleFlaechen) {}

	/**
	 * Die Farben einer Zelle. Leer und {@code x} heißen beide: keine Farbe.
	 *
	 * <p>Ein führendes {@code &} heißt: Die eine Farbe gilt dem <b>ganzen Element</b> und nicht
	 * seiner ersten Fläche. Das Emblem besteht aus zwei Flächen, die zusammen ein Wappen ergeben —
	 * gefragt wird es als Ganzes. Die Entscheidung trifft das Blatt, nicht der Generator; er sieht
	 * das Zeichen und gehorcht.</p>
	 */
	static Farben colors(String cell) {
		if (!FlagSheet.isSet(cell))
			return new Farben(List.of(), false);
		boolean fuerAlle = cell.startsWith("&");
		List<String> liste = split(fuerAlle ? cell.substring(1) : cell);
		if (fuerAlle && liste.size() != 1)
			throw new RuntimeException("'&' meint eine Farbe fürs ganze Element, hier stehen "
					+ liste.size() + ": " + cell);
		return new Farben(liste, fuerAlle);
	}

	// ---- Skizze und Farben ----------------------------------------------------

	/**
	 * Der reine Hintergrund-Dateiname aus den Attributen des Zweigs — wie {@code waagerecht-3}, nur mit
	 * Wörtern. Gösch und Dreieck stehen <b>nicht</b> im Namen: sie werden als Silhouetten aus
	 * {@code elements/} zur Laufzeit oben aufgelegt ({@code SketchImageAdd}), nicht eingebacken.
	 */
	private String branchSketch(Zeile z, String type) {
		return switch (type) {
			// Die Verteilung gehoert in den Namen: Sie veraendert die Breiten, und eine Skizze mit
			// gleichen Streifen widerspraeche der Antwort "Mitte breiter". Gefragt wird sie nur bei
			// drei und fuenf waagerechten Streifen -- sonst gibt es keinen Wert und keinen Zusatz.
			case "0" -> "waagerecht-" + z.plain("W-Streifen")
					+ (z.plain("W-Streifen").equals("3") ? "-" + z.plain("3W") : "")
					+ (z.plain("W-Streifen").equals("5") ? "-" + z.plain("5W") : "");
			case "1" -> "senkrecht-" + z.plain("S-Streifen") + "-" + z.plain("S-Anordnung");
			case "2" -> "kreuz-" + Antworten.word(z.plain("Kreuzausrichtung"), "senkrecht", "diagonal", "beides")
					+ "-" + Antworten.word(z.plain("Kreuzarme"), "uni", "dreifarbig", "fimbriert", "unsichtbar");
			case "3" -> "diagonal-" + Antworten.word(z.plain("Diagonal Richtung"), "steigend", "fallend", "faecher")
					+ "-" + z.plain("Diagonal Anzahl Streifen");
			case "4" -> "uni";
			case "7" -> "spezial-" + z.strict("Spezial");
			default -> throw new RuntimeException("Kein Sketch-Name für Hintergrundtyp " + type);
		};
	}

	/**
	 * Je Fläche eine Farbfrage, in zufälliger Reihenfolge. Bei einer einzigen Fläche gibt es nichts
	 * zu mischen — dann bleibt die Klammer weg.
	 */
	private void fillAreas(Kartenbauer k, List<Fill> fills) {
		if (fills.isEmpty())
			return;
		k.add("Output:Welche Farbe hat die schraffierte Fläche?");
		k.shuffled(fills, fill -> {
			// Die Zelle darf eine Toleranzklammer tragen: `Hellblau (Blau)`. Gemalt wird die Farbe
			// davor — SketchColor kennt den Klammertext nicht.
			String farbe = untolerated(fill.color());
			k.add("SketchImageMark:" + areaList(fill.areas()));
			k.add(answer(farbe, bracket(fill.color()), Skizzen.COLORS.toArray(new String[0])));
			k.add("SketchImageFill:" + areaList(fill.areas()) + "," + farbe);
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

	/** Nepal ist als einzige Flagge kein Viereck, die Schweiz und der Vatikan sind quadratisch. */
	private static String rectangular(String code) {
		return switch (code) {
			case "1" -> RECHTECKIG;
			case "2" -> QUADRATISCH_ANTWORT;
			default -> KEIN_VIERECK;
		};
	}

	private boolean frame(Zeile z) {
		return z.plain("Rahmen?").equals("1");
	}

	private static String rahmen(String code) {
		return code.equals("1") ? "Ja, sie hat einen Rahmen" : "Kein Rahmen";
	}

	/**
	 * Der Hinweis, sonst leer. Er trennt zwei Flaggen desselben Landes („Flagge bis 2021"). Zwei
	 * Umbrüche dahinter setzen ihn als eigenen Absatz ab; sie stehen als {@code <br />} da, weil in
	 * eine Deck-Zelle kein echter Zeilenumbruch passt — das Fragefeld versteht {@code <br />},
	 * {@code <b>} und {@code <i>}.
	 */
	private String hint(Zeile z) {
		String text = z.value("Hinweistext");
		return FlagSheet.isSet(text) ? "<i>" + text + "</i><br /><br />" : "";
	}

	/**
	 * Der Hinweis als eigener erster Schritt, mit einem Knopf zum Weitergehen.
	 *
	 * <p>Über den Fragen steht er weiter (siehe {@link #prependHint}), aber wer die Fragen auswendig
	 * kennt, liest sie nicht mehr und übersieht ihn dort. Als eigener Schritt muss er einmal
	 * weggeklickt werden. Läuft nach {@code prependHint}, sonst stünde der Hinweis doppelt da.</p>
	 */
	private void hinweisVorweg(List<String> steps, Zeile z) {
		String hinweis = hint(z);
		if (hinweis.isEmpty())
			return;
		int stelle = steps.get(3).startsWith("Mark:") ? 4 : 3;   // erst das Land markieren, dann der Hinweis
		steps.add(stelle, "MC:+Okay");             // ohne '+' wäre Okay ein Füller und damit falsch
		steps.add(stelle, "Output:" + hinweis.replace("<br /><br />", ""));
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
	private void prependHint(List<String> steps, Zeile z) {
		String hint = hint(z);
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
		return FlagSheet.isSet(element.count()) && !untolerated(element.count()).equals("1");
	}

	static Map<String, String> ordered(String... pairs) {
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
