package app.learn.anki;

import java.util.List;
import java.util.Map;
import java.util.Set;

import app.learn.MapService;
import app.learn.anki.model.Card;
import app.learn.model.Deck;
import app.learn.model.GeoMap;
import app.learn.model.LearnStat;
import app.learn.model.SessionProgressCounter;
import app.learn.repository.SketchFileSource;
import app.shared.model.ScreenView;
import app.shared.model.SketchColor;
import app.shared.model.AnkiCallbacks;
import app.shared.ui.AnkiLearnView;
import app.shared.ui.FastWriteLearnView;
import app.shared.ui.ShapeMapLearnView;
import app.shared.ui.ImageMapLearnView;
import app.shared.ui.McLearnView;

/**
 * Vermittelt zwischen der Anzeige (shared.ui) und dem SessionProgress (Ablauf): reicht Eingaben des
 * Nutzers an den Progress weiter und setzt umgekehrt dessen Anzeige-Aufrufe in der View um.
 *
 * Bei einem Skinwechsel baut sich die View neu auf; das MainWindow merkt davon nichts :-)
 */
public class SessionPresenter {

    private final AnkiLearnView view;
    private FastWriteLearnView fastView; // Gleiches Objekt wie die view, aber mit mehr Methoden sichtbar. Nur bei Fast Write gesetzt
    private final SessionProgress sessionProgress;
    // Strukturdateien sind winzig und werden je Karte einmal gebraucht — gelesen wie ein Bild,
    // ohne Vorrat.
    private final SketchFileSource sketchSource = new SketchFileSource();

    SessionPresenter(Deck type, SessionProgress sessionProgress) {
    	this.sessionProgress = sessionProgress;
    	sessionProgress.setPresenter(this);
        view = createViewFor(type);
    }
    
	ScreenView getView() {
		return view.getView();
	}

    void refresh() {
        view.rebuild();
    }

    /**
     * Die drei Lernformen als eigene Ansichten. Was ein Deck ist und wo seine Geometrien
     * herkommen, weiß nur diese Seite — die Ansicht bekommt Namen, Geometrien und Rückmeldungen.
     */
    private AnkiLearnView createViewFor(Deck type) {
        GeoMap map = type.getMapMetadata() != null ? MapService.getInstance().getMap(type) : null;
        String id = type.getId();
        String mapName = type.getMapName();
        String category = type.getCategory().toString();

        AnkiCallbacks callbacks = new AnkiCallbacks(
                this::clickedMapElement, this::clickedMCAnswer, this::typedText, this::clickedBack,
                this::timeExpired);

        return switch(type) {
            case GERMANY_CARDS -> new ShapeMapLearnView(id, mapName, category, map.getShapeGeometries(), callbacks);
            case MC_CARDS      -> new McLearnView(id, mapName, category, callbacks);
            case WORLD_CARDS,
                 FLAG_CARDS,
                 HANNOVER_CARDS-> new ImageMapLearnView(id, mapName, category, map::geometryFor, callbacks);
            case FAST_WRITE_CARDS -> {
                fastView = new FastWriteLearnView(id, mapName, category, Card.MAX_FAST_SLOTS, callbacks);
                yield fastView;
            }
            default -> null; // oder throw new IllegalArgumentException?
        };
    }

	// ========================================
	// STEP EXECUTION (from Progress)
	// ========================================
	
	void showImage(String imageName) {
		view.setImage(imageName);
	}

	/** Lädt die Struktur und zeigt sie leer — eine zuvor gezeigte Skizze ist damit weg. */
	void showSketch(String structure) {
		view.setSketch(sketchSource.load(structure));
	}

	/** Hängt eine weitere Struktur an, ohne die bisherigen Füllungen zu verlieren. */
	void addSketch(String structure, int cell) {
		view.addSketch(sketchSource.load(structure), cell);
	}

	/** Setzt eine Fläche in ein anderes Rasterfeld — Farbe und Markierung bleiben. */
	void moveSketchArea(int area, int cell) {
		view.moveSketchArea(area, cell);
	}

	void markSketchArea(int area) {
		view.markSketchArea(area);
	}

	void fillSketchArea(int area, SketchColor color) {
		view.fillSketchArea(area, color);
	}

	void showQuestion(String text) {
		view.setQuestion(text);
	}
	
	void showMultipleChoice (List<String> answers) {
		view.setMapActive(false);
		view.setTextInTextField("");
		view.setTextFieldActive(false);
		view.setMultipleChoice(answers);
	}
 
	void waitForClick(Set<String> idsInQuestion) {
		view.setClickTargets(idsInQuestion);
		view.setMapActive(true);
		view.setTextInTextField("");
		view.setTextFieldActive(false);
		view.disableMcPanel();
	}
	void waitForText() {
		view.setTextFieldActive(true);
		view.setMapActive(false);
		view.disableMcPanel();
	}

	// ========================================
	// FAST WRITE (from Progress)
	// ========================================

	void showFastStep(List<String> slotHints, Integer expectedSlot, int firstSeconds, int nextSeconds) {
		waitForText();
		fastView.showFastStep(slotHints, expectedSlot, firstSeconds, nextSeconds);
	}

	void fastAnswerCorrect(int slot, String text, Integer expectedSlot) {
		fastView.revealSlot(slot, text, expectedSlot);
		fastView.playCorrectSound();
		view.setTextInTextField("");
	}

	void fastTimeUp(Map<Integer, String> missing) {
		fastView.revealMissing(missing);
		view.setTextFieldActive(false);
	}

	void restartClock() {
		fastView.restartClock();
	}

	void toggleClock() {
		fastView.toggleClock();
	}

	/** Läuft beim Verlassen jeder Session — nur Fast Write hat überhaupt eine Uhr. */
	void stopClock() {
		if (fastView != null)
			fastView.stopClock();
	}

	void suspendClock() {
		if (fastView != null)
			fastView.suspendClock();
	}

	void resumeClock() {
		if (fastView != null)
			fastView.resumeClock();
	}
		
	// ========================================
	// OTHER (from Progress)
	// ========================================
	
	// Input
		
	void setCorrectText(String correctText) {
		view.setTextInTextField(correctText);
		view.setTextFieldActive(false);
	}
	
	void textIsCorrect() {
		view.setTextInTextField("");
	}
	
	// Map
	
	void mapClickChecked(String id, boolean correct, Set<String> corectSet) {
		if (correct) {
			view.addIdsToCorrect(Set.of(id));
		}
		else {
				view.setIdToIncorrect(id);
			view.addIdsToCorrect(corectSet);
			pause(); 
			}
	}
	
	void setCorrectMapElements(Set<String> correctIds) {
		view.addIdsToCorrect(correctIds); 
	}
	
	void markMapElements(Set<String> elements) {
		view.setMarkedIds(elements);
	}
	
	// MC
	
	void mcClickChecked(int id, boolean correct) {
		view.setMcCorrect(id, correct);
	}
	
	void setCorrectMc(Set<Integer> correctIds) {
		view.setMcSolution(correctIds); 
	}
	
	// ========================================
	// USER INPUT (from Panel)
	// ========================================
	
	void typedText(String text) {
		sessionProgress.textInputChanged(text);
	}

	void clickedMapElement(String id) {
		if (sessionProgress.isPaused())
			sessionProgress.reactOnPausePressed();
		else
			sessionProgress.elementClicked(id);
	}
	
	void clickedBack() {
		sessionProgress.goBack();
	}
	
	void clickedMCAnswer(int index) {
		if (sessionProgress.isPaused())
			sessionProgress.reactOnPausePressed();
		else
			sessionProgress.mcClicked(index);
	}

	void timeExpired() {
		sessionProgress.timeUp();
	}
	
	// ========================================
	// SESSION NOTIFICATIONS
	// ========================================
	
	void sessionProgressChanged(SessionProgressCounter progress) {
		String text = "Korrekt: " + progress.correct()
			+ "\nFalsch: " + progress.incorrect()
			+ "\nOffen: " + (progress.total() - progress.correct() - progress.incorrect());
		view.setProgress(text);
	}

	void newCardIncoming(LearnStat stats) {
		String text = "";
		if (stats != null) {
			text = "Zuletzt gespielt: " + stats.getLastPlayed()
				+ "\nLevel: " + stats.getCurrentLevel()
				+ "\nFalsch beantwortet: " + stats.getWrongCount();
		}
		view.setCardHistory(text);
	}
	
	/**
	 * Remove the image, clean the textfield, question and markers.
	 * @param correct can be null in case of back button!
	 */
	void cardFinished(Boolean correct) {
		view.resetMarkers();
		view.setImage(null);
		view.setTextInTextField("");
		view.setQuestion("");
		if (fastView != null) {
			fastView.stopClock(); // auch beim Zurückspringen, sonst tickt sie in die nächste Karte
			fastView.clearSlots();
		}
	}
	
	void pause() { // Von außen wegen Pause: im csv...
		view.setMapActive(false);
		view.setTextFieldActive(false);
		view.disableMcPanel();
	}

}