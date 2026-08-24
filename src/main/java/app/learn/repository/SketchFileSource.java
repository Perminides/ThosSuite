package app.learn.repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.shared.Config;
import app.shared.model.ShapeGeometry;
import app.shared.model.ShapeGeometry.Point;

/**
 * Liest eine Strukturdatei und gibt ihre Teilflächen als {@link ShapeGeometry} zurück.
 *
 * <p>Dasselbe Format wie die Karten, aber ein viel schmalerer Ausschnitt davon: In den
 * {@code properties} steht nur die Flächennummer, keine Namen, kein Deck, kein Layer. Deshalb ein
 * eigener Leser statt einer Erweiterung des {@code GeoJsonLoader} — der ist auf Kartenbegriffe
 * gebaut, von denen hier keiner vorkommt.</p>
 *
 * <p>Geteilt bleibt die Konvention: Die Datei wird mit nach oben wachsendem Y gezeichnet, der
 * Bildschirm wächst nach unten, also wird Y beim Einlesen invertiert.</p>
 *
 * <p>Die Flächen tragen die Nummern {@code 0..n-1}, jede genau einmal. Das wird geprüft — eine
 * Strukturdatei mit Lücke oder Dublette wäre still falsch, weil die Karten ihre Flächen über die
 * Nummer ansprechen.</p>
 */
public class SketchFileSource {

	/**
	 * @param structure der Name der Struktur, ohne Endung (etwa {@code waagerecht-3})
	 */
	public List<ShapeGeometry> load(String structure) {
		Path file = Config.getPath("sketchFolder").resolve(structure + ".geojson");
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(file.toFile());
			List<ShapeGeometry> areas = parseFeatures(root, structure);
			checkAreaNumbers(areas, structure);
			return areas;
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Laden der Struktur: " + file, e);
		}
	}

	private List<ShapeGeometry> parseFeatures(JsonNode root, String structure) {
		List<ShapeGeometry> areas = new ArrayList<>();
		for (JsonNode feature : root.get("features")) {
			String number = feature.path("properties").path("id").asText(null);
			if (number == null)
				throw new RuntimeException("Fläche ohne id in der Struktur " + structure);

			JsonNode geometry = feature.get("geometry");
			String geometryType = geometry.get("type").asText();
			if ("MultiPolygon".equals(geometryType))
				areas.add(ShapeGeometry.polygon(number, parseMultiPolygon(geometry)));
			else if ("Polygon".equals(geometryType))
				areas.add(ShapeGeometry.polygon(number, parsePolygon(geometry)));
			else
				throw new RuntimeException("Eine Fläche ist kein Polygon, sondern " + geometryType
						+ " (Struktur " + structure + ")");
		}
		return areas;
	}

	private void checkAreaNumbers(List<ShapeGeometry> areas, String structure) {
		Set<Integer> seen = new HashSet<>();
		for (ShapeGeometry area : areas) {
			int number = Integer.parseInt(area.id());
			if (number < 0 || number >= areas.size())
				throw new RuntimeException("Fläche " + number + " liegt außerhalb von 0.."
						+ (areas.size() - 1) + " (Struktur " + structure + ")");
			if (!seen.add(number))
				throw new RuntimeException("Fläche " + number + " kommt doppelt vor (Struktur " + structure + ")");
		}
	}

	private List<List<Point>> parsePolygon(JsonNode geometry) {
		List<List<Point>> rings = new ArrayList<>();
		for (JsonNode ring : geometry.get("coordinates"))
			rings.add(parsePoints(ring));
		return rings;
	}

	private List<List<Point>> parseMultiPolygon(JsonNode geometry) {
		List<List<Point>> rings = new ArrayList<>();
		for (JsonNode polygon : geometry.get("coordinates"))
			for (JsonNode ring : polygon)
				rings.add(parsePoints(ring));
		return rings;
	}

	private List<Point> parsePoints(JsonNode ring) {
		List<Point> points = new ArrayList<>();
		for (JsonNode pt : ring)
			points.add(new Point(pt.get(0).asDouble(), -pt.get(1).asDouble()));
		return points;
	}
}
