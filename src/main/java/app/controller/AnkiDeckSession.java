package app.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import app.data.AnkiCard;
import app.data.AnkiCardProgress;
import app.data.AnkiDeckService;
import app.data.AppClock;
import app.data.CardSortOrder;
import app.data.DeckType;
import app.data.LearnStat;
import app.data.SessionProgress;
import app.data.SessionSwitchStrategy;
import app.presenter.AnkiSessionPresenter;
import app.ui.skin.SkinService;
import app.util.Log;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;

public class AnkiDeckSession implements Session{

	private final AnkiSessionPresenter presenter;
	private final AnkiDeckService service;
    private final Controller controller;
    private final boolean isFreePlay;
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
    public AnkiDeckSession(List<AnkiCard> cards, Controller controller, AnkiDeckService service, DeckType type, Consumer<List<AnkiCard>> sorter, boolean isFreePlay) {
    	this.sorter = sorter;
    	this.type = type;
    	this.cards = cards;
    	this.service = service;
    	this.presenter = new AnkiSessionPresenter(type, this);
    	for (AnkiCard card : cards) {
    		card.setProgress(new AnkiCardProgress(card, presenter, this));
    	}
        this.controller = controller;
        this.isFreePlay = isFreePlay;
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
        presenter.sessionProgressChanged(createSessionProgress());
        presenter.newCardIncoming(cards.get(currentIndex).getLearnStat());
        Log.info(this, "Starte AnkiSession " + type.getDisplayName());
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
    
    @Override
    public void close(boolean save) {
    	if (save)
    		save();
    }
    
    /**
     * Updatet die LearnStats. Speichert den Fortschritt. Gibt an den Controller ab
     */
    @Override
    public void end() {
    	Alert alert = SkinService.get().createAlert(getView().getScene().getWindow(), "Zusammenfassung", createSummary(), false, false);
    	alert.showAndWait();
    	
    	if (isFreePlay) {
    		presenter.end();
            controller.sessionEnded();
            return;
    	}
    		
    	save();
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
	
	// ========================================
	// From Controller
	// ========================================	
	
	/**
	 * Hier findet sich die Session-Logik für einen SkinChange.
	 * Für den Moment: Reset der aktuellen Karte
	 * Keep it simple für diesen Edge-Case!
	 * !Sofort: Das sollte überhaupt nicht nötig sein! Wofür haben wir denn CSS-Steuerung?
	 */
	public void refresh() {
		presenter.cardFinished(null);
		presenter.refresh();
		cards.get(currentIndex).setProgress(new AnkiCardProgress(cards.get(currentIndex), presenter, this));
		presenter.sessionProgressChanged(createSessionProgress());
		presenter.newCardIncoming(cards.get(currentIndex).getLearnStat());
		getCurrentProgress().start();
	}
	
	@Override
	public void cancel() {
		getCurrentProgress().cancel();
	}
	
	// ========================================
	// OTHER (I am not happy)
	// ========================================
	
	public boolean isPaused() {
		return getCurrentProgress().isPaused();
	}
	
	private void save() {
    	for (AnkiCard card : cards) {
    		LearnStat learnStat = card.getLearnStat();
    		AnkiCardProgress progress = card.getProgress();
    		
    		// Karte wurde nicht gespielt
    		// !Sofort: Hier ist ein NPE geflogen weil progress null war! Der Fortschritt ist aber abgespeichert worden. Sehr seltsam. Wurde das danach noch einmal aufgerufen?
    		/**
    		 * java.lang.NullPointerException: Cannot invoke "app.data.AnkiCardProgress.isCorrectlyAnswered()" because "progress" is null
	at app.controller.AnkiDeckSession.save(AnkiDeckSession.java:201)
	at app.controller.AnkiDeckSession.close(AnkiDeckSession.java:92)
	at app.controller.Controller.requestSessionSwitch(Controller.java:219)
	at app.controller.Controller.onLearnMenuItemSelected(Controller.java:99)
    		 */
    		if (progress.isCorrectlyAnswered() == null)
    			continue;
    		
    		// Es ist eine neue Karte
    		if (learnStat == null) {
    			card.setLearnStat(new LearnStat(AppClock.TODAY, AppClock.TODAY, progress.isCorrectlyAnswered() ? 1 : 0, progress.isCorrectlyAnswered() ? 0 : 1));
    			continue;
    		}
    		
    		if (!learnStat.isDueToday())
    			throw new RuntimeException("Sicherheitsnetz eingebaut. Diese Karte war gar nicht dran. Und ich soll den Fortschritt überschreiben? Mache ich ungern!");
    		
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
		if (!isFreePlay)
			text+= "\n\nDer Fortschritt wird nun gespeichert.";
		return text;
	}
	
	/**
	 * Damit das MainWindow auch die Session anzeigen kann. Im Falle eines SkinChanges, werden innerhalb dieser StackPane
	 * die Kinder vom Presenter ausgetauscht. 
	 * @return
	 */
	public Pane getView() {
		return presenter.getView();
	}

	@Override
	public SessionSwitchStrategy getSwitchStrategy() {
		if (currentIndex <= 0 || isFreePlay)
			return SessionSwitchStrategy.IMMEDIATE;
		else
			return SessionSwitchStrategy.OFFER_SAVE;
	}

}
