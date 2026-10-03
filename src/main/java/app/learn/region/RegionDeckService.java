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
import app.learn.model.MapShape;
import app.learn.region.model.RegionLearnSessionInfo;
import app.learn.region.model.Mode;
import app.learn.region.model.SessionSpec;
import app.learn.region.repository.DbRegionDeckProgressRepository;
import app.shared.AppClock;

/**
 * Hält alle RegionSets mit LearnStats.
 *
 * <p>Ob eine Deck-Modus-Kombination im Lernen auftaucht, entscheidet die Datenbank: ohne Zeile in
 * {@code region_learn_stat} liefert {@link #getDueGameInfos()} dafür nichts, und das gilt je Modus
 * einzeln. Das freie Spiel ist unberührt, {@code RegionPlaySetup} baut seine Auswahl aus
 * {@code Deck.values()}.</p>
 *
 * <p>Bei Anki ist es umgekehrt: eine Karte ohne Stand ist <em>neu</em> und kommt über das
 * Tagesbudget von selbst ins Lernen.</p>
 */
public class RegionDeckService {
	
	private final DbRegionDeckProgressRepository repo;
	
    private final Map<Deck, Set<MapShape>> regionCache = new HashMap<>();
    private final Map<Deck, Map<Mode, LearnStat>> statCache = new HashMap<>();

	/** Die Formen zu jedem Region-Deck und die Lernstände, die es dazu gibt. */
	public RegionDeckService() {
		this.repo = new DbRegionDeckProgressRepository();
		loadShapes();
		loadStats();
	}

	private void loadShapes() {
		MapService mapService = MapService.getInstance();
		for (Deck type : Deck.values())
			if (type.getCategory() == DeckCategory.REGION_DECK)
				regionCache.put(type, mapService.getPlayableShapesForDeck(type));
	}

	private void loadStats() {
		Map<Deck, Map<Mode, LearnStat>> stored = repo.loadAll();
		for (Deck type : Deck.values()) {
			if (type.getCategory() != DeckCategory.REGION_DECK)
				continue;

			Map<Mode, LearnStat> stats = stored.get(type);
			if (stats == null)
				stats = new HashMap<>(); // kein Lernstand gespeichert: dieses Deck ist nicht im Lernen
			statCache.put(type, stats);
		}
	}
	
	public List<LearnSessionInfo> getDueGameInfos() {
		ArrayList<LearnSessionInfo> result = new ArrayList<>();
		for (Deck type : Deck.values()) {
			if (type.getCategory() != DeckCategory.REGION_DECK)
				continue;
			for (Mode mode : Mode.values()) {
				SessionSpec spec = new SessionSpec(type, mode);
				LearnStat stat = statCache.get(type).get(mode);
				if (stat == null)
					continue; 
				if (stat.isDueToday() || stat.getLastPlayed().equals(AppClock.TODAY))
					result.add(new RegionLearnSessionInfo(spec, stat.getCurrentLevel(), stat.isDueToday()));
			}
		}
		return result;
	}

	public Set<MapShape> getRegions(SessionSpec spec) {
		Set<MapShape> result = new HashSet<>(regionCache.get(spec.getDeckType())); // Die Original-Sets bleiben HIER!
		// For play sessions more than one deck can be combined...
		if (spec.getAdditionalDeckTypesForFreePlay() != null) {
			for (Deck deckType : spec.getAdditionalDeckTypesForFreePlay()) {
				result.addAll(regionCache.get(deckType));
			}
		}
		return result;
	}
	
	public LearnStat getLearnStat(SessionSpec spec) {
		return statCache.get(spec.getDeckType()).get(spec.getMode());
	}
	
	public void savePlayedSession(SessionSpec spec, LearnStat stats, boolean correct, String incorrectId) {
		// Die Learnstats sind bereits aktualisiert.
		repo.save(spec, stats, correct, incorrectId);
		statCache.get(spec.getDeckType()).put(spec.getMode(), stats);
	}
}
