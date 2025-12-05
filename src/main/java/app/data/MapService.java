package app.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import app.data.persistence.MapRepository;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;

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
    
    public GeoMap getMap(DeckType type) {
    	Skin skin = SkinService.get();
    	if (skin != skinForCaches) {
    		cache.entrySet().removeIf(e -> e.getValue().getType()==MapType.IMAGE);
    		skinForCaches = skin;
    	}
        return cache.computeIfAbsent(type.getMapMetadata(), _ -> repository.load(type, skin));
    }

    public Set<MapShape> getPlayableShapesForDeck(DeckType type) {
        GeoMap map = getMap(type);
        return map.getShapes().stream()
            .filter(shape -> type.getId().equals(shape.deckId()))
            .filter(shape -> shape.fixedColorSet() == null)  // nur spielbare
            .collect(Collectors.toSet());
    }
}