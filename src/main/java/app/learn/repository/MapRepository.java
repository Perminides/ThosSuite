package app.learn.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import app.learn.model.Deck;
import app.learn.model.GeoMap;
import app.learn.model.MapMetadata;
import app.learn.model.MapType;
import app.learn.model.ShapeMap;
import app.shared.Config;
import javafx.scene.image.Image;

/**
 * Sollte nur(!) vom MapService benutzt werden!
 */
public class MapRepository {
    private final GeoJsonLoader loader;
    
    public MapRepository() {
        this.loader = new GeoJsonLoader();
    }
    
    public GeoMap load(Deck type, Path bgImagePath, Path overlayPath, Path inactiveBgPath, Path inactiveOverlayPath) {
        MapMetadata meta = type.getMapMetadata();

        if (meta.getMapType() == MapType.SHAPE) {
            Path folder = Config.getPath("geoJsonFolder");
            String[] fileNames = meta.getGeoJsonFiles();
            List<ShapeMap> shapes = new ArrayList<>();
            for (String fileName : fileNames) {
                shapes.addAll(loader.load(folder.resolve(fileName), true));
            }
            return new GeoMap(shapes, MapType.SHAPE, null, null, null, null);

        } else {
            try {
                Image bg    = loadImage(bgImagePath);
                Image ov    = loadImage(overlayPath);
                Image bgia  = loadImage(inactiveBgPath);
                Image ovia  = loadImage(inactiveOverlayPath);

                Path folder = Config.getPath("geoJsonFolder");
                String[] fileNames = meta.getGeoJsonFiles();
                List<ShapeMap> shapes = new ArrayList<>();
                for (String fileName : fileNames) {
                    shapes.addAll(loader.load(folder.resolve(fileName), false));
                }
                return new GeoMap(shapes, MapType.IMAGE, bg, ov, bgia, ovia);

            } catch (Exception e) {
                throw new RuntimeException("Probs beim Laden der MapBilder für " + type, e);
            }
        }
    }
    
 // Kleiner Helper, um null-Checks und Stream-Handling zu zentralisieren
    private Image loadImage(Path path) throws IOException {
        if (path == null) return null;
        File file = path.toFile();
        if (!file.exists()) return null;

        try (InputStream is = new FileInputStream(file)) {
            // Image(InputStream) lädt das Bild synchron (wichtig, damit es sofort da ist)
            // Wir könnten auch "file:..." URL nehmen, das wäre async, aber hier wollen wir sicher sein.
            return new Image(is);
        }
    }
}