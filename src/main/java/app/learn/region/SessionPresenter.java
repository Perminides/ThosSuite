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
	private final RegionLearnView view;
	private final SessionSpec spec; // Benötigt für den Neuaufbau eines Panels bei skinChanged
	private final SessionProgress progress;

	private ShapeMapState beforeWrongAnswer;
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
			beforeWrongAnswer = view.getState();
			view.moveAllToActive();
			view.setIdToIncorrect(id);
			view.addIdsToCorrect(Set.of(correctId));
		}
	}
	
	void handleCorrectAnswers(Set<String> matches) {
		view.addIdsToCorrect(matches);
		view.setTextInTextField("");
	}

	/** Hebt die Sperre aus {@link #setCorrectText} auf und leert das Feld. */
	void retryWrite() {
		view.setTextInTextField("");
		view.setTextFieldActive(true);
	}

	void markMissed(String id) {
		view.setIdToIncorrect(id);
	}

	void clearMap() {
		view.moveAllToActive();
	}

	/**
	 * Stellt den Stand von vor dem Fehlgriff wieder her. Ohne Fehlgriff passiert nichts — das
	 * Benennen landet hier auch nach ESC im freien Spiel.
	 */
	void restoreBeforeWrongAnswer() {
		if (beforeWrongAnswer == null)
			return;
		view.setState(beforeWrongAnswer);
		beforeWrongAnswer = null;
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
