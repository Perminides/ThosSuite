package app.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import app.data.AnkiCard;
import app.data.AnkiCardProgress;
import app.data.AnkiDeckService;
import app.data.AppClock;
import app.data.CardSortOrder;
import app.data.DeckType;
import app.data.LearnStat;
import app.data.SessionProgress;
import app.presenter.AnkiSessionPresenter;
import app.ui.MainWindow;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
import javafx.scene.control.Alert;

public class AnkiDeckSession implements Session{

	private final MainWindow mainWindow; // !Architektur. Also so richtig nice finde ich nicht, dass das hier gehalten werden muss. Aber wenn Du hier einen Alert showst...
	private final AnkiSessionPresenter presenter;
	private final AnkiDeckService service;
    private final Controller controller;
    private int currentIndex = -1;
    private final DeckType type;
    private List<AnkiCard> cards;
    private Consumer<List<AnkiCard>> sorter;

    /**
     * 
     * @param cards
     * @param controller
     * @param presenter
     * @param service
     * @param type
     */
    public AnkiDeckSession(MainWindow mainWindow, List<AnkiCard> cards, Controller controller, AnkiDeckService service, DeckType type, Consumer<List<AnkiCard>> sorter) {
    	this.mainWindow = mainWindow;
    	this.sorter = sorter;
    	this.type = type;
    	this.cards = cards;
    	this.service = service;
    	this.presenter = new AnkiSessionPresenter(mainWindow, type, this);
    	for (AnkiCard card : cards) {
    		card.setProgress(new AnkiCardProgress(card, presenter, this));
    	}
        this.controller = controller;
    }

    public void start() {
    	// Neue Karten immer zuerst
    	List<AnkiCard> newCards = new ArrayList<>();
    	Iterator<AnkiCard> iter = cards.listIterator();
    	while (iter.hasNext()) {
    		AnkiCard card = iter.next();
    		if (card.isNew()) {
    			iter.remove();
    			newCards.add(card);
    		}
    	}
    	sorter.accept(cards);
    	cards = Stream.concat(newCards.stream(), cards.stream()).collect(Collectors.toCollection(ArrayList::new));
        currentIndex = 0;
        presenter.start();
        presenter.sessionProgressChanged(createSessionProgress());
        presenter.newCardIncoming(cards.get(currentIndex).getLearnStat());
        getCurrentProgress().start();
    }
    
    public void sort(CardSortOrder order) {
    	int index = currentIndex+1;
    	while (index < cards.size() && cards.get(index).isNew()) {
   			index++;
    	}
    	List<AnkiCard> fixed = new ArrayList<AnkiCard>(cards.subList(0, index));
    	List<AnkiCard> toSort = new ArrayList<AnkiCard>(cards.subList(index, cards.size()));
    	order.sort(toSort);
    	fixed.addAll(toSort);
    	cards = fixed;
    }
    
    /**
     * Updatet die LearnStats 
     * Speichert den Fortschritt
     * Gibt an die Session ab
     */
    public void end() {
    	Alert alert = SkinService.get().createAlert(mainWindow.getStage(), "Zusammenfassung", createSummary());
    	alert.showAndWait();
    	
    	for (AnkiCard card : cards) {
    		LearnStat learnStat = card.getLearnStat();
    		AnkiCardProgress progress = card.getProgress();
    		
    		// Karte wurde nicht gespielt
    		if (progress.isCorrectlyAnswered() == null)
    			continue;
    		
    		// Es ist eine neue Karte
    		if (learnStat == null) {
    			card.setLearnStat(new LearnStat(AppClock.TODAY, AppClock.TODAY, progress.isCorrectlyAnswered() ? 1 : 0, progress.isCorrectlyAnswered() ? 0 : 1));
    			continue;
    		}
    		
    		learnStat.setLevel(progress.calculateNewLevel(learnStat.getLastPlayed(), progress.isCorrectlyAnswered(), true));
    		learnStat.setLastPlayed(AppClock.TODAY);
    		if (!progress.isCorrectlyAnswered())
    				learnStat.incrementWrongCount();
    	}
		service.savePlayedCards(type, cards);
        for (AnkiCard card : cards) {
        	card.setProgress(null);
        }
        presenter.end();
        controller.sessionEnded();
    }

	public void cardFinished(boolean correct) {
		currentIndex++;
		if (currentIndex < cards.size()) {
			presenter.cardFinished(correct);
			presenter.sessionProgressChanged(createSessionProgress());
			presenter.newCardIncoming(cards.get(currentIndex).getLearnStat());
			getCurrentProgress().start();
		} else {
			end();
		}
			
	}
	
	// ========================================
	// User interaction from presenter
	// ========================================

	public void textInputChanged(String text) {
		getCurrentProgress().checkTextInput(text);
	}
	
	public void elementClicked(String id) {
		getCurrentProgress().elementClicked(id);
	}
	
	/**public void clickedWrongly() {
		getCurrentProgress().clickedWrongly();
	}**/
	

	public void mcClicked(int index) {
		getCurrentProgress().mcClicked(index);
	}
	
	public void endPause() {
		getCurrentProgress().endPause();
	}
	
	/**
	 * Hier findet sich die Session-Logik für einen SkinChange.
	 * Für den Moment: Reset der aktuellen Karte
	 * Keep it simple für diesen Edge-Case!
	 */
	public void refresh() {
		presenter.cardFinished(null);
		presenter.refresh();
		cards.get(currentIndex).setProgress(new AnkiCardProgress(cards.get(currentIndex), presenter, this));
		presenter.sessionProgressChanged(createSessionProgress());
		presenter.newCardIncoming(cards.get(currentIndex).getLearnStat());
		getCurrentProgress().start();
	}
	
	public void goBack() {
		if (currentIndex > 0) {
			presenter.cardFinished(null);
			cards.get(currentIndex).setProgress(new AnkiCardProgress(cards.get(currentIndex), presenter, this));
			currentIndex--;
			cards.get(currentIndex).setProgress(new AnkiCardProgress(cards.get(currentIndex), presenter, this));
			presenter.sessionProgressChanged(createSessionProgress());
			presenter.newCardIncoming(cards.get(currentIndex).getLearnStat());
			getCurrentProgress().start();
		}
	}
	
	public void cancel() {
		getCurrentProgress().cancel();
	}
	
	// ========================================
	// OTHER (I am not happy)
	// ========================================
	
	public boolean isPaused() {
		return getCurrentProgress().isPaused();
	}
	
	private AnkiCardProgress getCurrentProgress() {
	    return cards.get(currentIndex).getProgress();
	}
	
	// On-the-fly berechnen
	private SessionProgress createSessionProgress() {
	    List<Boolean> history = new ArrayList<>();
	    int correct = 0;
	    int incorrect = 0;
	    
	    for (AnkiCard card : cards) {
	        Boolean answered = card.getProgress().isCorrectlyAnswered();
	        history.add(answered);
	        if (answered != null) {
	            if (answered) correct++;
	            else incorrect++;
	        }
	    }
	    return new SessionProgress(correct, incorrect, history);
	}
	
	private String createSummary() {
		SessionProgress progress = createSessionProgress();
		String text = "Du hast " + (progress.correct() + progress.incorrect()) + " von " + progress.details().size() + " Karten gelernt.";
		text += "\n\nDavon hast Du " + progress.correct() + " richtig und " + progress.incorrect() + " falsch beantwortet.";
		text+= "\n\nDer Fortschritt wird nun gespeichert.";
		return text;
	}

}
