package app.presenter;

import java.util.Set;

import app.data.RegionMode;
import app.data.RegionSessionProgress;
import app.data.RegionSessionSpec;
import app.ui.MainWindow;
import app.ui.components.ShapeMapState;
import app.ui.panels.RegionSessionPanel;

public class RegionSessionPresenter {
	
	private record SavedState (ShapeMapState mapState, String text) {};
	
	private final MainWindow mainWindow; // Benötigt für den Neuaufbau eines Panels bei skinChanged
	private final RegionSessionSpec spec; // Benötigt für den Neuaufbau eines Panels bei skinChanged
	private final RegionSessionProgress progress;
	private final boolean hard;
	private RegionSessionPanel panel;
	private SavedState savedState;
	
	public RegionSessionPresenter(MainWindow  mainWindow, RegionSessionProgress progress, RegionSessionSpec spec) {
		progress.setPresenter(this);
		this.panel = new RegionSessionPanel(mainWindow, this, spec.getDeckType(), spec.getMode().getSubCategory() == RegionMode.SubCategory.CLICK);
		this.mainWindow = mainWindow;
		this.progress = progress;
		this.spec = spec;
		this.hard = spec.getMode().getEasyHard() == RegionMode.EasyHard.HARD;
	}
	
	// ========================================
	// STEP EXECUTION (from Progress)
	// ========================================

	public void start() {
		panel.show();
	}
	
	public void refresh() {
		savedState = new SavedState(panel.getState(), panel.getQuestion());
		this.panel = new RegionSessionPanel(mainWindow, this, spec.getDeckType(), spec.getMode().getSubCategory() == RegionMode.SubCategory.CLICK);
		panel.setState(savedState.mapState);
		panel.setQuestion(savedState.text);
		panel.show();
		savedState = null;
	}

	public void weWaitForClick(Set<String> ids) {
		panel.addIdsToActive(ids);
		panel.setMapActive(true);
	}
	
	public void weWaitForEliminationText(Set<String> ids) {
		panel.addIdsToMarked(ids);
		panel.setMapActive(false);
	}
	
	public void weWaitForWriteText(String id) {
		panel.addIdsToActive(Set.of(id));
		panel.setMapActive(true);
	}
	
	public void prepareWriteSession(Set<String> ids) {
		panel.addIdsToMarked(ids);
		panel.setMapActive(false);
	}
	
	public void setCorrectText(String correctText) {
		panel.setTextInTextField(correctText);
		panel.setTextFieldActive(false);
	}
	
	public void showQuestion(String text) {
		panel.setQuestion(text);
	}
	
	public void handleClickResult(String id, boolean correct, String correctId) {
		if (correct) {
			if (hard) {
				panel.moveCorrectToActive();
			}
			panel.addIdsToCorrect(Set.of(id));
			panel.addIdsToCorrect(Set.of(id));

		} else {
			savedState = new SavedState(panel.getState(), panel.getQuestion());
			panel.moveAllToActive();
			panel.setIdToIncorrect(id);
			panel.addIdsToCorrect(Set.of(correctId));
		}
	}
	
	public void handleCorrectAnswers(Set<String> matches) {
		panel.addIdsToCorrect(matches);
		panel.setTextInTextField("");
	}
	
	public void undoClick() {
		panel.setState(savedState.mapState);
		panel.setQuestion(savedState.text);
		savedState = null;
	}
	
	// ========================================
	// USER INPUT (from Panel)
	// ========================================
	
	public void clickedMapElement(String id) {
		if (progress.isPause())
			progress.endPause();
		else
			progress.elementClicked(id);
	}
	

	public void typedText(String text) {
		progress.textInputChanged(text);
	}
}
