package app.learn.region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import app.learn.model.MapShape;
import app.learn.region.SessionPresenter.WrongClickResolution;
import app.learn.region.model.Mode;
import app.learn.region.model.SessionSpec;

public class ClickSessionProgress implements SessionProgress{
	
	public record QuizElement(String toFind, String shapeId) {
		String getToFind() {
			return toFind;
		}
		
		String getShapeId() {
			return shapeId;
		}
	};
	
	private final RegionSession session;
	private final Set<String> sessionRegions;
	private final List<QuizElement> quizElements;
	private final Set<String> notFound = new TreeSet<>();
	private final SessionSpec spec;
	private boolean isPaused = false;
	private int currentIndex = -1;
	private String lastClickedId = null;
	
	private SessionPresenter presenter;

	public ClickSessionProgress(Set<MapShape> regions, SessionSpec spec, RegionSession regionSession) {
		this.session = regionSession;
		this.spec = spec;
		this.sessionRegions = new HashSet<>();
		for (MapShape region : regions) {
			sessionRegions.add(region.id());
		}
		quizElements = new ArrayList<>();
		for (MapShape region : regions) {
			if (!region.isPlayable())
				continue;
			
			String name = spec.getMode().getCapitalOrRegion() == Mode.CapitalOrRegion.CAPITAL ? region.capitalName() : region.regionName();
			quizElements.add(new QuizElement(name, region.id()));
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
			sessionRegions.removeAll(notFound);
		} else {
			notFound.clear();
		}
			
		isPaused = false;

		presenter.undoWrongClick(
			    spec.isPlaySession()
			        ? WrongClickResolution.COMMIT_MISS_AND_CONTINUE
			        : WrongClickResolution.ROLLBACK_FOR_RETRY
			);
		
		if (!spec.isPlaySession()) {
		    // In einer Lernsession ist das hier ein starker Eingriff in die Logik. 
		    // Ich habe mich geweigert, die Session als falsch abzuspeichern. 
		    // Ok, dann muss ich aber noch einmal auf das korrekte Klicken...
		    currentIndex--;
		}
		
		nextStep();
	}

	@Override
	public void elementClicked(String id) {
		lastClickedId = id;
		if (isPaused) {
			endPause();
			return;
		}
		
		if (quizElements.get(currentIndex).getShapeId().equals(id)) {
			presenter.handleClickResult(id, true, null);
			sessionRegions.remove(id);
			nextStep();
		} else {
			notFound.add(quizElements.get(currentIndex).getShapeId());
			isPaused = true; 
			presenter.handleClickResult(id, false, quizElements.get(currentIndex).getShapeId());
		}
	}

	@Override
	public void endPause() { // Durch Klick im Presenter oder Pause-Taste 
		if (spec.isPlaySession()) {
			isPaused = false;
			resume();
		} else if (isPaused) {
			session.end(false, quizElements.get(currentIndex).shapeId(), "Statt " + quizElements.get(currentIndex).toFind() + " wurde " + getNameForId(lastClickedId) + " geklickt.", true);
		}
	}
	
	@Override
	public boolean isPause() {
		return !notFound.isEmpty();
	}
	
	@Override
	public void setPresenter(SessionPresenter presenter) {
		this.presenter = presenter;
	}
	
	private void nextStep() {
		currentIndex++;
		if (currentIndex >= quizElements.size()) {
			if (spec.isPlaySession()) {
				if (notFound.isEmpty()) 	// Freies Spiel ohne Fehler. Super!
					session.end(true, null, "Super gemacht!", false);
				else { 							// Freies Spiel mit Fehlern. Die listen wir auf
					String result = "Folgende Elemente wurden nicht erkannt: \n\n";
					for (String wrongId : notFound) {
						result = result + getNameForId(wrongId) + "\n";
					}
					session.end(false, "", result, false);
				}
			} else {
				if (notFound.isEmpty()) 	// Lernsession korrekt beantwortet
					session.end(true, null, null, false);
				else {
					throw new RuntimeException("Moment. Entweder wird ein falscher Klick zurückgenommen oder es wird ohne nextStep beendet. Hierhin dürfte der Code nie kommen. Untersuchen!");
				}
			}
		}
		else {
			presenter.showQuestion(quizElements.get(currentIndex).getToFind());
			presenter.weWaitForClick(sessionRegions);
		}
	}
	
	private String getNameForId(String wrongClicked) {
		return getNameForId (Set.of(wrongClicked));
	}

	
	private String getNameForId(Set<String> wrongClicked) {
		if (wrongClicked.size() != 1)
			throw new RuntimeException("Moment. Wieso gibt es mehr als einen flaschen Klick hier?");
		for (QuizElement element : quizElements) {
			if (wrongClicked.contains(element.shapeId))
				return element.toFind();
		}
		return "";
	}

	@Override
	public boolean hasProgressed() {
		return currentIndex > 0;
	}
}
