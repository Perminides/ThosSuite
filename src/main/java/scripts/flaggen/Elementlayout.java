package scripts.flaggen;

import static scripts.flaggen.Flaggenkarte.NICHT_RECHTECKIG;
import static scripts.flaggen.Flaggenkarte.QUADRATISCH;
import static scripts.flaggen.Flaggenkarte.colors;
import static scripts.flaggen.Flaggenkarte.ordered;
import static scripts.flaggen.Zeile.untolerated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import scripts.flaggen.Flaggenkarte.Element;

/**
 * Wo und wie groß die Zusatzelemente gezeichnet werden: Datei, Rasterfeld, Größe und Versatz. Reine
 * Geometrie — gefragt und gefärbt wird in der {@link Flaggenkarte}.
 */
final class Elementlayout {

	private Elementlayout() {}

	/** Elementname → Datei. Der Stern hängt zusätzlich an der Anzahl, siehe {@link #sketchOf}. */
	static final Map<String, String> ELEMENT_FILES = ordered(
			"Kreis", "kreis", "Raute", "raute", "Schrift", "schrift", "Mond", "mond",
			"Hand", "hand", "Hacke", "hacke", "Klingenwaffe", "machete", "Zahnrad", "zahnrad", "Emblem", "emblem",
			"Vogel", "vogel", "Sonne", "sonne", "Union Jack", "union-jack",
			"Dreizack", "dreizack", "Muster", "muster", "Drache", "drache", "Blatt / Zweig",
			"zweig", "Kreuz", "kreuz", "Nuss", "nuss", "Blume", "blume",
			"Gebäude", "gebaeude", "Landumriss", "landumriss",
			"Hut", "hut", "Baum", "baum", "Krone", "krone", "Gewehr", "gewehr", "Buch", "buch",
			"Stoßzahn", "stosszahn", "Löwe", "loewe", "Chakra", "chakra", "Flagge", "flagge", "Kanu", "kanu");

	/** Das Rasterfeld als Behälter — der äußerste, den jedes Element durchläuft. */
	static final String SEGMENT = "Segment";

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
	static final Map<String, double[]> CONTAINERS = Map.of(
			SEGMENT, new double[] {0.8, 0.8, 0.56, 0.44},
			"Raute", new double[] {1.12, 1.12, 0.784, 0.616},
			"Kreis", new double[] {0.875, 0.625, 0.394, 0.309});

	/**
	 * Wo Elemente auf einer Skizze liegen, die ihre Leinwand nicht füllt: je Rasterfeld Größe und
	 * Versatz zur Feldmitte, in Leinwand-Einheiten, y nach oben positiv. Die Mitte von Feld 1 läge
	 * neben dem schmalen oberen Wimpel. Die Punkte hängen an der Skizze und der Antwort, nicht am
	 * Land: Es ist jeweils die Mitte des größten Kreises, der in den Wimpel passt.
	 */
	static final Map<String, Map<Integer, double[]>> ANKER = Map.of(
			NICHT_RECHTECKIG, Map.of(
					1, new double[] {0.7, -27.6, -18.4},
					7, new double[] {0.7, -21.7, 7.5}));

	/**
	 * Wo die Geschwister im Kasten ihres Behälters sitzen, je nach Anzahl. Keine Größen — Orte.
	 * Sie skalieren mit dem Kasten, also mit dem Faktor des Behälters.
	 */
	static final Map<Integer, double[]> OFFSETS = Map.of(
			1, new double[] {0},
			2, new double[] {-10, 10},
			3, new double[] {-20, 0, 20},
			4, new double[] {-22.5, -7.5, 7.5, 22.5});

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
	static final List<int[]> VERSTREUT = List.of(
			new int[] {0, 5, 7}, new int[] {1, 3, 8}, new int[] {2, 3, 7},
			new int[] {1, 5, 6}, new int[] {0, 5, 6}, new int[] {2, 4, 7});

	/**
	 * Dateiname und Platzierungen einer Figur: alles, was hinter {@code SketchImageAdd:} steht.
	 * Mehrere, wenn das Element verstreut ist — dann steht dieselbe Figur in drei Feldern.
	 */
	record Layout(Element element, String sketch, List<String> placements) {}

	/**
	 * Größe und Versatz je Element. Ein Element ohne Behälter füllt sein Feld; was in einem Behälter
	 * liegt, erbt dessen Faktor. Geschwister werden nebeneinandergelegt und dabei zur Mitte hin
	 * zusammengeschoben — im Behälter stärker als im freien Feld.
	 */
	static List<Layout> layout(List<Element> elements, int id, String background) {
		List<Layout> result = new ArrayList<>();
		Map<Integer, double[]> anker = ANKER.get(background);

		// Jedes Element haengt am letzten Behaelter davor, der im selben Feld liegt.
		int[] parent = new int[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			parent[i] = -1;
			for (int p = i - 1; p >= 0; p--)
				if (CONTAINERS.containsKey(elements.get(p).name())
						&& selbesFeld(elements.get(p), elements.get(i))) {
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

			if (anker != null) {
				String ort = untolerated(element.position());
				double[] a = ort.matches("\\d") ? anker.get(Integer.parseInt(ort)) : null;
				if (a == null || group.size() > 1 || parent[i] >= 0)
					throw new RuntimeException(background + " kennt für " + element.name() + " auf " + ort
							+ " keinen Platz — ANKER hat nur einzelne Figuren auf " + anker.keySet());
				result.add(new Layout(element, sketchOf(element, false),
						List.of(ort + "," + number(a[0]) + "," + number(a[1]) + "," + number(a[2]))));
				continue;
			}
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
			// Mehrere Felder im Ort heißt: eine Figur, die eine ganze Spalte oder Reihe besetzt —
			// Turkmenistans Ornamentband steht als 0|3|6. Gezeichnet wird sie in jedem genannten Feld.
			if (felder(element.position()).size() > 1) {
				List<String> placements = new ArrayList<>();
				for (String feld : felder(element.position()))   // aufsteigend, damit die Flächen von oben zählen
					placements.add(feld + "," + number(size));
				result.add(new Layout(element, sketchOf(element, false), placements));
				continue;
			}
			String placement = untolerated(element.position()) + "," + number(size);
			if (group.size() > 1)
				placement += "," + number(offset) + ",0";
			result.add(new Layout(element, sketchOf(element, false), List.of(placement)));
		}
		if (!background.endsWith(QUADRATISCH))
			return result;
		List<Layout> verschoben = new ArrayList<>();
		for (Layout layout : result)
			verschoben.add(new Layout(layout.element(), layout.sketch(),
					layout.placements().stream().map(Elementlayout::imQuadrat).toList()));
		return verschoben;
	}

	/**
	 * Eine Platzierung {@code feld,größe[,dx,dy]} auf dem Quadrat: Die äußeren Spalten rücken um ein
	 * Sechstel der Quadratbreite nach innen, auf die Mitte seiner Drittel (30+20 = 50, 150−20 = 130).
	 */
	static String imQuadrat(String placement) {
		String[] teile = placement.split(",");
		int spalte = Integer.parseInt(teile[0]) % 3;
		double dx = (teile.length > 2 ? Double.parseDouble(teile[2]) : 0) + (spalte == 0 ? 20 : spalte == 2 ? -20 : 0);
		double dy = teile.length > 3 ? Double.parseDouble(teile[3]) : 0;
		if (dx == 0 && dy == 0)
			return teile[0] + "," + teile[1];
		return teile[0] + "," + teile[1] + "," + number(dx) + "," + number(dy);
	}

	/** Die Figuren, die sich denselben Kasten teilen: gleicher Behälter, gleiches Rasterfeld. */
	static List<Integer> gruppe(List<Element> elements, int[] parent, int i) {
		List<Integer> result = new ArrayList<>();
		for (int j = 0; j < elements.size(); j++)
			if (parent[j] == parent[i] && selbesFeld(elements.get(j), elements.get(i)))
				result.add(j);
		return result;
	}

	/** Zwei Figuren stehen im selben Feld. Die Toleranzklammer zählt dafür nicht — sie ist kein Ort. */
	static boolean selbesFeld(Element a, Element b) {
		return untolerated(a.position()).equals(untolerated(b.position()));
	}

	/** Der Ortswert, der „über die ganze Flagge verteilt" bedeutet. */
	static final String VERSTREUT_ORT = "9";

	/** Die Rasterfelder, in denen schon etwas liegt — verstreute Elemente zählen nicht mit. */
	static Set<Integer> belegteFelder(List<Element> elements) {
		Set<Integer> belegt = new HashSet<>();
		for (Element element : elements)
			for (String ort : felder(element.position()))
				if (!ort.equals(VERSTREUT_ORT))
					belegt.add(Integer.parseInt(ort));
		return belegt;
	}

	/** Die Felder einer Ortsangabe, aufsteigend. Mehrere stehen mit {@code |} darin: {@code 0|3|6}. */
	static List<String> felder(String position) {
		List<String> result = new ArrayList<>(List.of(untolerated(position).split("\\|")));
		Collections.sort(result);
		return result;
	}

	/**
	 * Drei freie Felder aus {@link #VERSTREUT} — das erste Muster ab {@code id}, das kein belegtes
	 * Feld trifft. Passt keins, fliegt es: Bei höchstens drei belegten Feldern und sechs Mustern
	 * kommt das praktisch nicht vor, und wenn doch, will man ein Muster ergänzen statt still etwas
	 * Schlechteres zu malen.
	 */
	static int[] streufelder(int id, Set<Integer> belegt) {
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
	static String sketchOf(Element element, boolean einzeln) {
		if (element.name().equals("Stern")) {
			if (einzeln)
				return "stern";
			int count = FlagSheet.isSet(element.count()) ? Integer.parseInt(untolerated(element.count())) : 1;
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

	/** Zahlen kurz halten: 1 statt 1.0, 0.55 statt 0.5500000001. */
	static String number(double value) {
		String text = String.format(java.util.Locale.ROOT, "%.3f", value);
		text = text.replaceAll("0+$", "");
		return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
	}
}
