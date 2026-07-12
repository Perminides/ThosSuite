package app.learn.model;

/**
 * Größen der Kreis-Shapes. Kreise sind vollwertige Shapes (unsichtbare Trefferflächen, Marker), aber ihre
 * Definition kommt nicht aus GeoJSON, sondern direkt aus der id: {@code "größe|x|y"} (so in der CSV notiert).
 *
 * <p>Framework-frei — nur Name → Radius. Den Node baut daraus der MapNodeBuilder.</p>
 */
public enum CircleSizes {
	EMPTY("empty", 1),
	ULTRA_SMALL("ultra small", 4),
	SMALL("small", 10),
	MIDDLE_SMALL("middle small", 18),
	MIDDLE("middle", 25),
	MIDDLE_BIG("middle big", 37),
	BIG("big", 50),
	ULTRA_BIG("ultra big", 100);

	private final int size;
	private final String csvName;

	CircleSizes(String csvName, int size) {
		this.size = size;
		this.csvName = csvName;
	}

	public int getSize() {
		return size;
	}

	public static CircleSizes fromCsvName(String name) {
		for (CircleSizes cs : values())
			if (cs.csvName.equals(name))
				return cs;
		throw new RuntimeException("Unbekannte Kreis-Größe: " + name);
	}
}