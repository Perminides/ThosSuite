package app.learn.anki.repository;

import java.util.List;
import java.util.Map;

import app.learn.anki.model.Card;
import app.learn.model.Deck;
import app.learn.model.LearnStat;

/**
 * Diese Klasse sollte nur vom AnkiDeckService gekannt werden.
 * Wrapper um das CSV und DB Repository
 */
public class DeckRepository {

    private final CsvDeckCardSource csv;
    private final DbDeckProgressSource db;
    
    public DeckRepository() {
    	csv = new CsvDeckCardSource(); //!Später: Hier muss der GameType mitgegeben werden
    	db = new DbDeckProgressSource();
    }

	public List<Card> getAllHints(Deck type) {
		
		Map<String, LearnStat> statsById = db.loadAll(type);
		List<Card> hints = csv.loadAll(type);  // liefert fertige Hints

	    for (Card h : hints) {
	        h.setLearnStat(statsById.get(String.valueOf(h.getId())));  // darf null sein = „nie gespielt“
	    }

	    return hints;
	}
	
	public void savePlayedCards(Deck type, List<Card> cards){
		db.saveLearned(type, cards);
	}
	
	public int getInitialDue(Deck type) {
		return db.getInitialDue(type);
	}
}
