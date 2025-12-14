package app.presenter;

import java.util.List;
import java.util.Set;

import app.config.Config;
import app.controller.AnkiDeckSession;
import app.data.DeckType;
import app.data.LearnStat;
import app.data.SessionProgress;
import app.ui.MainWindow;
import app.ui.panels.AnkiSessionPanel;
import app.ui.panels.GermanySessionPane;
import app.ui.panels.MCSessionPane;
import app.ui.panels.WorldSessionPanel;

// !Später auch mal ein DeckPresenter und Presenter-Interface wenn es Sinn ergibt? Stay tuned...
public class AnkiSessionPresenter {

    private AnkiSessionPanel panel; //!Später MapDeckGamePanel!
    private AnkiDeckSession session;
    private final DeckType type; // Benötigt für den Neuaufbau eines Panels bei skinChanged
    private final MainWindow mainWindow; // Benötigt für den Neuaufbau eines Panels bei skinChanged

    public AnkiSessionPresenter(MainWindow mainWindow, DeckType type, AnkiDeckSession session) {
    	this.type = type;
    	this.mainWindow = mainWindow;
    	this.session = session;
        this.panel = createPanelForType(type, mainWindow);
    }

    public void refresh() {
        panel = createPanelForType(type, mainWindow);
        panel.show();
    }

    private AnkiSessionPanel createPanelForType(DeckType type, MainWindow mainWindow) {
        return switch(type) {
            case GERMANY_CARDS -> new GermanySessionPane(mainWindow, this);
            case MC_CARDS -> new MCSessionPane(mainWindow, this);
            case WORLD_CARDS -> new WorldSessionPanel(mainWindow, this);
            default -> null; // oder throw new IllegalArgumentException?
        };
    }

    public void start() {
        panel.show();
    }
    
    public void end() {
        session = null;
    }
	
	// ========================================
	// STEP EXECUTION (from Progress)
	// ========================================
	
	public void showImage(String imagePath) {
		panel.setImage(Config.get("imageFolder") + imagePath);
	}

	public void showQuestion(String text) {
		panel.setQuestion(text);
	}
	
	public void showMultipleChoice (List<String> answers) {
		panel.setMapActive(false);
		panel.setTextInTextField("");
		panel.setTextFieldActive(false);
		panel.setMultipleChoice(answers);
	}
 
	public void waitForClick(Set<String> idsInQuestion) {
		panel.beginTx();
		panel.setIdsInQuestion(idsInQuestion);
		panel.setMapActive(true);
		panel.endTx();
		panel.setTextInTextField("");
		panel.setTextFieldActive(false);
		panel.setMCPanelActive(false);
	}
	public void waitForText() {
		panel.setTextFieldActive(true);
		panel.setMapActive(false);
		panel.setMCPanelActive(false);
	}
		
	// ========================================
	// OTHER (from Progress)
	// ========================================
	
	// Input
		
	public void setCorrectText(String correctText) {
		panel.setTextInTextField(correctText);
		panel.setTextFieldActive(false);
	}
	
	public void textIsCorrect() {
		panel.setTextInTextField("");
	}
	
	// Map
	
	public void mapClickChecked(String id, boolean correct, Set<String> corectSet) {
		if (correct) {
			panel.addIdsToCorrect(Set.of(id));
		}
		else {
			panel.beginTx();
			panel.setIdToIncorrect(id);
			panel.addIdsToCorrect(corectSet);
			pause(); // !Architektur: Wir verlassen uns darauf, dass auch der Progress in Pause geht. Ist das clever?
			panel.endTx();
		}
	}
	
	public void setCorrectMapElements(Set<String> correctIds) {
		panel.addIdsToCorrect(correctIds); 
	}
	
	public void markMapElements(Set<String> elements) {
		panel.setMarkedIds(elements);
	}
	
	// MC
	
	public void mcClickChecked(int id, boolean correct) {
		panel.setMcCorrect(id, correct);
	}
	
	public void setCorrectMc(Set<Integer> correctIds) {
		panel.setMcSolution(correctIds); 
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
		panel.sessionProgressChanged(progress);
	}
	
	public void newCardIncoming(LearnStat stats) {
		panel.updateCardStats(stats);
	}
	
	/**
	 * Remove the image, clean the textfield, question and markers.
	 * @param correct can be null in case of back button!
	 */
	public void cardFinished(Boolean correct) {
		panel.resetMarkers();
		panel.setImage(null);
		panel.setTextInTextField("");
		panel.setQuestion("");
	}
	
	public void pause() { // Von außen wegen Pause: im csv...
		panel.setMapActive(false);
		panel.setTextFieldActive(false);
		panel.setMCPanelActive(false);
	}

}
