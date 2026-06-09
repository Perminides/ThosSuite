package app.learn.region;

import java.util.Set;

import app.learn.ShapeMapPane.ShapeMapState;
import app.learn.region.model.Mode;
import app.learn.region.model.SessionSpec;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Intermediary between RegionSessionPane and the RegionSessionProgress.
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
	
	private final StackPane sessionPaneContainer = new StackPane();
	private SessionPane sessionPane;
	private final SessionSpec spec; // Benötigt für den Neuaufbau eines Panels bei skinChanged
	private final SessionProgress progress;
	private final boolean hard;

	private WrongClickSnapshot wrongClickSnapshot;
	private SavedState savedState;
	
	public SessionPresenter(SessionProgress progress, SessionSpec spec) {
		progress.setPresenter(this);
		sessionPane = new SessionPane(this, spec.getDeckType(), spec.getMode().getSubCategory() == Mode.SubCategory.CLICK);
		sessionPaneContainer.getChildren().add(sessionPane);
		this.progress = progress;
		this.spec = spec;
		this.hard = spec.getMode().getEasyHard() == Mode.EasyHard.HARD;
	}
	
	public Pane getView() {
		return sessionPaneContainer;
	}
	
	// ========================================
	// STEP EXECUTION (from Progress)
	// ========================================
	
	public void refresh() {
		savedState = new SavedState(sessionPane.getState(), sessionPane.getQuestion());
		this.sessionPane = new SessionPane(this, spec.getDeckType(), spec.getMode().getSubCategory() == Mode.SubCategory.CLICK);
		sessionPane.setState(savedState.mapState);
		sessionPane.setQuestion(savedState.text);
		sessionPaneContainer.getChildren().setAll(sessionPane);
		savedState = null;
	}

	public void weWaitForClick(Set<String> ids) {
		sessionPane.addIdsToActive(ids);
		sessionPane.setMapActive(true);
	}
	
	public void weWaitForEliminationText(Set<String> ids) {
		sessionPane.addIdsToInactive(ids);
		sessionPane.setMapActive(false);
	}
	
	public void weWaitForWriteText(String id) {
		sessionPane.addIdsToMarked(Set.of(id));
		sessionPane.setMapActive(true);
	}
	
	public void prepareWriteSession(Set<String> ids) {
		sessionPane.addIdsToInactive(ids);
		sessionPane.setMapActive(false);
	}
	
	public void setCorrectText(String correctText) {
		sessionPane.setTextInTextField(correctText);
		sessionPane.setTextFieldActive(false);
	}
	
	public void showQuestion(String text) {
		sessionPane.setQuestion(text);
	}
	
	public void handleClickResult(String id, boolean correct, String correctId) {
		if (correct) {
			if (hard) {
				sessionPane.moveCorrectToActive();
			}
			sessionPane.addIdsToCorrect(Set.of(id));

		} else {
			wrongClickSnapshot = new WrongClickSnapshot(
				    sessionPane.getState(),
				    sessionPane.getQuestion(),
				    correctId // expected target
				);
			sessionPane.moveAllToActive();
			sessionPane.setIdToIncorrect(id);
			sessionPane.addIdsToCorrect(Set.of(correctId));
		}
	}
	
	public void handleCorrectAnswers(Set<String> matches) {
		sessionPane.addIdsToCorrect(matches);
		sessionPane.setTextInTextField("");
	}
	
	public void undoWrongClick(WrongClickResolution resolution) {
	    ShapeMapState base = wrongClickSnapshot.beforeMap();
	    if (resolution == WrongClickResolution.COMMIT_MISS_AND_CONTINUE) {
	    	base.incorrectShapes().add(wrongClickSnapshot.expectedId());
	    	base.activeShapes().remove(wrongClickSnapshot.expectedId());
	    }
	    sessionPane.setState(base);	    
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
