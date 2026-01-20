package app.data.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import app.config.Config;
import app.data.Deck;
import app.data.GeoMap;
import app.data.MapMetadata;
import app.data.MapShape;
import app.data.MapType;
import app.ui.skin.Skin;
import javafx.scene.image.Image;

/**
 * Sollte nur(!) vom MapService benutzt werden!
 */
public class MapRepository {
    private final GeoJsonLoader loader;
    
    public MapRepository() {
        this.loader = new GeoJsonLoader();
    }
    
    public GeoMap load(Deck type, Skin skin) {
        MapMetadata meta = type.getMapMetadata();
        
        if (meta.getMapType() == MapType.SHAPE) {
            // SHAPE MAP (Deutschland, Spanien)
        	String folder = Config.get("geoJsonFolder");
            String[] fileNames = meta.getGeoJsonFiles();
            List<MapShape> shapes = new ArrayList<>();
            for (String fileName : fileNames) {
                shapes.addAll(loader.load(folder + fileName, true));
            }
            return new GeoMap(shapes, MapType.SHAPE, null, null, null, null);
            
        } else {
            // IMAGE MAP (Welt, Hannover)
            try {
                // Bilder laden: JavaFX Image kann direkt vom Stream lesen
                Image bg = loadImage(skin.getMapImagePath(type));
                Image ov = loadImage(skin.getMapOverlayImagePath(type));
                Image bgia = loadImage(skin.getMapInactiveImagePath(type));
                Image ovia = loadImage(skin.getMapInactiveOverlayImagePath(type));
                
                String folder = Config.get("geoJsonFolder");
                String[] fileNames = meta.getGeoJsonFiles();
                List<MapShape> shapes = new ArrayList<>();
                for (String fileName : fileNames) {
                    shapes.addAll(loader.load(folder + fileName, false));
                }
                
                return new GeoMap(shapes, MapType.IMAGE, bg, ov, bgia, ovia);
                
            } catch (Exception e) {
                throw new RuntimeException("Probs beim Laden der MapBilder für " + type, e);
            }
        }
    }
    
    // Kleiner Helper, um null-Checks und Stream-Handling zu zentralisieren
    private Image loadImage(String path) throws IOException {
        if (path == null) return null;
        File file = new File(path);
        if (!file.exists()) return null;
        
        try (InputStream is = new FileInputStream(file)) {
            // Image(InputStream) lädt das Bild synchron (wichtig, damit es sofort da ist)
            // Wir könnten auch "file:..." URL nehmen, das wäre async, aber hier wollen wir sicher sein.
            return new Image(is);
        }
    }
}