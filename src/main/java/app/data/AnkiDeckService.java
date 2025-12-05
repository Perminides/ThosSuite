package app.data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import app.config.Config;
import app.data.persistence.anki.AnkiDeckRepository;

/**
 * Hält alle Karten aus allen Decks sowie die heute fälligen.
 */
public class AnkiDeckService {
			
	private final AnkiDeckRepository repo;
	private final Map<DeckType,List<AnkiCard>> allCards;
	private final Map<DeckType,Map<Integer, AnkiCard>> dueCards;
	private final Map<DeckType,Integer> initialDueCounts;
	
	/**
	 * Erstellt einen neuen AnkiDeckService mit Listen von allen und heute fälligen Cards.
	 */
	public AnkiDeckService() {
		repo = new AnkiDeckRepository();
		allCards = new EnumMap<>(DeckType.class);
		dueCards = new EnumMap<>(DeckType.class);
		initialDueCounts = new EnumMap<>(DeckType.class);
		for (DeckType type : DeckType.values()) {
			if (type.getCategory() != DeckCategory.ANKI_DECK)
				continue;
			
			dueCards.put(type, new HashMap<>());
			allCards.put(type, repo.getAllHints(type));
			initialDueCounts.put(type, repo.getInitialDue(type));
			int newCounter = Integer.parseInt(Config.get(type.getConfigValueNewCards()));
			List<AnkiCard> newCards = new ArrayList<AnkiCard>();
			for (AnkiCard card : allCards.get(type)) {
				if (card.isDueToday())
					dueCards.get(type).put(card.getId(), card);
				else if (card.isNew() && newCounter > 0) {
					newCards.add(card);
					newCounter--;
				}
			}
			
			// !Später vielleicht etwas mehr sophisticated auch den Fall berücksichtigen, dass ich 2 neue Karten gelernt habe
			// und noch 3 neue übrig sind. Was ehrlich gesagt ein eher theoretischer Fall ist...
			if (dueCards.get(type).size() == initialDueCounts.get(type)) {
				for (AnkiCard card : newCards)
					dueCards.get(type).put(card.getId(), card);
				initialDueCounts.put(type, initialDueCounts.get(type) + newCards.size());
			}
			
			//Preload Maps von Decks die heute fällig sind. !Später toggle im config berücksichtigen
			if (type.getMapMetadata() != null)
				MapService.getInstance().getMap(type);
		}
	}
	
	public List<LearnSessionInfo> getDueGameInfos() {
		ArrayList<LearnSessionInfo> result = new ArrayList<>();
		for (DeckType type : DeckType.values()) {
			if (type.getCategory() != DeckCategory.ANKI_DECK)
				continue;
			
			int dueNow = dueCards.get(type).size();
			int dueToday = initialDueCounts.get(type);
			LearnSessionInfo info = new AnkiLearnSessionInfo(type, dueNow, dueToday);
			result.add(info);
		}
		return result;
	}
	
	public List<AnkiCard> getDueCards(DeckType type) {
		return new ArrayList<AnkiCard>(dueCards.get(type).values());
	}
	
	public void savePlayedCards(DeckType type, List<AnkiCard> cards) {
		repo.savePlayedCards(type, cards);
		// Die Learnstats in den Karten sind bereits aktualisiert.
		Iterator<AnkiCard> iter = cards.iterator();
		while (iter.hasNext()) {
			AnkiCard card = iter.next();
			if (!card.isDueToday())
				dueCards.get(type).remove(card.getId());
		}
	}
	
}
