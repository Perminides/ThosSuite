package app.learn.anki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.learn.MapService;
import app.learn.anki.model.Card;
import app.learn.anki.model.AnkiLearnSessionInfo;
import app.learn.anki.repository.DeckRepository;
import app.learn.anki.repository.PlayedCardData;
import app.learn.model.Deck;
import app.learn.model.MapType;
import app.learn.model.DeckCategory;
import app.learn.model.LearnSessionInfo;
import app.shared.Config;

/**
 * Lädt im Konstruktor die Karten-Shapes, die Anki-Karten und deren Fortschritte.
 * Hält alle Karten aus allen Decks sowie die heute fälligen.
 */
public class AnkiDeckService {
			
	private final DeckRepository repo;
	private final Map<Deck,List<Card>> allCards;
	private final Map<Deck,Map<Integer, Card>> dueCards;
	private final Map<Deck,Integer> initialDueCounts;
	private final Map<Deck, Set<String>> allLabels;
	
	/** Kartennamen der Bild-Decks — der Controller lässt die Skin-Seite deren Bilder vorwärmen. */
	private final List<String> imageMapNames = new ArrayList<>();

	/**
	 * Lädt alle Karten aller Anki-Decks samt Fortschritt und hält sie — nicht nur die heute
	 * fälligen. Bewusst alles im Voraus, damit später nichts nachgeladen werden muss.
	 *
	 * <p>Je Deck steht danach fest, was heute dran ist: die fälligen Karten plus so viele neue, wie
	 * das Tagesbudget aus der Config noch zulässt.</p>
	 *
	 * <p>Bilder fasst er nicht an. Die Bild-Decks nennt er nur beim Namen — sie vorzuwärmen ist
	 * Sache der Skin-Seite, angestoßen vom Controller.</p>
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
			
			preloadMap(type);

			dueCards.put(type, new HashMap<>());
			allCards.put(type, repo.getAllCards(type));
			initialDueCounts.put(type, repo.getInitialDue(type));
			// Das Tagesbudget für neue Karten, abzüglich dessen, was heute schon verbraucht ist.
			// Dadurch rutschen neue Karten auch dann noch nach, wenn der heutige Stapel längst
			// begonnen oder abgearbeitet ist — und trotzdem nie mehr als die Config erlaubt.
			int budget = Integer.parseInt(Config.get(type.getConfigValueNewCards()))
					- repo.getNewLearnedToday(type);
			int newAdded = 0;
			Set<String> labels = new HashSet<>();

			for (Card card : allCards.get(type)) {
				if (card.isDueToday())
					dueCards.get(type).put(card.getId(), card);
				else if (card.isNew() && newAdded < budget) {
					dueCards.get(type).put(card.getId(), card);
					newAdded++;
				}
				labels.addAll(card.getLabels());
			}

			allLabels.put(type, labels);
			initialDueCounts.put(type, initialDueCounts.get(type) + newAdded);
		}
	}
	
	/**
	 * Wärmt die Karten-Shapes vor: bewusst für *alle* Anki-Decks mit Karte, nicht nur die heute
	 * fälligen — ein Fälligkeitsfilter lohnt bei vier Decks nicht.
	 *
	 * <p>Die Bilder einer Bild-Karte bleiben unangetastet; von denen wird nur der Kartenname
	 * notiert, damit der Controller die Skin-Seite vorwärmen lassen kann.</p>
	 */
	private void preloadMap(Deck type) {
		if (type.getMapMetadata() == null)
			return;

		MapService.getInstance().preloadShapes(type);
		if (type.getMapMetadata().getMapType() == MapType.IMAGE)
			imageMapNames.add(type.getMapName());
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
	
	public void savePlayedCards(Deck type, List<PlayedCardData> rows) {
		repo.savePlayedCards(type, rows);
		// Die Learnstats in den Karten sind bereits aktualisiert (in-place vom AnkiSessionProgress).
		// Gespielte Karten liegen weiterhin in dueCards, daher Lookup über die cardId.
		for (PlayedCardData row : rows) {
			Card card = dueCards.get(type).get(row.cardId());
			if (!card.isDueToday())
				dueCards.get(type).remove(row.cardId());
		}
	}
	

	/**
	 * Die Kartennamen der Decks mit Bild-Karte. Reine Namen, keine Pfade — das Auflösen und Vorwärmen
	 * macht die Skin-Seite (siehe {@code SkinImageCache.warmMapImages}).
	 */
	public List<String> getImageMapNames() {
		return imageMapNames;
	}
}
