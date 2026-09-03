package app.shared.model;

/**
 * Die Farben, mit denen sich eine Fläche einer Skizze füllen lässt.
 *
 * <p>Acht, und sie sind zugleich das Antwortvokabular der Karten, die eine Skizze aufbauen. Die
 * Werte dahinter stehen im Skin; hier steht nur der Name und die Style-Klasse, unter der die
 * Regel dazu läuft.</p>
 *
 * <p>Warum acht und nicht mehr: Jede weitere Farbe zieht eine neue Grenze, an der man sich
 * entscheiden muss — bei nur „Blau" liegt nichts dazwischen, erst ein zweiter Blauton macht
 * Kasachstan zu einer Frage. Türkis, Karmin, Braun und Grau werden deshalb eingefaltet.</p>
 */
public enum SketchColor {

	RED("Rot"),
	BLUE("Blau"),
	LIGHT_BLUE("Hellblau"),
	GREEN("Grün"),
	YELLOW("Gelb"),
	ORANGE("Orange"),
	WHITE("Weiß"),
	BLACK("Schwarz");

	private final String label;

	SketchColor(String label) {
		this.label = label;
	}

	/** Der geschriebene Name, wie er in der Deck-Datei steht. */
	public String label() {
		return label;
	}

	/** Die Klasse, unter der der Skin die Füllung dieser Farbe führt. */
	public String styleClass() {
		return "sketch-" + name().toLowerCase();
	}

	/**
	 * Die Farbe zu ihrem geschriebenen Namen, Groß- und Kleinschreibung egal.
	 *
	 * @throws RuntimeException wenn der Name zu keiner der acht gehört
	 */
	public static SketchColor fromLabel(String label) {
		for (SketchColor color : values())
			if (color.label.equalsIgnoreCase(label))
				return color;
		throw new RuntimeException("Keine Skizzenfarbe mit dem Namen: " + label);
	}
}
