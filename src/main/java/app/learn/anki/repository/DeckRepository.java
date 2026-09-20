package app.learn.anki.repository;

import java.util.List;
import java.util.Map;

import app.learn.anki.model.Card;
import app.learn.model.Deck;
import app.learn.model.LearnStat;

/**
 * Diese Klasse sollte nur vom AnkiDeckService (und vom DashboardScreen) gekannt werden.
 * Wrapper um das CSV und DB Repository
 */
public class DeckRepository {

    private final CsvDeckCardSource csv;
    private final DbDeckProgressRepository db;

    public DeckRepository() {
    	csv = new CsvDeckCardSource();
    	db = new DbDeckProgressRepository();
    }

	public List<Card> getAllCards(Deck type) {

		Map<String, LearnStat> statsById = db.loadAll(type);
		List<Card> cards = csv.loadAll(type);  // liefert fertige Hints

	    for (Card c : cards) {
	        c.setLearnStat(statsById.get(String.valueOf(c.getId())));  // darf null sein = „nie gespielt“
	    }

	    return cards;
	}

	public void savePlayedCards(Deck type, List<PlayedCardData> rows){
		db.saveLearned(type, rows);
	}

	public int getInitialDue(Deck type) {
		return db.getInitialDue(type);
	}

	public int getNewLearnedToday(Deck type) {
		return db.getNewLearnedToday(type);
	}
	
	public int getNoOfLearnedCards() {
		return db.getNoOfLearnedCards();
	}
}