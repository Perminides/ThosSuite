package app.learn.anki;

import java.util.List;
import java.util.Map;
import java.util.Set;

import app.learn.MapService;
import app.learn.anki.model.Card;
import app.learn.model.Deck;
import app.learn.model.GeoMap;
import app.learn.model.LearnStat;
import app.shared.model.CardHistory;
import app.shared.model.CollectChip;
import app.shared.model.SessionProgressCounter;
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
        String category = type.getCategory().getSkinKey();

        AnkiCallbacks callbacks = new AnkiCallbacks(
                this::clickedMapElement, this::clickedMCAnswer, this::typedText, this::clickedBack,
                this::clickedSubmit, this::timeExpired, this::collectAdded, this::collectRemoved);

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
            default -> throw new RuntimeException("Kein View-Typ für Deck " + type);
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
		view.setSketch(sketchSource.loadBackground(structure));
	}

	/** Hängt eine weitere Struktur an, ohne die bisherigen Füllungen zu verlieren. */
	void addSketch(String structure, int cell, double size, double offsetX, double offsetY) {
		view.addSketch(sketchSource.load("elements", structure), cell, size, offsetX, offsetY);
	}

	void markSketchAreas(List<Integer> areas) {
		view.markSketchAreas(areas);
	}

	void fillSketchAreas(List<Integer> areas, SketchColor color) {
		view.fillSketchAreas(areas, color);
	}

	void showQuestion(String text) {
		view.setQuestion(text);
	}
	
	void showMultipleChoice (List<String> answers) {
		view.askMultipleChoice(answers);
	}

	void waitForClick(Set<String> idsInQuestion) {
		view.askClick(idsInQuestion);
	}

	void waitForText() {
		view.askText();
	}

	// ========================================
	// COLLECT (from Progress)
	// ========================================

	void waitForCollect(List<String> suggestions) {
		view.askCollect(suggestions);
	}

	void showCollected(List<CollectChip> chips) {
		view.setCollectChips(chips);
	}

	void revealCollect(List<CollectChip> chips) {
		view.revealCollect(chips);
	}

	// ========================================
	// FAST WRITE (from Progress)
	// ========================================

	void showFastStep(List<String> slotHints, Integer expectedSlot, int firstSeconds, int nextSeconds) {
		fastView.showFastStep(slotHints, expectedSlot, firstSeconds, nextSeconds);
	}

	void fastAnswerCorrect(int slot, String text, Integer expectedSlot) {
		fastView.revealSlot(slot, text, expectedSlot);
	}

	void fastTimeUp(Map<Integer, String> missing) {
		fastView.revealMissing(missing);
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
		view.revealText(correctText);
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

	void mcMarked(int id, boolean marked) {
		view.setMcMarked(id, marked);
	}

	/** Angekommen, aber nicht angenommen — ohne Wertung und ohne bleibende Spur. */
	void mcClickRejected(int id) {
		view.rejectMcClick(id);
	}
	
	void setCorrectMc(Set<Integer> correctIds) {
		view.setMcSolution(correctIds);
	}

	/** Ob Absenden jetzt eine Wirkung hätte — bei MC+ und Collect, sobald etwas ausgewählt ist. */
	void canSubmit(boolean possible) {
		view.setSubmitActive(possible);
	}
	
	// ========================================
	// USER INPUT (from Panel)
	// ========================================
	
	void typedText(String text) {
		sessionProgress.textInputChanged(text);
	}

	void collectAdded(String word) {
		sessionProgress.collectAdded(word);
	}

	void collectRemoved(String word) {
		sessionProgress.collectRemoved(word);
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

	/**
	 * Absende-Knopf, Enter am Fenster und Enter im leeren Eingabefeld — ein Weg für alle drei. In der
	 * Pause schaltet er weiter, genau wie ein Klick auf einen der grauen MC-Knöpfe.
	 */
	void clickedSubmit() {
		if (sessionProgress.isPaused())
			sessionProgress.reactOnPausePressed();
		else {
			view.setSubmitActive(false);
			sessionProgress.submitClicked();
		}
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
		view.setProgress(progress);
	}

	void newCardIncoming(LearnStat stats) {
		// Kartenstart: Back nur ab Karte 2.
		view.setBackActive(sessionProgress.canGoBack());
		if (stats == null)
			view.clearCardHistory();
		else
			view.setCardHistory(new CardHistory(stats.getLastPlayed(), stats.getCurrentLevel(), stats.getWrongCount()));
	}
	
	void cardFinished() {
		view.clearCard();
	}

	void pause() {
		view.pause();
	}

}