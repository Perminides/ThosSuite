package app.presenter;

import java.util.Set;

import app.data.RegionMode;
import app.data.RegionSessionProgress;
import app.data.RegionSessionSpec;
import app.ui.components.ShapeMapPane.ShapeMapState;
import app.ui.panes.RegionSessionPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Intermediary between RegionSessionPane and the RegionSessionProgress.
 * Also holds a final sessionPaneContainer, which is shown in the MainWindow. In case of a skin change,
 * the sessionPane inside this container is recreated. The MainWindow won't realize this :-)
 */
public class RegionSessionPresenter {
	
	private record SavedState (ShapeMapState mapState, String text) {};
	
	private final StackPane sessionPaneContainer = new StackPane();
	private RegionSessionPane sessionPane;
	private final RegionSessionSpec spec; // Benötigt für den Neuaufbau eines Panels bei skinChanged
	private final RegionSessionProgress progress;
	private final boolean hard;
	private SavedState savedState;
	
	public RegionSessionPresenter(RegionSessionProgress progress, RegionSessionSpec spec) {
		progress.setPresenter(this);
		sessionPane = new RegionSessionPane(this, spec.getDeckType(), spec.getMode().getSubCategory() == RegionMode.SubCategory.CLICK);
		sessionPaneContainer.getChildren().add(sessionPane);
		this.progress = progress;
		this.spec = spec;
		this.hard = spec.getMode().getEasyHard() == RegionMode.EasyHard.HARD;
	}
	
	public Pane getView() {
		return sessionPaneContainer;
	}
	
	// ========================================
	// STEP EXECUTION (from Progress)
	// ========================================
	
	public void refresh() {
		savedState = new SavedState(sessionPane.getState(), sessionPane.getQuestion());
		this.sessionPane = new RegionSessionPane(this, spec.getDeckType(), spec.getMode().getSubCategory() == RegionMode.SubCategory.CLICK);
		sessionPane.setState(savedState.mapState, false);
		sessionPane.setQuestion(savedState.text);
		sessionPaneContainer.getChildren().setAll(sessionPane);
		savedState = null;
	}

	public void weWaitForClick(Set<String> ids) {
		sessionPane.addIdsToActive(ids);
		sessionPane.setMapActive(true);
	}
	
	public void weWaitForEliminationText(Set<String> ids) {
		sessionPane.addIdsToMarked(ids);
		sessionPane.setMapActive(false);
	}
	
	public void weWaitForWriteText(String id) {
		sessionPane.addIdsToActive(Set.of(id));
		sessionPane.setMapActive(true);
	}
	
	public void prepareWriteSession(Set<String> ids) {
		sessionPane.addIdsToMarked(ids);
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
			sessionPane.addIdsToCorrect(Set.of(id));

		} else {
			savedState = new SavedState(sessionPane.getState(), sessionPane.getQuestion());
			sessionPane.moveAllToActive();
			sessionPane.setIdToIncorrect(id);
			sessionPane.addIdsToCorrect(Set.of(correctId));
		}
	}
	
	public void handleCorrectAnswers(Set<String> matches) {
		sessionPane.addIdsToCorrect(matches);
		sessionPane.setTextInTextField("");
	}
	
	/**
	 * @param keepLastIncorrect -> Wenn true, dann wird das aktuell als korrekt dargestellte rot! Wurde ja nicht gefunden!
	 */
	public void undoClick(boolean keepLastIncorrect) {
		sessionPane.setState(savedState.mapState, keepLastIncorrect);
		sessionPane.setQuestion(savedState.text);
		savedState = null;
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
