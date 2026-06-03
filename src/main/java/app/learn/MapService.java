package app.learn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import app.learn.model.Deck;
import app.learn.model.GeoMap;
import app.learn.model.MapMetadata;
import app.learn.model.MapType;
import app.learn.model.ShapeMap;
import app.learn.repository.MapRepository;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;

/**
 * Implementiert als Singleton wegen des Cachings der Map-Daten. Da darf es natürlich nur einen geben.
 */
public class MapService {
    private static MapService instance;
    
    private final MapRepository repository;
    private final Map<MapMetadata, GeoMap> cache = new HashMap<>();
    private Skin skinForCaches;
    
    private MapService() {
        this.repository = new MapRepository();
    }
    
    public static MapService getInstance() {
        if (instance == null) {
            instance = new MapService();
        }
        return instance;
    }
    
    public GeoMap getMap(Deck type) {
        Skin skin = SkinService.get();
        if (skin != skinForCaches) {
            cache.entrySet().removeIf(e -> e.getValue().getType() == MapType.IMAGE);
            skinForCaches = skin;
        }
        return cache.computeIfAbsent(type.getMapMetadata(), _ -> repository.load(
                type,
                skin.getMapImagePath(type.getMapName()),
                skin.getMapOverlayImagePath(type.getMapName()),
                skin.getMapInactiveImagePath(type.getMapName()),
                skin.getMapInactiveOverlayImagePath(type.getMapName())
        ));
    }

    /**
     * Returns the mapShapes that are playable in this session
     * E.g. Only the shapes from Lower Saxony...
     * 
     * @param type
     * @return
     */
    public Set<ShapeMap> getPlayableShapesForDeck(Deck type) {
        GeoMap map = getMap(type);
        return map.getShapes().stream()
            .filter(shape -> type.getId().equals(shape.deckId()))
            .filter(shape -> shape.isInteractive())  // nur spielbare
            .collect(Collectors.toSet());
    }
}