package app.learn.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.shared.model.ShapeGeometry;

/**
 * Dünne Datenklasse: die Shapes einer Karte (reine Daten inkl. ShapeGeometry) plus der Kartentyp. Framework-frei.
 *
 * <p>{@link #geometryFor(Set)} ist die <b>eine</b> Stelle, an der ids in ihre framework-freien Geometrien
 * übersetzt werden — für alle id-Sorten einer Karte. Konsequent hier, weil {@link #getShape(String)} schon der
 * Ort ist, an dem man aus einer id ein Shape holt. Die Geometrie→Node-Umsetzung macht dagegen der MapNodeBuilder
 * (shared); diese Klasse baut nie einen Node.</p>
 */
public class GeoMap {

	private final Map<String, MapShape> shapes;
	private final MapType type;

	public GeoMap(List<MapShape> shapes, MapType type) {
		this.shapes = new HashMap<>();
		for (MapShape shape : shapes)
			this.shapes.put(shape.id(), shape);
		this.type = type;
	}

	public Collection<MapShape> getShapes() {
		return shapes.values();
	}

	/**
	 * Die Geometrien aller Shapes dieser Karte — der framework-freie Transport an die {@code ShapeMapPane}
	 * (shared). Jede Geometrie trägt id und (bei Shape-Karten) type; die Pane bzw. der MapNodeBuilder leiten
	 * zIndex/Layer-Klasse/interaktiv selbst daraus ab. Reihenfolge egal — die Pane sortiert nach zIndex.
	 */
	public List<ShapeGeometry> getShapeGeometries() {
		List<ShapeGeometry> result = new ArrayList<>();
		for (MapShape shape : shapes.values())
			result.add(shape.geometry());
		return result;
	}

	public Collection<String> getIds() {
		return shapes.keySet();
	}

	public MapType getType() {
		return type;
	}

	/** Liefert den Shape zur id oder wirft. Fabriziert nichts. */
	public MapShape getShape(String id) {
		MapShape shape = shapes.get(id);
		if (shape == null)
			throw new RuntimeException("Kein Shape für diese id: " + id);
		return shape;
	}

	/** Übersetzt eine Menge von ids in ihre Geometrien (jede trägt ihre id). Siehe {@link #geometryFor(String)}. */
	public List<ShapeGeometry> geometryFor(Set<String> ids) {
		List<ShapeGeometry> result = new ArrayList<>();
		for (String id : ids)
			result.add(geometryFor(id));
		return result;
	}

	/**
	 * Übersetzt eine id in ihre framework-freie Geometrie. Drei id-Sorten:
	 * <ul>
	 *   <li>echtes Shape ("Frankreich", "Donau") → Lookup, die Geometrie steht schon aus dem GeoJSON fest;</li>
	 *   <li>sichtbarer Kreis ("größe|x|y", z.B. Städte als Punkte) → CIRCLE, Radius aus CircleSizes;</li>
	 *   <li>Zentrier-Anker ("empty|x|y") → CENTER, kein sichtbares Shape; die Bild-Karte zentriert nur darauf.</li>
	 * </ul>
	 */
	private ShapeGeometry geometryFor(String id) {
		if (id.contains("|")) {
			String[] parts = id.split("\\|");
			CircleSizes size = CircleSizes.fromCsvName(parts[0]);
			int x = Integer.parseInt(parts[1]);
			int y = Integer.parseInt(parts[2]);
			if (size == CircleSizes.EMPTY)
				return ShapeGeometry.center(id, x, y);
			return ShapeGeometry.circle(id, x, y, size.getSize());
		}
		return getShape(id).geometry();
	}

	public void setShapes(List<MapShape> transformed) {
		this.shapes.clear();
		for (MapShape ms : transformed)
			this.shapes.put(ms.id(), ms);
	}
}