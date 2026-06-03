package app.learn.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.learn.model.ShapeMap;
import javafx.scene.Group;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

/**
 * Diese Klasse kennt nur das MapRepository!
 * Erzeugt aus einer geoJSON mit Multipolygons eine Liste von MapShapes
 */
class GeoJsonLoader {
	public List<ShapeMap> load(String filePath, boolean isShapeMap) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(filePath));
            return parseFeatures(root, isShapeMap);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Laden von GeoJSON: " + filePath, e);
        }
    }

	private List<ShapeMap> parseFeatures(JsonNode root, boolean isShapeMap) {
	    List<ShapeMap> shapes = new ArrayList<>();
	    JsonNode features = root.get("features");

	    for (JsonNode feature : features) {
	        // Properties auslesen
	        String id = feature.path("properties").path("id").asText(null);
	        String capitalName = feature.path("properties").path("capitalName").asText(null);
	        String regionName = feature.path("properties").path("regionName").asText(null);
	        String shapeType = feature.path("properties").path("type").asText(null);
	        String deckId = feature.path("properties").path("deckId").asText(null);
	        String altCapitalNames = feature.path("properties").path("altCapitalNames").asText(null);
	        String altRegionNames = feature.path("properties").path("altRegionNames").asText(null);

	        JsonNode geometry = feature.get("geometry");
	        String geometryType = geometry.get("type").asText();

	        javafx.scene.Node shapeNode;

	        if ("MultiPolygon".equals(geometryType) || "Polygon".equals(geometryType)) {
	            // Parse Geometrie
	            Path geometryPath = new Path();
	            geometryPath.setFillRule(FillRule.EVEN_ODD);
	            
	            if ("MultiPolygon".equals(geometryType)) {
	                parseMultiPolygon(geometry, geometryPath);
	            } else {
	                parsePolygon(geometry, geometryPath);
	            }
	            
	            if (isShapeMap) {
	                // ShapeMap (Deutschland): Einzelner Path
	                geometryPath.setUserData(id);
	                geometryPath.setId("pathId-" + id);
	                shapeNode = geometryPath;
	                
	            } else {
	                // ImageMap (Welt): Group mit Fill + Border
	                
	                // Fill Path (unten)
	                Path firstPath = new Path();
	                firstPath.setFillRule(FillRule.EVEN_ODD);
	                firstPath.getElements().addAll(geometryPath.getElements());
	                firstPath.getStyleClass().add("first");
	                firstPath.setId("pathId-" + id + "-first");
	                
	                // Border Path (oben)
	                Path secondPath = new Path();
	                secondPath.setFillRule(FillRule.EVEN_ODD);
	                secondPath.getElements().addAll(geometryPath.getElements());
	                secondPath.getStyleClass().add("second");
	                secondPath.setId("pathId-" + id + "-second");
	                
	                // Group erstellen (Fill zuerst, dann Border!)
	                Group group = new Group(firstPath, secondPath);
	                group.setUserData(id);
	                group.setId("groupId-" + id);
	                
	                shapeNode = group;
	            }
	            
	        } else if ("MultiLineString".equals(geometryType)) {
	            // Parse LineString
	            Path linePath = new Path();
	            linePath.setFillRule(FillRule.EVEN_ODD);
	            parseMultiLineString(geometry, linePath);
	            
	            // Fill Path (gelbe Linie, unten)
	            Path firstPath = new Path();
	            firstPath.setFillRule(FillRule.EVEN_ODD);
	            firstPath.getElements().addAll(linePath.getElements());
	            firstPath.getStyleClass().add("first");
	            firstPath.setId("pathId-" + id + "-river-first");
	            
	            // Border Path (schwarzer Rahmen, oben)
	            Path secondPath = new Path();
	            secondPath.setFillRule(FillRule.EVEN_ODD);
	            secondPath.getElements().addAll(linePath.getElements());
	            secondPath.getStyleClass().add("second");
	            secondPath.setId("pathId-" + id + "-river-second");
	            
	            // Group erstellen
	            Group group = new Group(firstPath, secondPath);
	            group.getStyleClass().add("river");
	            group.setUserData(id);
	            group.setId("groupId-" + id);
	            
	            shapeNode = group;
	            
	        } else {
	            throw new RuntimeException("Unerwarteter Typ: " + geometryType);
	        }

	        shapes.add(new ShapeMap(shapeNode, id, deckId, regionName, capitalName, 
	                                altRegionNames, altCapitalNames, shapeType, isShapeMap));
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
    
    private void parseMultiLineString(JsonNode geometry, javafx.scene.shape.Path path) {
        JsonNode lines = geometry.get("coordinates");
        for (JsonNode coords : lines) {
            if (coords.size() == 0) continue;
            
            JsonNode firstPoint = coords.get(0);
            path.getElements().add(new MoveTo(
                firstPoint.get(0).asDouble(), 
                -firstPoint.get(1).asDouble()
            ));
            
            for (int i = 1; i < coords.size(); i++) {
                JsonNode pt = coords.get(i);
                path.getElements().add(new LineTo(
                    pt.get(0).asDouble(), 
                    -pt.get(1).asDouble()
                ));
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