package app.presenter;

import java.util.List;
import java.util.Set;

import app.config.Config;
import app.controller.AnkiDeckSession;
import app.data.Deck;
import app.data.LearnStat;
import app.data.SessionProgress;
import app.ui.panes.AnkiSessionPane;
import app.ui.panes.GermanySessionPane;
import app.ui.panes.MCSessionPane;
import app.ui.panes.ImageMapSessionPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Calls AnkiSessionPane and the Session (which forwards most calls to the current Progress of course).
 * Is called by the current progress directly.
 * 
 * Also holds a final sessionPaneContainer, which is shown in the MainWindow. In case of a skin change,
 * the sessionPane inside this container is recreated. The MainWindow won't realize this :-)
 */
public class AnkiSessionPresenter {

	private final StackPane sessionPaneContainer = new StackPane();
    private AnkiSessionPane sessionPane; //!Später MapDeckGamePanel!
    private AnkiDeckSession session;
    private final Deck type; // Benötigt für den Neuaufbau eines Panels bei skinChanged

    public AnkiSessionPresenter(Deck type, AnkiDeckSession session) {
    	this.type = type;
    	this.session = session;
        sessionPane = createPanelForType(type);
        sessionPaneContainer.getChildren().setAll(sessionPane.asPane());
    }

    public void refresh() {
        sessionPane = createPanelForType(type);
        sessionPaneContainer.getChildren().setAll(sessionPane.asPane());
    }

    private AnkiSessionPane createPanelForType(Deck type) {
        return switch(type) {
            case GERMANY_CARDS -> new GermanySessionPane(this);
            case MC_CARDS -> new MCSessionPane(this);
            case WORLD_CARDS -> new ImageMapSessionPane(this, type);
            case HANNOVER_CARDS -> new ImageMapSessionPane(this, type);
            default -> null; // oder throw new IllegalArgumentException?
        };
    }
    
    public void end() {
        session = null;
    }
    
	public Pane getView() {
		return sessionPaneContainer;
	}
	
	// ========================================
	// STEP EXECUTION (from Progress)
	// ========================================
	
	public void showImage(String imagePath) {
		sessionPane.setImage(Config.get("learnImageFolder") + imagePath);
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
		session.textInputChanged(text);
	}

	public void clickedMapElement(String id) {
		if (session.isPaused())
			session.endPause();
		else
			session.elementClicked(id);
	}
	
	/**public void mapClicked() {
		if (session.isPaused())
			session.endPause();
		else
			session.clickedWrongly();
	}**/
	
	public void clickedPlay() {
		session.endPause();
	}
	
	public void clickedBack() {
		session.goBack();
	}
	
	public void clickedMCAnswer(int index) {
		if (session.isPaused())
			session.endPause();
		else
			session.mcClicked(index);		
	}
	
	// ========================================
	// SESSION NOTIFICATIONS
	// ========================================
	
	public void sessionProgressChanged(SessionProgress progress) {
		sessionPane.sessionProgressChanged(progress);
	}
	
	public void newCardIncoming(LearnStat stats) {
		sessionPane.updateCardStats(stats);
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
