package app.learn.anki;

import java.util.List;
import java.util.Set;

import app.learn.MapService;
import app.learn.model.Deck;
import app.learn.model.GeoMap;
import app.learn.model.LearnStat;
import app.learn.model.SessionProgressCounter;
import app.shared.model.ScreenView;
import app.shared.model.SessionCallbacks;
import app.shared.ui.AnkiSessionView;
import app.shared.ui.GermanySessionView;
import app.shared.ui.ImageMapSessionView;
import app.shared.ui.McSessionView;

/**
 * Vermittelt zwischen der Anzeige (shared.ui) und dem SessionProgress (Ablauf): reicht Eingaben des
 * Nutzers an den Progress weiter und setzt umgekehrt dessen Anzeige-Aufrufe in der View um.
 *
 * Bei einem Skinwechsel baut sich die View neu auf; das MainWindow merkt davon nichts :-)
 */
public class SessionPresenter {

    private final AnkiSessionView view;
    private SessionProgress sessionProgress;

    public SessionPresenter(Deck type, SessionProgress sessionProgress) {
    	this.sessionProgress = sessionProgress;
    	sessionProgress.setPresenter(this);
        view = createViewFor(type);
    }
    
	public ScreenView getView() {
		return view.getView();
	}

    public void refresh() {
        view.rebuild();
    }

    /**
     * Die drei Lernformen als eigene Ansichten. Was ein Deck ist und wo seine Geometrien
     * herkommen, weiß nur diese Seite — die Ansicht bekommt Namen, Geometrien und Rückmeldungen.
     */
    private AnkiSessionView createViewFor(Deck type) {
        GeoMap map = type.getMapMetadata() != null ? MapService.getInstance().getMap(type) : null;
        String id = type.getId();
        String mapName = type.getMapName();
        String kategorie = type.getCategory().toString();

        SessionCallbacks callbacks = new SessionCallbacks(
                this::clickedMapElement, this::clickedMCAnswer, this::typedText, this::clickedBack);

        return switch(type) {
            case GERMANY_CARDS -> new GermanySessionView(id, mapName, kategorie, map.getShapeGeometries(), callbacks);
            case MC_CARDS      -> new McSessionView(id, mapName, kategorie, callbacks);
            case WORLD_CARDS,
                 HANNOVER_CARDS-> new ImageMapSessionView(id, mapName, kategorie, map::geometryFor, callbacks);
            default -> null; // oder throw new IllegalArgumentException?
        };
    }

    public void end() {
        sessionProgress = null;
    }
	
	// ========================================
	// STEP EXECUTION (from Progress)
	// ========================================
	
	public void showImage(String imageName) {
		view.setImage(imageName);
	}

	public void showQuestion(String text) {
		view.setQuestion(text);
	}
	
	public void showMultipleChoice (List<String> answers) {
		view.setMapActive(false);
		view.setTextInTextField("");
		view.setTextFieldActive(false);
		view.setMultipleChoice(answers);
	}
 
	public void waitForClick(Set<String> idsInQuestion) {
		view.setIdsInQuestion(idsInQuestion);
		view.setMapActive(true);
		view.setTextInTextField("");
		view.setTextFieldActive(false);
		view.disableMcPanel();
	}
	public void waitForText() {
		view.setTextFieldActive(true);
		view.setMapActive(false);
		view.disableMcPanel();
	}
		
	// ========================================
	// OTHER (from Progress)
	// ========================================
	
	// Input
		
	public void setCorrectText(String correctText) {
		view.setTextInTextField(correctText);
		view.setTextFieldActive(false);
	}
	
	public void textIsCorrect() {
		view.setTextInTextField("");
	}
	
	// Map
	
	public void mapClickChecked(String id, boolean correct, Set<String> corectSet) {
		if (correct) {
			view.addIdsToCorrect(Set.of(id));
		}
		else {
				view.setIdToIncorrect(id);
			view.addIdsToCorrect(corectSet);
			pause(); 
			}
	}
	
	public void setCorrectMapElements(Set<String> correctIds) {
		view.addIdsToCorrect(correctIds); 
	}
	
	public void markMapElements(Set<String> elements) {
		view.setMarkedIds(elements);
	}
	
	// MC
	
	public void mcClickChecked(int id, boolean correct) {
		view.setMcCorrect(id, correct);
	}
	
	public void setCorrectMc(Set<Integer> correctIds) {
		view.setMcSolution(correctIds); 
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
		view.setProgressText(text);
	}

	public void newCardIncoming(LearnStat stats) {
		String text = "";
		if (stats != null) {
			text = "Zuletzt gespielt: " + stats.getLastPlayed()
				+ "\nLevel: " + stats.getCurrentLevel()
				+ "\nFalsch beantwortet: " + stats.getWrongCount();
		}
		view.setCardHistoryText(text);
	}
	
	/**
	 * Remove the image, clean the textfield, question and markers.
	 * @param correct can be null in case of back button!
	 */
	public void cardFinished(Boolean correct) {
		view.resetMarkers();
		view.setImage(null);
		view.setTextInTextField("");
		view.setQuestion("");
	}
	
	public void pause() { // Von außen wegen Pause: im csv...
		view.setMapActive(false);
		view.setTextFieldActive(false);
		view.disableMcPanel();
	}

}