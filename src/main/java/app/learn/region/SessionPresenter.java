package app.learn.region;

import java.util.Set;

import app.learn.region.model.Mode;
import app.learn.region.model.SessionSpec;
import app.shared.model.RegionCallbacks;
import app.shared.model.ScreenView;
import app.learn.MapService;
import app.shared.model.ShapeMapState;
import app.shared.ui.RegionLearnView;

/**
 * Intermediary between RegionLearnView and the RegionSessionProgress.
 * Bei einem Skinwechsel baut sich die View neu auf; das MainWindow merkt davon nichts :-)
 */
public class SessionPresenter {
	// Wir speichern auch die Frage für Skinwechsel
	private record SavedState (ShapeMapState mapState, String text) {};
	private record WrongAnswerSnapshot(ShapeMapState beforeMap, String beforeText, String expectedId) {}
	
	private final RegionLearnView view;
	private final SessionSpec spec; // Benötigt für den Neuaufbau eines Panels bei skinChanged
	private final SessionProgress progress;

	private WrongAnswerSnapshot wrongAnswerSnapshot;
	private SavedState savedState;
	
	SessionPresenter(SessionProgress progress, SessionSpec spec, Set<String> sessionShapes) {
		progress.setPresenter(this);
		view = new RegionLearnView(
				spec.getDeckType().getId(),
				spec.getDeckType().getMapName(),
				spec.getDeckType().getCategory().getSkinKey(),
				MapService.getInstance().getMap(spec.getDeckType()).getShapeGeometries(),
				sessionShapes,
				spec.getMode().getSubCategory() == Mode.SubCategory.CLICK,
				new RegionCallbacks(this::clickedMapElement, this::typedText));
		this.progress = progress;
		this.spec = spec;
	}
	
	ScreenView getView() {
		return view.getView();
	}
	
	// ========================================
	// STEP EXECUTION (from Progress)
	// ========================================
	
	void refresh() {
		savedState = new SavedState(view.getState(), view.getQuestion());
		view.rebuild(spec.getMode().getSubCategory() == Mode.SubCategory.CLICK);
		view.setState(savedState.mapState);
		view.setQuestion(savedState.text);
		savedState = null;
	}

	void weWaitForClick(Set<String> ids) {
		view.addIdsToActive(ids);
		view.setMapActive(true);
	}
	
	void weWaitForEliminationText(Set<String> ids) {
		view.addIdsToInactive(ids);
		view.setMapActive(false);
	}
	
	void weWaitForWriteText(String id) {
		view.addIdsToMarked(Set.of(id));
		view.setMapActive(true);
	}
	
	void prepareWriteSession(Set<String> ids) {
		view.addIdsToInactive(ids);
		view.setMapActive(false);
	}
	
	void setCorrectText(String correctText) {
		view.setTextInTextField(correctText);
		view.setTextFieldActive(false);
	}
	
	void showQuestion(String text) {
		view.setQuestion(text);
	}
	
	void handleAnswerResult(String id, boolean correct, String correctId) {
		if (correct) {
			view.addIdsToCorrect(Set.of(id));

		} else {
			wrongAnswerSnapshot = new WrongAnswerSnapshot(
				    view.getState(),
				    view.getQuestion(),
				    correctId // expected target
				);
			view.moveAllToActive();
			view.setIdToIncorrect(id);
			view.addIdsToCorrect(Set.of(correctId));
		}
	}
	
	void handleCorrectAnswers(Set<String> matches) {
		view.addIdsToCorrect(matches);
		view.setTextInTextField("");
	}

	/**
	 * Schreib-Modus im freien Spiel: das aufgegebene Element bleibt rot auf der Karte stehen.
	 *
	 * <p>Das Eingabefeld muss dabei wieder freigeschaltet werden — {@link #setCorrectText} hat es
	 * gesperrt, um die angezeigte Lösung nicht überschreibbar zu machen.</p>
	 */
	void handleMissedWrite(String id) {
		view.setIdToIncorrect(id);
		retryWrite();
	}

	void retryWrite() {
		view.setTextInTextField("");
		view.setTextFieldActive(true);
	}
	
	/**
	 * Leert die Karte: alle Formen zurück auf aktiv, kein Grün, kein Rot. Ein gemerkter Fehlgriff
	 * ist damit ebenfalls erledigt.
	 */
	void clearMap() {
		view.moveAllToActive();
		wrongAnswerSnapshot = null;
	}

	/**
	 * Nimmt den gezeigten Vergleich (ein rot, ein grün) zurück und stellt den Stand von vor dem
	 * Fehlgriff wieder her.
	 *
	 * <p>Ohne gemerkten Fehlgriff passiert nichts: Im Schreib-Modus endet auch eine per ESC
	 * aufgegebene Pause hier, und vor der stand kein Vergleich.</p>
	 *
	 * @param keepMissedRed die gesuchte, verfehlte Form bleibt rot stehen, statt in den alten Stand
	 *                      zurückzukehren
	 */
	void restoreBeforeWrongAnswer(boolean keepMissedRed) {
		if (wrongAnswerSnapshot == null)
			return;
		ShapeMapState base = wrongAnswerSnapshot.beforeMap();
		if (keepMissedRed) {
			base.incorrectShapes().add(wrongAnswerSnapshot.expectedId());
			base.activeShapes().remove(wrongAnswerSnapshot.expectedId());
		}
		view.setState(base);
		wrongAnswerSnapshot = null;
	}
	
	// ========================================
	// USER INPUT (from Panel)
	// ========================================
	
	void clickedMapElement(String id) {
		progress.elementClicked(id);
	}
	

	void typedText(String text) {
		progress.textInputChanged(text);
	}
}
