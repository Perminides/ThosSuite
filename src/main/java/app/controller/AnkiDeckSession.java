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
import app.data.Deck;
import app.data.LearnStat;
import app.data.SessionProgress;
import app.data.SessionSwitchStrategy;
import app.presenter.AnkiSessionPresenter;
import app.ui.skin.SkinService;
import app.util.Log;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;

public class AnkiDeckSession implements Screen{

	private final AnkiSessionPresenter presenter;
	private final AnkiDeckService service;
    private final Controller controller;
    private final boolean isFreePlay;
    private int currentIndex = -1;
    private final Deck type;
    private List<AnkiCard> cards;
    private Consumer<List<AnkiCard>> sorter;
    private Boolean active = null;

    /**
     * 
     * @param cards
     * @param controller
     * @param presenter
     * @param service
     * @param type
     */
    public AnkiDeckSession(List<AnkiCard> cards, Controller controller, AnkiDeckService service, Deck type, Consumer<List<AnkiCard>> sorter, boolean isFreePlay) {
    	Log.info(this, "=== SESSION CONSTRUCTOR === Session@" + System.identityHashCode(this));
    	this.active = true;
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
    	Log.info(this, "=== SESSION START === Session@" + System.identityHashCode(this));
    	if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
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
    	if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
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
    
    // ==== How to end a session ====
    
    @Override
    /**
     * Beende die Session ohne weitere Dialoge bitte. Je nach Parameter mit oder ohne Save...
     */
    public void closeSilent(boolean save) {
    	if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
    	active = false;
    	Log.info(this, "=== CLOSE === Session@" + System.identityHashCode(this) + ", save=" + save);
    	if (save)
    		save();
    }
    
    /**
     * Beende die Session, aber gern sauber schön mit Zusammenfassung und so :)
     */
    @Override
    public void endGracefully() {
    	if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
    	Pane currentPane = getView(); // Muss vor dem inaktiv setzen passieren...
    	active = false;
    	Alert alert = SkinService.get().createAlert(currentPane.getScene().getWindow(), "Zusammenfassung", createSummary(), false, false);
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
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		currentIndex++;
		if (currentIndex < cards.size()) {
			presenter.cardFinished(correct);
			presenter.sessionProgressChanged(createSessionProgress());
			presenter.newCardIncoming(cards.get(currentIndex).getLearnStat());
			getCurrentProgress().start();
		} else {
			endGracefully();
		}
			
	}
	
	// ========================================
	// User interaction from presenter
	// ========================================

	public void textInputChanged(String text) {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		getCurrentProgress().checkTextInput(text);
	}
	
	public void elementClicked(String id) {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		getCurrentProgress().elementClicked(id);
	}
	
	/**public void clickedWrongly() {
		getCurrentProgress().clickedWrongly();
	}**/
	

	public void mcClicked(int index) {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		getCurrentProgress().mcClicked(index);
	}
	
	@Override
	public void reactOnPauseClick() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		getCurrentProgress().endPause();
	}
	
	public void goBack() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
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
	 * Für den Moment: Reset der aktuellen Karte Keep it simple für diesen Edge-Case!
	 * Logik nötig, da SkinChange nicht nur für Bilder beschneiden problematisch ist,
	 * sondern ja auch Komponenten anders platzieren könnte und deren Größe ändern.
	 * Also um einen Neuaufbau kommt man bei meinen mächtigen Skins nicht herum...
	 */
	public void refresh() {
		Log.info(this, "=== REFRESH === Session@" + System.identityHashCode(this) + ", currentIndex=" + currentIndex);
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		presenter.cardFinished(null);
		presenter.refresh();
		cards.get(currentIndex).setProgress(new AnkiCardProgress(cards.get(currentIndex), presenter, this));
		presenter.sessionProgressChanged(createSessionProgress());
		presenter.newCardIncoming(cards.get(currentIndex).getLearnStat());
		getCurrentProgress().start();
	}
	
	@Override
	public void escClicked() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		getCurrentProgress().cancel();
	}
	
	// ========================================
	// OTHER (I am not happy)
	// ========================================
	
	public boolean isPaused() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		return getCurrentProgress().isPaused();
	}
	
	/**
     * Updatet die LearnStats. Speichert den Fortschritt.
	 */
	private void save() {
		Log.info(this, "=== SAVE START === Session@" + System.identityHashCode(this) 
        + ", cards.size=" + cards.size() 
        + ", currentIndex=" + currentIndex
        + ", isFreePlay=" + isFreePlay);
		
    	for (AnkiCard card : cards) {
    		LearnStat learnStat = card.getLearnStat();
    		AnkiCardProgress progress = card.getProgress();
    		
    		// Karte wurde nicht gespielt
    		// !Sofort: Hier ist ein NPE geflogen weil progress null war! Der Fortschritt ist aber abgespeichert worden. Sehr seltsam. Wurde das danach noch einmal aufgerufen?
    		/**
    		 * 2026-01-16 22:02:02 [INFO] app.controller.AnkiDeckSession - Starte AnkiSession Deutschland
2026-01-16 22:02:02 [INFO] app.data.AnkiCardProgress - Los geht es mit Karte: 1160008
2026-01-16 22:02:07 [INFO] app.data.AnkiCardProgress - Los geht es mit Karte: 1500160
2026-01-16 22:02:11 [INFO] app.controller.AnkiDeckSession - Starte AnkiSession Multiple Choice
2026-01-16 22:02:11 [INFO] app.data.AnkiCardProgress - Los geht es mit Karte: 2146
2026-01-16 22:02:14 [INFO] app.data.AnkiCardProgress - Los geht es mit Karte: 4886 → Mit dem Zeitpunkt wurde 2146 gespeichert
2026-01-16 22:02:19 [INFO] app.data.AnkiCardProgress - Los geht es mit Karte: 4886
2026-01-16 22:02:27 [SEVERE] app.ThosSuiteApp - Uncaught exception in thread JavaFX Application Thread
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
        Log.info(this, "=== SAVE END === All progress set to null");
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
	 * die Kinder vom Presenter ausgetauscht. Wird auch intern genutzt beim Anzeigen von PopUps...
	 * @return
	 */
	public Pane getView() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		return presenter.getView();
	}

	@Override
	public SessionSwitchStrategy getSwitchStrategy() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		if (currentIndex <= 0 || isFreePlay)
			return SessionSwitchStrategy.IMMEDIATE;
		else
			return SessionSwitchStrategy.OFFER_SAVE;
	}

}
