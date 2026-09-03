package app.shared.skin;

import java.util.ArrayList;
import java.util.List;

/**
 * Wie viel Platz ein Schatten neben seinem Baustein braucht, in Pixeln je Seite — in der Reihenfolge
 * der CSS-Schreibweise.
 *
 * <p>Der Hintergrund: {@code -fx-effect} gehört nicht zum Box-Modell. Der Effekt wird auf das fertig
 * gemalte Bild angewendet, <em>nachdem</em> das Layout durch ist, und geht in keine Größenrechnung
 * ein. Solange nichts clippt, stört das nicht — die meisten Container lassen Kinder über ihre
 * Grenzen hinausmalen. Der Viewport einer ScrollPane clippt aber, und zwar hart und ohne Handhabe.
 * Wer einen Schatten in eine ScrollPane setzt, muss ihm den Platz also selbst kaufen.</p>
 *
 * <p>Abgeleitet statt zweitem Feld: Ein Schattenwert und eine danebenstehende Platzangabe müssten
 * zueinander passen, und der Tag, an dem jemand den Radius ändert und die Zahl daneben vergisst,
 * kommt bestimmt. Hier gibt es nur eine Quelle.</p>
 */
record ShadowSpace(int top, int right, int bottom, int left) {

	private static final ShadowSpace KEINER = new ShadowSpace(0, 0, 0, 0);

	/**
	 * Liest den Platzbedarf aus einem CSS-Effekt, wie er in {@code componentShadow} steht.
	 *
	 * <p>Ein {@code dropshadow} reicht {@code radius} Pixel über den Rand hinaus, verschoben um den
	 * Versatz. {@code spread} verdichtet den Verlauf, dehnt ihn aber nicht — er bleibt draußen.</p>
	 *
	 * <p>Alles, was kein {@code dropshadow} ist, braucht keinen Platz: {@code null}, ein
	 * {@code innershadow} (der liegt innen), ein leerer Wert. Ein {@code dropshadow}, dessen Form
	 * nicht stimmt, fliegt dagegen — das ist ein kaputter Skin, kein Sonderfall.</p>
	 */
	static ShadowSpace of(String effect) {
		if (effect == null || !effect.trim().toLowerCase().startsWith("dropshadow("))
			return KEINER;

		String inner = effect.trim();
		inner = inner.substring(inner.indexOf('(') + 1, inner.lastIndexOf(')'));

		List<String> parts = splitOnTopLevel(inner);
		if (parts.size() != 6)
			throw new RuntimeException("dropshadow braucht sechs Werte (Weichzeichner, Farbe, Radius,"
					+ " Streuung, X-Versatz, Y-Versatz), gelesen habe ich " + parts.size() + ": " + effect);

		double radius = number(parts.get(2), effect);
		double offsetX = number(parts.get(4), effect);
		double offsetY = number(parts.get(5), effect);

		return new ShadowSpace(
				roundedUp(radius - offsetY),
				roundedUp(radius + offsetX),
				roundedUp(radius + offsetY),
				roundedUp(radius - offsetX));
	}

	/** Ob überhaupt Platz gebraucht wird — für Regeln, die es ohne Schatten gar nicht geben soll. */
	boolean isPresent() {
		return top > 0 || right > 0 || bottom > 0 || left > 0;
	}

	/**
	 * Zerlegt an Kommas, aber nur außerhalb von Klammern: Die Farbe bringt als {@code rgba(0,0,0,0.22)}
	 * eigene Kommas mit, und ein schlichtes {@code split(",")} zerlegte den Wert an der falschen Stelle.
	 */
	private static List<String> splitOnTopLevel(String inner) {
		List<String> parts = new ArrayList<>();
		int depth = 0;
		int start = 0;

		for (int i = 0; i < inner.length(); i++) {
			char character = inner.charAt(i);
			if (character == '(')
				depth++;
			else if (character == ')')
				depth--;
			else if (character == ',' && depth == 0) {
				parts.add(inner.substring(start, i));
				start = i + 1;
			}
		}
		parts.add(inner.substring(start));
		return parts;
	}

	private static double number(String value, String effect) {
		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException e) {
			throw new RuntimeException("Keine Zahl, wo im dropshadow eine stehen muss: '" + value.trim()
					+ "' in " + effect, e);
		}
	}

	/** Aufgerundet, damit der Platz nie knapp ausfällt; ein Versatz größer als der Radius ergibt null. */
	private static int roundedUp(double value) {
		return value <= 0 ? 0 : (int) Math.ceil(value);
	}
}
