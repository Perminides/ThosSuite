package app.learn.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.learn.model.MapShape;
import app.shared.model.ShapeGeometry;

/**
 * Diese Klasse kennt nur das MapRepository!
 * Erzeugt aus einer geoJSON mit Multipolygons eine Liste von MapShapes.
 *
 * <p>Baut keine JavaFX-Nodes mehr — nur noch framework-freie {@link ShapeGeometry}. Die Y-Invertierung
 * (Bildschirm-Koordinaten) bleibt hier als reine Arithmetik; den Path/Group-Bau macht der MapNodeBuilder.</p>
 */
class GeoJsonFileSource {

	public List<MapShape> load(java.nio.file.Path filePath, boolean isShapeMap) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(filePath.toFile());
			return parseFeatures(root, isShapeMap);
		} catch (IOException e) {
			throw new RuntimeException("Fehler beim Laden von GeoJSON: " + filePath, e);
		}
	}

	private List<MapShape> parseFeatures(JsonNode root, boolean isShapeMap) {
		List<MapShape> shapes = new ArrayList<>();
		JsonNode features = root.get("features");

		for (JsonNode feature : features) {
			String id = feature.path("properties").path("id").asText(null);
			String capitalName = feature.path("properties").path("capitalName").asText(null);
			String regionName = feature.path("properties").path("regionName").asText(null);
			String shapeType = feature.path("properties").path("type").asText(null);
			String deckId = feature.path("properties").path("deckId").asText(null);
			String altCapitalNames = feature.path("properties").path("altCapitalNames").asText(null);
			String altRegionNames = feature.path("properties").path("altRegionNames").asText(null);

			JsonNode geometry = feature.get("geometry");
			String geometryType = geometry.get("type").asText();

			ShapeGeometry geo;
			if (isShapeMap) {
				geo = ShapeGeometry.shapePolygon(id, GeoJsonGeometry.parseMultiPolygon(geometry), shapeType);
			} else  if ("MultiPolygon".equals(geometryType)) {
				geo = ShapeGeometry.polygon(id, GeoJsonGeometry.parseMultiPolygon(geometry));
			} else if ("Polygon".equals(geometryType)) {
				geo = ShapeGeometry.polygon(id, GeoJsonGeometry.parsePolygon(geometry));
			} else if ("MultiLineString".equals(geometryType)) {
				geo = ShapeGeometry.line(id, GeoJsonGeometry.parseMultiLineString(geometry));
			} else {
				throw new RuntimeException("Unerwarteter Typ: " + geometryType);
			}

			shapes.add(new MapShape(geo, deckId, regionName, capitalName,
					altRegionNames, altCapitalNames));
		}
		return shapes;
	}
}