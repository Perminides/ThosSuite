package app.shared.ui.components.map;

/**
 * Die <b>Darstellungs-Ableitung</b> aus dem rohen GeoJSON-{@code type} ("0".."3") einer Shape-Karte:
 * zIndex (Zeichenreihenfolge), CSS-Layer-Klasse und "Klicks registrieren?". Alles drei ist Node-Bau-Wissen und
 * wohnt darum hier in shared, package-private neben ihren beiden einzigen Nutzern: {@link MapNodeBuilder}
 * (styleClass, mouseTransparent) und {@code ShapeMapPane} (zIndex, Klick-Verdrahtung).
 *
 * <p>Bewusst getrennt von der <b>Lernstoff</b>-Bedeutung desselben {@code type}: die leitet learn eigenständig
 * über {@code MapShape.isPlayable()} ab. Zwei unabhängige Ableitungen aus einer Quelle, jede auf ihrer Seite —
 * früher fielen sie im überladenen {@code isInteractive} zusammen. Der doppelte {@code fromJsonType}-Lookup
 * (Pane + Builder) ist bewusst in Kauf genommen; ein 4-Werte-Enum-Scan ist vernachlässigbar.</p>
 */
enum ShapeLayer {
	// Definition: (GeoJsonId, Z-Index, Css-Klasse, Interaktiv?)
	INTERACTIVE("0", 0, "layer-region", true),
	NEIGHBOR("1", 10, "layer-neighbor", false),
	WATER("2", 20, "layer-water", false),
	OVERLAY("3", 30, "layer-overlay", false); // z.B. Bundeslandgrenzen

	private final String type;
	private final int zIndex;
	private final String styleClass;
	private final boolean interactive;

	private ShapeLayer(String type, int zIndex, String styleClass, boolean interactive) {
		this.type = type;
		this.zIndex = zIndex;
		this.styleClass = styleClass;
		this.interactive = interactive;
	}

	int zIndex() { return zIndex; }
	String styleClass() { return styleClass; }
	boolean interactive() { return interactive; }

	static ShapeLayer fromJsonType(String type) {
		for (ShapeLayer layer : values()) {
			if (type != null && type.equals(layer.type)) return layer;
		}
		throw new RuntimeException("Unerwarteter Value im Typen eines Shapes: " + type);
	}
}