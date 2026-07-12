package app.learn.anki;

import java.util.List;
import java.util.Set;

import app.learn.anki.model.SessionPane;
import app.learn.model.Deck;
import app.learn.model.LearnStat;
import app.learn.model.SessionProgressCounter;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Vermittelt zwischen SessionPane (GUI) und SessionProgress (Ablauf): reicht User-Eingaben der
 * Pane weiter an den Progress und setzt umgekehrt die Anzeige-Aufrufe des Progress in der Pane um.
 *
 * Hält einen final sessionPaneContainer, der im MainWindow gezeigt wird. Bei einem Skin-Wechsel wird
 * die sessionPane darin neu aufgebaut; das MainWindow merkt davon nichts :-)
 */
public class SessionPresenter {

	private final StackPane sessionPaneContainer = new StackPane();
    private SessionPane sessionPane; //!Später MapDeckGamePanel!
    private SessionProgress sessionProgress;
    private final Deck type; // Benötigt für den Neuaufbau eines Panels bei skinChanged

    public SessionPresenter(Deck type, SessionProgress sessionProgress) {
    	this.type = type;
    	this.sessionProgress = sessionProgress;
    	sessionProgress.setPresenter(this);
        sessionPane = createPanelForType(type);
        sessionPaneContainer.getChildren().setAll(sessionPane.asPane());
    }

    public void refresh() {
        sessionPane = createPanelForType(type);
        sessionPaneContainer.getChildren().setAll(sessionPane.asPane());
    }

    private SessionPane createPanelForType(Deck type) {
        return switch(type) {
            case GERMANY_CARDS -> new GermanySessionPane(this);
            case MC_CARDS -> new MCSessionPane(this);
            case WORLD_CARDS -> new ImageMapSessionPane(this, type);
            case HANNOVER_CARDS -> new ImageMapSessionPane(this, type);
            default -> null; // oder throw new IllegalArgumentException?
        };
    }
    
    public void end() {
        sessionProgress = null;
    }
    
	public Pane getView() {
		return sessionPaneContainer;
	}
	
	// ========================================
	// STEP EXECUTION (from Progress)
	// ========================================
	
	public void showImage(String imageName) {
		sessionPane.setImage(imageName);
	}

	public void showQuestion(String text) {
		sessionPane.setQuestion(text);
	}
	
	public void showMultipleChoice (List<String> answers) {
		sessionPane.setMapActive(false);
		sessionPane.setTextInTextField("");
		sessionPane.setTextFieldActive(false);
		sessionPane.setMultipleChoice(answers);
	}
 
	public void waitForClick(Set<String> idsInQuestion) {
		sessionPane.beginTx();
		sessionPane.setIdsInQuestion(idsInQuestion);
		sessionPane.setMapActive(true);
		sessionPane.endTx();
		sessionPane.setTextInTextField("");
		sessionPane.setTextFieldActive(false);
		sessionPane.disableMcPanel();
	}
	public void waitForText() {
		sessionPane.setTextFieldActive(true);
		sessionPane.setMapActive(false);
		sessionPane.disableMcPanel();
	}
		
	// ========================================
	// OTHER (from Progress)
	// ========================================
	
	// Input
		
	public void setCorrectText(String correctText) {
		sessionPane.setTextInTextField(correctText);
		sessionPane.setTextFieldActive(false);
	}
	
	public void textIsCorrect() {
		sessionPane.setTextInTextField("");
	}
	
	// Map
	
	public void mapClickChecked(String id, boolean correct, Set<String> corectSet) {
		if (correct) {
			sessionPane.addIdsToCorrect(Set.of(id));
		}
		else {
			sessionPane.beginTx();
			sessionPane.setIdToIncorrect(id);
			sessionPane.addIdsToCorrect(corectSet);
			pause(); 
			sessionPane.endTx();
		}
	}
	
	public void setCorrectMapElements(Set<String> correctIds) {
		sessionPane.addIdsToCorrect(correctIds); 
	}
	
	public void markMapElements(Set<String> elements) {
		sessionPane.setMarkedIds(elements);
	}
	
	// MC
	
	public void mcClickChecked(int id, boolean correct) {
		sessionPane.setMcCorrect(id, correct);
	}
	
	public void setCorrectMc(Set<Integer> correctIds) {
		sessionPane.setMcSolution(correctIds); 
	}
	
	// ========================================
	// USER INPUT (from Panel)
	// ========================================
	
	public void typedText(String text) {
		sessionProgress.textInputChanged(text);
	}

	public void clickedMapElement(String id) {
		if (sessionProgress.isPaused())
			sessionProgress.reactOnPauseClick();
		else
			sessionProgress.elementClicked(id);
	}
	
	public void clickedPlay() {
		sessionProgress.reactOnPauseClick();
	}
	
	public void clickedBack() {
		sessionProgress.goBack();
	}
	
	public void clickedMCAnswer(int index) {
		if (sessionProgress.isPaused())
			sessionProgress.reactOnPauseClick();
		else
			sessionProgress.mcClicked(index);		
	}
	
	// ========================================
	// SESSION NOTIFICATIONS
	// ========================================
	
	public void sessionProgressChanged(SessionProgressCounter progress) {
		String text = "Korrekt: " + progress.correct()
			+ "\nFalsch: " + progress.incorrect()
			+ "\nOffen: " + (progress.total() - progress.correct() - progress.incorrect());
		sessionPane.setProgressText(text);
	}

	public void newCardIncoming(LearnStat stats) {
		String text = "";
		if (stats != null) {
			text = "Zuletzt gespielt: " + stats.getLastPlayed()
				+ "\nLevel: " + stats.getCurrentLevel()
				+ "\nFalsch beantwortet: " + stats.getWrongCount();
		}
		sessionPane.setCardHistoryText(text);
	}
	
	/**
	 * Remove the image, clean the textfield, question and markers.
	 * @param correct can be null in case of back button!
	 */
	public void cardFinished(Boolean correct) {
		sessionPane.resetMarkers();
		sessionPane.setImage(null);
		sessionPane.setTextInTextField("");
		sessionPane.setQuestion("");
	}
	
	public void pause() { // Von außen wegen Pause: im csv...
		sessionPane.setMapActive(false);
		sessionPane.setTextFieldActive(false);
		sessionPane.disableMcPanel();
	}

}