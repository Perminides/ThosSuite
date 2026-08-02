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
				new RegionCallbacks(this::clickedMapElement, this::typedText));
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
				view.moveResolvedToActive();
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

	/**
	 * Schreib-Modus im freien Spiel: das aufgegebene Element bleibt rot auf der Karte stehen.
	 *
	 * <p>Das Eingabefeld muss dabei wieder freigeschaltet werden — {@link #setCorrectText} hat es
	 * gesperrt, um die angezeigte Lösung nicht überschreibbar zu machen.</p>
	 */
	public void handleMissedWrite(String id) {
		view.setIdToIncorrect(id);
		view.setTextInTextField("");
		view.setTextFieldActive(true);
	}
	
	/**
	 * Der gezeigte Vergleich (ein rot, ein grün) verschwindet, sobald es weitergeht.
	 *
	 * <p>Bei „schwer" bleibt danach <b>nichts</b> stehen — weder der verpasste Kreis noch das Grün
	 * früherer Treffer. Es gibt dort also auch nichts wiederherzustellen, die Karte wird schlicht
	 * geleert. Würde man stattdessen den Schnappschuss von vor dem Falsch-Klick einspielen, käme
	 * altes Grün zurück und neues Rot käme dazu; nach zwei Fehlern in Folge stünde die halbe Karte
	 * bunt da.</p>
	 *
	 * <p>Bei „leicht" ist genau das Gegenteil gewollt: der Stand von vorher lebt weiter, und der
	 * verpasste Kreis bleibt im freien Spiel rot markiert.</p>
	 */
	public void undoWrongClick(WrongClickResolution resolution) {
	    if (hard) {
	        view.moveAllToActive();
	    } else {
	        ShapeMapState base = wrongClickSnapshot.beforeMap();
	        if (resolution == WrongClickResolution.COMMIT_MISS_AND_CONTINUE) {
	        	base.incorrectShapes().add(wrongClickSnapshot.expectedId());
	        	base.activeShapes().remove(wrongClickSnapshot.expectedId());
	        }
	        view.setState(base);
	    }
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
