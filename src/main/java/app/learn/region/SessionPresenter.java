package app.learn.region;

import java.util.Set;

import app.learn.region.model.Mode;
import app.learn.region.model.SessionSpec;
import app.shared.model.ScreenView;
import app.learn.MapService;
import app.shared.model.ShapeMapState;
import app.shared.ui.RegionLearnView;

/**
 * Intermediary between RegionSessionView and the RegionSessionProgress.
 * Also holds a final sessionPaneContainer, which is shown in the MainWindow. In case of a skin change,
 * the sessionPane inside this container is recreated. The MainWindow won't realize this :-)
 */
public class SessionPresenter {
	// Wir speichern auch die Frage für Skinwechsel
	private record SavedState (ShapeMapState mapState, String text) {};
	private record WrongClickSnapshot(ShapeMapState beforeMap, String beforeText, String expectedId) {}
	public enum WrongClickResolution {
	    ROLLBACK_FOR_RETRY,          // Learning "Fortsetzen"
	    COMMIT_MISS_AND_CONTINUE     // FreePlay
	}
	
	private final RegionLearnView view;
	private final SessionSpec spec; // Benötigt für den Neuaufbau eines Panels bei skinChanged
	private final SessionProgress progress;
	private final boolean hard;

	private WrongClickSnapshot wrongClickSnapshot;
	private SavedState savedState;
	
	public SessionPresenter(SessionProgress progress, SessionSpec spec) {
		progress.setPresenter(this);
		view = new RegionLearnView(
				spec.getDeckType().getMapName(),
				spec.getDeckType().getCategory().toString(),
				MapService.getInstance().getMap(spec.getDeckType()).getShapeGeometries(),
				spec.getMode().getSubCategory() == Mode.SubCategory.CLICK,
				this::clickedMapElement,
				this::typedText);
		this.progress = progress;
		this.spec = spec;
		this.hard = spec.getMode().getEasyHard() == Mode.EasyHard.HARD;
	}
	
	public ScreenView getView() {
		return view.getView();
	}
	
	// ========================================
	// STEP EXECUTION (from Progress)
	// ========================================
	
	public void refresh() {
		savedState = new SavedState(view.getState(), view.getQuestion());
		view.rebuild(spec.getMode().getSubCategory() == Mode.SubCategory.CLICK);
		view.setState(savedState.mapState);
		view.setQuestion(savedState.text);
		savedState = null;
	}

	public void weWaitForClick(Set<String> ids) {
		view.addIdsToActive(ids);
		view.setMapActive(true);
	}
	
	public void weWaitForEliminationText(Set<String> ids) {
		view.addIdsToInactive(ids);
		view.setMapActive(false);
	}
	
	public void weWaitForWriteText(String id) {
		view.addIdsToMarked(Set.of(id));
		view.setMapActive(true);
	}
	
	public void prepareWriteSession(Set<String> ids) {
		view.addIdsToInactive(ids);
		view.setMapActive(false);
	}
	
	public void setCorrectText(String correctText) {
		view.setTextInTextField(correctText);
		view.setTextFieldActive(false);
	}
	
	public void showQuestion(String text) {
		view.setQuestion(text);
	}
	
	public void handleClickResult(String id, boolean correct, String correctId) {
		if (correct) {
			if (hard) {
				view.moveCorrectToActive();
			}
			view.addIdsToCorrect(Set.of(id));

		} else {
			wrongClickSnapshot = new WrongClickSnapshot(
				    view.getState(),
				    view.getQuestion(),
				    correctId // expected target
				);
			view.moveAllToActive();
			view.setIdToIncorrect(id);
			view.addIdsToCorrect(Set.of(correctId));
		}
	}
	
	public void handleCorrectAnswers(Set<String> matches) {
		view.addIdsToCorrect(matches);
		view.setTextInTextField("");
	}
	
	public void undoWrongClick(WrongClickResolution resolution) {
	    ShapeMapState base = wrongClickSnapshot.beforeMap();
	    if (resolution == WrongClickResolution.COMMIT_MISS_AND_CONTINUE) {
	    	base.incorrectShapes().add(wrongClickSnapshot.expectedId());
	    	base.activeShapes().remove(wrongClickSnapshot.expectedId());
	    }
	    view.setState(base);	    
	    wrongClickSnapshot = null;
	}
	
	// ========================================
	// USER INPUT (from Panel)
	// ========================================
	
	public void clickedMapElement(String id) {
		progress.elementClicked(id);
	}
	

	public void typedText(String text) {
		progress.textInputChanged(text);
	}
}
