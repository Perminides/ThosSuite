package app.data.persistence.anki;

import java.util.List;
import java.util.Map;

import app.data.AnkiCard;
import app.data.DeckType;
import app.data.LearnStat;

/**
 * Diese Klasse sollte nur vom MapDeckService gekannt werden.
 */
public class AnkiDeckRepository {

    private final CsvAnkiDeckCardSource csv;
    private final DbAnkiDeckProgressSource db;
    
    public AnkiDeckRepository() {
    	csv = new CsvAnkiDeckCardSource(); //!Später: Hier muss der GameType mitgegeben werden
    	db = new DbAnkiDeckProgressSource();
    }

	public List<AnkiCard> getAllHints(DeckType type) {
		
		Map<String, LearnStat> statsById = db.loadAll(type);
		List<AnkiCard> hints = csv.loadAll(type);  // liefert fertige Hints

	    for (AnkiCard h : hints) {
	        h.setLearnStat(statsById.get(String.valueOf(h.getId())));  // darf null sein = „nie gespielt“
	    }

	    return hints;
	}
	
	public void savePlayedCards(DeckType type, List<AnkiCard> cards){
		db.saveLearned(type, cards);
	}
	
	public int getInitialDue(DeckType type) {
		return db.getInitialDue(type);
	}
}
