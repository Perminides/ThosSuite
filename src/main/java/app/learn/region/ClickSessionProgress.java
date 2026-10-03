package app.learn.region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.learn.model.MapShape;
import app.learn.region.model.Mode;
import app.learn.region.model.SessionSpec;

public class ClickSessionProgress extends SessionProgress {
	
	private final Set<String> sessionRegions;
	private final List<MapShape> quizElements;
	private final Set<MapShape> notFound = new LinkedHashSet<>();
	private final boolean easy;
	private boolean isPaused = false;
	private int currentIndex = -1;
	private String lastClickedId = null;

	public ClickSessionProgress(Set<MapShape> regions, SessionSpec spec, RegionDeckService service,
			Runnable onFinished) {
		super(spec, service, onFinished);
		this.easy = spec.getMode().getEasyHard() == Mode.EasyHard.EASY;
		this.sessionRegions = new HashSet<>();
		for (MapShape region : regions) {
			sessionRegions.add(region.id());
		}
		quizElements = new ArrayList<>();
		for (MapShape region : regions) {
			if (!region.isPlayable())
				continue;
			
			quizElements.add(region);
		}
		Collections.shuffle(quizElements);
	}

	@Override
	public void start() {
		nextStep();
	}

	@Override
	public void resume() {
		if (spec.isPlaySession()) {
			sessionRegions.removeAll(MapShape.idsOf(notFound));
		} else {
			notFound.clear();
		}
			
		isPaused = false;

		presenter.undoWrongClick();
		
		if (!spec.isPlaySession()) {
		    // In einer Lernsession ist das hier ein starker Eingriff in die Logik. 
		    // Ich habe mich geweigert, die Session als falsch abzuspeichern. 
		    // Ok, dann muss ich aber noch einmal auf das korrekte Klicken...
		    currentIndex--;
		}
		
		nextStep();
	}

	/**
	 * Die Pause geht vor: nach einem Fehler heißt <em>jeder</em> Klick „weiter", auch der auf einen
	 * markierten Kreis.
	 *
	 * <p>Danach gilt bei „leicht": was nicht mehr in {@link #sessionRegions} steht, ist durch und
	 * nimmt keine Klicks mehr an. Dieselbe Menge geht an {@code weWaitForClick} — was also nicht
	 * mehr aktiv gesetzt wird, wird auch nicht mehr gewertet. Bei „schwer" ist das Gegenteil richtig:
	 * dort sind alle Kreise unsichtbar und damit alle potenziell falsch, ein Treffer auf etwas
	 * längst Erledigtes ist ein echter Fehlgriff.</p>
	 */
	@Override
	public void elementClicked(String id) {
		if (isPaused) {
			endPause();
		} else {
			if (easy && !sessionRegions.contains(id))
				return;

			lastClickedId = id;
			if (quizElements.get(currentIndex).id().equals(id)) {
				presenter.handleClickResult(id, true, null);
				sessionRegions.remove(id);
				nextStep();
			} else {
				notFound.add(quizElements.get(currentIndex));
				isPaused = true;
				presenter.handleClickResult(id, false, quizElements.get(currentIndex).id());
			}
		}
	}

	@Override
	public void endPause() { // Durch Klick im Presenter oder Pause-Taste 
		if (spec.isPlaySession()) {
			isPaused = false;
			resume();
		} else if (isPaused) {
			finishIncorrect("Statt " + nameOf(quizElements.get(currentIndex)) + " wurde " + getNameForId(lastClickedId) + " geklickt.",
					true, quizElements.get(currentIndex).id());
		}
	}
	
	private void nextStep() {
		currentIndex++;
		if (currentIndex >= quizElements.size()) {
			if (notFound.isEmpty()) {           // alles gefunden — im Lernmodus der einzige Weg hierher
				finishCorrect();
			} else if (spec.isPlaySession()) {  // freies Spiel mit Fehlern: die listen wir auf
				finishWithMisses("Folgende Elemente wurden nicht erkannt:", notFound);
			} else {
				throw new RuntimeException("Moment. Entweder wird ein falscher Klick zurückgenommen oder es wird ohne nextStep beendet. Hierhin dürfte der Code nie kommen. Untersuchen!");
			}
		}
		else {
			presenter.showQuestion(nameOf(quizElements.get(currentIndex)));
			presenter.weWaitForClick(sessionRegions);
		}
	}
	
	/**
	 * Der Name zu einer angeklickten Form. Klicks kommen nur aus dem Spielfeld, und das ist genau
	 * die Menge, aus der {@code quizElements} gebaut wird — gefunden wird also immer etwas.
	 */
	private String getNameForId(String clickedId) {
		for (MapShape shape : quizElements)
			if (shape.id().equals(clickedId))
				return nameOf(shape);
		throw new RuntimeException("Geklickt wurde eine Form, die gar nicht im Spiel ist: " + clickedId);
	}

	@Override
	public boolean hasProgressed() {
		return currentIndex > 0;
	}
}
