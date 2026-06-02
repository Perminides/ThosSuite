package app.learn.anki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.learn.MapService;
import app.learn.anki.model.Card;
import app.learn.anki.model.AnkiLearnSessionInfo;
import app.learn.anki.repository.DeckRepository;
import app.learn.model.Deck;
import app.learn.model.DeckCategory;
import app.learn.model.LearnSessionInfo;
import app.shared.Config;

/**
 * Hält alle Karten aus allen Decks sowie die heute fälligen.
 */
public class AnkiDeckService {
			
	private final DeckRepository repo;
	private final Map<Deck,List<Card>> allCards;
	private final Map<Deck,Map<Integer, Card>> dueCards;
	private final Map<Deck,Integer> initialDueCounts;
	private final Map<Deck, Set<String>> allLabels;
	
	/**
	 * <p>
	 * Erstellt einen neuen AnkiDeckService mit Listen von allen und heute fälligen Cards.
	 * </p>
	 * 
	 * <p>
	 * Wir setzen erst einmal mehr auf Performance und laden deswegen alle Bilder hier sofort und halten sie. Ja, auch das fette Hannover-Bild. Wir haben
	 * gesehen, dass beim Laden des Bildes, was gute 400 MB benötigt, noch ein "Schatten" in den Heap geladen wird, der nochmal sogar etwas größer ist, dann
	 * aber nach dem Laden nicht mehr benötigt wird und freigegeben werden könnte. Wenn man hart System.gc(); aufruft, dann sinkt der Used um 600 MB. Was dieser
	 * Schatten ist, wissen wir nicht, aber ist vielleicht nicht so entscheidend. Hier nochmal die beiden relevanten Outputs meiner Speichermessung. Den Code
	 * dafür findest Du im MemoryTestHelloWorld...
	 * 
	 * </p>
	 * 
	 * <p>
	 * <ul>
	 * <li><b>Direkt nach der Initialisierung des AnkiDeckService</b></li>
	 * <li>12:18:11</li>
	 * <li>Heap</li>
	 * <li>Init: 536 MB</li>
	 * <li><b>Used: 1,3 GB</b></li>
	 * <li><b>Commited: 1,5 GB</b></li>
	 * <li>Max: 4,3 GB</li>
	 * <li>Non Heap</li>
	 * <li>Init: 7 MB</li>
	 * <li>Used: 30 MB</li>
	 * <li>Commited: 32 MB</li>
	 * <li>Max: -1 B</li>
	 * </ul>
	 * </p>
	 * 
	 * <p>
	 * <ul>
	 * <li><b>1 Sekunde nach Aufruf von System.gc();</b></li>
	 * <li>12:18:12</li>
	 * <li>Heap</li>
	 * <li>Init: 536 MB</li>
	 * <li><b>Used: 697 MB</b></li>
	 * <li><b>Commited: 1,5 GB</b></li>
	 * <li>Max: 4,3 GB</li>
	 * <li>Non Heap</li>
	 * <li>Init: 7 MB</li>
	 * <li>Used: 28 MB</li>
	 * <li>Commited: 32 MB</li>
	 * <li>Max: -1 B</li>
	 * </ul>
	 * </p>
	 * 
	 */
	public AnkiDeckService() {
		repo = new DeckRepository();
		allCards = new EnumMap<>(Deck.class);
		dueCards = new EnumMap<>(Deck.class);
		allLabels = new EnumMap<>(Deck.class);
		initialDueCounts = new EnumMap<>(Deck.class);
		for (Deck type : Deck.values()) {
			if (type.getCategory() != DeckCategory.ANKI_DECK)
				continue;
			
			//Preload Maps von Decks die heute fällig sind. !Später toggle im config berücksichtigen
			if (type.getMapMetadata() != null)
				MapService.getInstance().getMap(type);
						
			dueCards.put(type, new HashMap<>());
			allCards.put(type, repo.getAllHints(type));
			initialDueCounts.put(type, repo.getInitialDue(type));
			int newCounter = Integer.parseInt(Config.get(type.getConfigValueNewCards()));
			List<Card> newCards = new ArrayList<Card>();
			for (Card card : allCards.get(type)) {
				if (card.isDueToday())
					dueCards.get(type).put(card.getId(), card);
				else if (card.isNew() && newCounter > 0) {
					newCards.add(card);
					newCounter--;
				}
				allLabels.computeIfAbsent(type, _ -> new HashSet<>())
					.addAll(card.getLabels());
			}
			
			// !Später vielleicht etwas mehr sophisticated auch den Fall berücksichtigen, dass ich 2 neue Karten gelernt habe
			// und noch 3 neue übrig sind. Was ehrlich gesagt ein eher theoretischer Fall ist...
			if (dueCards.get(type).size() == initialDueCounts.get(type)) {
				for (Card card : newCards)
					dueCards.get(type).put(card.getId(), card);
				initialDueCounts.put(type, initialDueCounts.get(type) + newCards.size());
			}			
		}
		// Ach, was solls? Wir wollen den Schatten freigeben!
		System.gc();
	}
	
	public List<LearnSessionInfo> getDueGameInfos() {
		ArrayList<LearnSessionInfo> result = new ArrayList<>();
		for (Deck type : Deck.values()) {
			if (type.getCategory() != DeckCategory.ANKI_DECK)
				continue;
			
			int dueNow = dueCards.get(type).size();
			int dueToday = initialDueCounts.get(type);
			LearnSessionInfo info = new AnkiLearnSessionInfo(type, dueNow, dueToday);
			result.add(info);
		}
		return result;
	}
	
	public List<Card> getDueCards(Deck type) {
		return new ArrayList<Card>(dueCards.get(type).values());
	}

	public Set<String> getAvailableLabels(Deck type) {
	    return allLabels.get(type);
	}

	/**
	 * Liefert eine zufällige Liste von Karten für eine freie Spielrunde.
	 * @param type Das zu spielende Deck.
	 * @param minIndex Der kleinste erlaubte Karten-Index (inklusiv).
	 * @param maxIndex Der größte erlaubte Karten-Index (inklusiv).
	 * @param maxCards Maximale Anzahl an Karten.
	 * @param labelFilter Ein Set an Labels. Wenn nicht leer, muss die Karte mindestens eines dieser Labels haben (ODER-Verknüpfung).
	 * @return Eine gemischte Liste passender Karten.
	 */
	public List<Card> getCardsForPlay(Deck type, int minIndex, int maxIndex, int maxCards, Set<String> labelFilter) {
	    List<Card> candidates = new ArrayList<>();
	    
	    for (Card card : allCards.get(type)) {
	        // Index-Filter
	        if (card.getId() < minIndex || card.getId() > maxIndex) {
	            continue;
	        }
	        
	        // Label-Filter (ODER-Verknüpfung)
	        if (!labelFilter.isEmpty()) {
	            boolean hasMatchingLabel = false;
	            for (String label : card.getLabels()) {
	                if (labelFilter.contains(label)) {
	                    hasMatchingLabel = true;
	                    break;
	                }
	            }
	            if (!hasMatchingLabel) {
	                continue;
	            }
	        }
	        
	        candidates.add(card);
	    }
	    
	    // Mischen und limitieren
	    Collections.shuffle(candidates);
	    return candidates.subList(0, Math.min(maxCards, candidates.size()));
	}
	
	public void savePlayedCards(Deck type, List<Card> cards) {
		repo.savePlayedCards(type, cards);
		// Die Learnstats in den Karten sind bereits aktualisiert.
		Iterator<Card> iter = cards.iterator();
		while (iter.hasNext()) {
			Card card = iter.next();
			if (!card.isDueToday())
				dueCards.get(type).remove(card.getId());
		}
	}
	
}
