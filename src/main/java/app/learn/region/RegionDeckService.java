package app.learn.region;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.learn.MapService;
import app.learn.model.Deck;
import app.learn.model.DeckCategory;
import app.learn.model.LearnSessionInfo;
import app.learn.model.LearnStat;
import app.learn.model.ShapeMap;
import app.learn.region.model.RegionLearnSessionInfo;
import app.learn.region.model.Mode;
import app.learn.region.model.SessionSpec;
import app.learn.region.repository.RegionDeckRepository;
import app.shared.AppClock;

/**
 * Hält alle RegionSets mit LearnStats
 */
public class RegionDeckService {
	
	private final RegionDeckRepository repo;
	
    private final Map<Deck, Set<ShapeMap>> regionCache = new HashMap<>();
    private final Map<Deck, Map<Mode, LearnStat>> statCache = new HashMap<>();

	/**
	 * Erstellt einen neuen RegionDeckService mit RegionSets und LearnStats von allen möglichen RegionSessions.
	 */
	public RegionDeckService() {
		MapService mapService = MapService.getInstance();
		this.repo = new RegionDeckRepository();
		for (Deck type : Deck.values()) {
			if (type.getCategory() != DeckCategory.REGION_DECK)
				continue;

			Set<ShapeMap> regions = mapService.getPlayableShapesForDeck(type);
			if (regions != null) {
				regionCache.put(type, regions);
			}
			
			for (Mode mode : Mode.values()) {
				SessionSpec spec = new SessionSpec(type, mode);
				LearnStat stat = repo.getLearnStat(spec);
				if (stat != null)
					statCache.computeIfAbsent(type, _ -> new HashMap<>())
			         .put(mode, stat);
			}
		}

	}
	
	public List<LearnSessionInfo> getDueGameInfos() {
		ArrayList<LearnSessionInfo> result = new ArrayList<>();
		for (Deck type : Deck.values()) {
			if (type.getCategory() != DeckCategory.REGION_DECK)
				continue;
			for (Mode mode : Mode.values()) {
				SessionSpec spec = new SessionSpec(type, mode);
				LearnStat stat = statCache.get(type) == null ? null : statCache.get(type).get(mode);
				if (stat == null)
					continue; 
				if (stat.isDueToday() || stat.getLastPlayed().equals(AppClock.TODAY))
					result.add(new RegionLearnSessionInfo(spec, stat.getCurrentLevel(), stat.isDueToday()));
			}
		}
		return result;
	}

	public Set<ShapeMap> getRegions(SessionSpec spec) {
		Set<ShapeMap> result = new HashSet<>(regionCache.get(spec.getDeckType())); // Die Original-Sets bleiben HIER!
		// For play sessions more than one deck can be combined...
		if (spec.getAdditonalDeckTypesForPlay() != null) {
			for (Deck deckType : spec.getAdditonalDeckTypesForPlay()) {
				result.addAll(regionCache.get(deckType));
			}
		}
		return result;
	}
	
	public LearnStat getLearnStat(SessionSpec spec) {
		return statCache.get(spec.getDeckType()).get(spec.getMode());
	}
	
	public void savePlayedCards(SessionSpec spec, LearnStat stats, boolean correct, String incorrectId) {
		// Die Learnstats sind bereits aktualisiert.
		repo.saveRegionSession(spec, stats, correct, incorrectId);
		statCache.get(spec.getDeckType()).put(spec.getMode(), stats);
	}
}
