package app.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.data.persistence.region.RegionDeckRepository;

/**
 * Hält alle RegionSets mit LearnStats
 */
public class RegionDeckService {
	
	private final RegionDeckRepository repo;
	
    private final Map<DeckType, Set<MapShape>> regionCache = new HashMap<>();
    private final Map<DeckType, Map<RegionMode, LearnStat>> statCache = new HashMap<>();

	/**
	 * Erstellt einen neuen RegionDeckService mit RegionSets und LearnStats von allen möglichen RegionSessions.
	 */
	public RegionDeckService() {
		MapService mapService = MapService.getInstance();
		this.repo = new RegionDeckRepository();
		for (DeckType type : DeckType.values()) {
			if (type.getCategory() != DeckCategory.REGION_DECK)
				continue;

			Set<MapShape> regions = mapService.getPlayableShapesForDeck(type);
			if (regions != null) {
				regionCache.put(type, regions);
			}
			
			for (RegionMode mode : RegionMode.values()) {
				RegionSessionSpec spec = new RegionSessionSpec(type, mode);
				LearnStat stat = repo.getLearnStat(spec);
				if (stat != null)
					statCache.computeIfAbsent(type, _ -> new HashMap<>())
			         .put(mode, stat);
			}
		}

	}
	
	public List<LearnSessionInfo> getDueGameInfos() {
		ArrayList<LearnSessionInfo> result = new ArrayList<>();
		for (DeckType type : DeckType.values()) {
			if (type.getCategory() != DeckCategory.REGION_DECK)
				continue;
			for (RegionMode mode : RegionMode.values()) {
				RegionSessionSpec spec = new RegionSessionSpec(type, mode);
				LearnStat stat = statCache.get(type) == null ? null : statCache.get(type).get(mode);
				if (stat == null)
					continue; 
				if (stat.isDueToday() || stat.getLastPlayed().equals(AppClock.TODAY))
					result.add(new RegionLearnSessionInfo(spec, stat.getCurrentLevel(), stat.isDueToday()));
			}
		}
		return result;
	}

	public Set<MapShape> getRegions(DeckType deckType) {
		return new HashSet<>(regionCache.get(deckType)); // Die Original-Sets bleiben HIER!
	}
	
	public LearnStat getLearnStat(RegionSessionSpec spec) {
		return statCache.get(spec.getDeckType()).get(spec.getMode());
	}
	
	public void savePlayedCards(RegionSessionSpec spec, LearnStat stats, boolean correct, String incorrectId) {
		// Die Learnstats sind bereits aktualisiert.
		repo.saveRegionSession(spec, stats, correct, incorrectId);
		statCache.get(spec.getDeckType()).put(spec.getMode(), stats);
	}
}
