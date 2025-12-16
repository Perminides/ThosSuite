package app.data.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.data.MapShape;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

/**
 * Diese Klasse kennt nur das MapRepository!
 * Erzeugt aus einer geoJSON mit Multipolygons eine Liste von MapShapes
 */
class GeoJsonLoader {
	public List<MapShape> load(String filePath) {
		System.out.println("Lade geojson: " + filePath);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(filePath));
            return parseFeatures(root);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Laden von GeoJSON: " + filePath, e);
        }
    }

    private List<MapShape> parseFeatures(JsonNode root) {
        List<MapShape> shapes = new ArrayList<>();
        JsonNode features = root.get("features");

        for (JsonNode feature : features) {
            // Properties auslesen (identisch zu vorher)
            String id = feature.path("properties").path("id").asText(null);
            String capitalName = feature.path("properties").path("capitalName").asText(null);
            String regionName = feature.path("properties").path("regionName").asText(null);
            String fixedColorId = feature.path("properties").path("fixedColorId").asText(null);
            String deckId = feature.path("properties").path("deckId").asText(null);
            String altCapitalNames = feature.path("properties").path("altCapitalNames").asText(null);
            String altRegionNames = feature.path("properties").path("altRegionNames").asText(null);

            JsonNode geometry = feature.get("geometry");
            String type = geometry.get("type").asText();

            Path path = new Path();
            
            // JavaFX Path Performance-Tipp:
            // "FillRule.EVEN_ODD" ist Standard für Geo-Daten (Löcher in Polygonen)
            path.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);

            if ("MultiPolygon".equals(type)) {
                parseMultiPolygon(geometry, path);
            } else if ("Polygon".equals(type)) {
                parsePolygon(geometry, path);
            } 
            // MultiLineString Logik ggf. analog ergänzen

            // WICHTIG: UserData setzen! Das brauchen wir für das Event-Handling (Klick -> ID)
            path.setUserData(id);
            path.setId("pathId-"+id); // Für debugging mit ScenicView
            
            // CSS Klasse initial setzen
            path.getStyleClass().add("map-shape");
            if (fixedColorId != null) {
                path.getStyleClass().add("decoration"); // Für Styling
            }

            shapes.add(new MapShape(path, id, deckId, regionName, capitalName, altRegionNames, altCapitalNames, fixedColorId));
        }
        return shapes;
    }

    private void parsePolygon(JsonNode geometry, Path path) {
        JsonNode coords = geometry.get("coordinates");
        for (JsonNode ring : coords) {
            appendRingToPath(ring, path);
        }
    }

    private void parseMultiPolygon(JsonNode geometry, Path path) {
        JsonNode coords = geometry.get("coordinates");
        for (JsonNode polygon : coords) {
            for (JsonNode ring : polygon) {
                appendRingToPath(ring, path);
            }
        }
    }

    private void appendRingToPath(JsonNode ring, Path path) {
        if (ring.size() == 0) return;

        // Erster Punkt -> MoveTo
        JsonNode firstPoint = ring.get(0);
        path.getElements().add(new MoveTo(
            firstPoint.get(0).asDouble(), 
            -firstPoint.get(1).asDouble() // Y-Invertierung beibehalten
        ));

        // Weitere Punkte -> LineTo
        for (int i = 1; i < ring.size(); i++) {
            JsonNode pt = ring.get(i);
            path.getElements().add(new LineTo(
                pt.get(0).asDouble(), 
                -pt.get(1).asDouble()
            ));
        }
        
        // Ring schließen -> ClosePath
        path.getElements().add(new ClosePath());
    }
}