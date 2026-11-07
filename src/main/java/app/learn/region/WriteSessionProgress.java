package app.learn.region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import app.learn.model.MapShape;
import app.learn.region.model.SessionSpec;

/**
 * Der Schreib-Modus: die Karte markiert ein Element, der Benutzer tippt dessen Namen.
 *
 * <p>ESC zeigt den gesuchten Namen im Eingabefeld und hält die Session an. Was das <b>Beenden</b>
 * dieser Pause bedeutet, hängt davon ab, ob gelernt oder gespielt wird:</p>
 *
 * <ul>
 *   <li><b>Lernsession:</b> vorbei. Ein nicht gewusstes Element ist das Ergebnis der Session — sie
 *       wird als falsch gewertet und gespeichert.</li>
 *   <li><b>Freies Spiel:</b> das Element bleibt rot auf der Karte stehen, und es geht mit dem
 *       nächsten weiter. Am Ende steht die Liste dessen, was nicht erkannt wurde.</li>
 * </ul>
 *
 * <p>Dieselbe Zweiteilung wie im Klick-Modus ({@link ClickSessionProgress}): dort wird ein
 * Fehlgriff im freien Spiel ebenfalls festgeschrieben, statt die Session abzubrechen.</p>
 */
public class WriteSessionProgress extends SessionProgress {

	private final Set<MapShape> sessionRegions;
	private final List<MapShape> toLearnRegions;

	/** Nur im freien Spiel gefüllt: was verfehlt oder per ESC aufgegeben wurde, in der Reihenfolge des Fragens. */
	private final List<MapShape> notFound = new ArrayList<>();

	private int currentIndex = -1;
	private boolean paused = false;

	public WriteSessionProgress(Set<MapShape> regions, SessionSpec spec, RegionDeckService service,
			Runnable onFinished) {
		super(spec, service, onFinished);
		this.sessionRegions = regions;
		toLearnRegions = new ArrayList<>(sessionRegions);
		Collections.shuffle(toLearnRegions);
	}

	@Override
	public void start() {
		presenter.prepareWriteSession(MapShape.idsOf(sessionRegions));
		currentIndex++;
		presenter.weWaitForWriteText(toLearnRegions.get(currentIndex).id());
	}

	@Override
	public void cancel() {
		if (paused) {
			endPause();
		} else {
			presenter.setCorrectText(nameOf(toLearnRegions.get(currentIndex)));
			paused = true;
		}
	}

	@Override
	public void textInputChanged(String text) {
		MapShape currentRegion = toLearnRegions.get(currentIndex);

		if (matches(currentRegion, text)) {
			presenter.handleCorrectAnswers(Set.of(currentRegion.id()));
			nextStep();
			return;
		}

		MapShape wrongRegion = findWrongRegion(currentRegion, text);
		if (wrongRegion == null)
			return;

		presenter.setCorrectText(nameOf(currentRegion));
		presenter.handleClickResult(wrongRegion.id(), false, currentRegion.id());
		paused = true;
		if (!spec.isPlaySession())
			finishIncorrect("Statt " + nameOf(currentRegion) + " wurde " + nameOf(wrongRegion) + " eingegeben.",
					true, currentRegion.id());
	}

	@Override
	void resume() {
		MapShape currentRegion = toLearnRegions.get(currentIndex);
		paused = false;
		presenter.undoWrongClick();
		presenter.weWaitForWriteText(currentRegion.id());
		presenter.retryWrite();
	}

	private MapShape findWrongRegion(MapShape currentRegion, String text) {
		if (isPrefixOfCurrent(currentRegion, text))
			return null;
		for (MapShape region : toLearnRegions)
			if (!region.equals(currentRegion) && matches(region, text))
				return region;
		return null;
	}

	private boolean isPrefixOfCurrent(MapShape currentRegion, String text) {
		String normalized = text.toLowerCase().trim();
		if (normalized.isEmpty())
			return false;
		return switch (spec.getMode().getCapitalOrRegion()) {
		case CAPITAL -> isPrefix(normalized, currentRegion.capitalName(), currentRegion.altCapitalNames());
		case REGION -> isPrefix(normalized, currentRegion.regionName(), currentRegion.altRegionNames());
		case BOTH -> isPrefix(normalized, currentRegion.capitalName(), currentRegion.altCapitalNames())
				|| isPrefix(normalized, currentRegion.regionName(), currentRegion.altRegionNames());
		};
	}

	private boolean isPrefix(String normalized, String name, Set<String> alternatives) {
		if (name.toLowerCase().trim().startsWith(normalized))
			return true;
		if (alternatives != null)
			for (String alternative : alternatives)
				if (alternative.toLowerCase().trim().startsWith(normalized))
					return true;
		return false;
	}

	@Override
	public void endPause() {
		if (!paused)
			return;

		MapShape currentRegion = toLearnRegions.get(currentIndex);

		if (!spec.isPlaySession()) {
			finishIncorrect("Folgendes Element nicht erkannt: \n\n" + nameOf(currentRegion), false, currentRegion.id());
		} else {
			notFound.add(currentRegion);
			presenter.undoWrongWrite();
			presenter.handleMissedWrite(currentRegion.id());
			paused = false;
			nextStep();
		}
	}

	@Override
	public boolean hasProgressed() {
		return currentIndex > 0;
	}

	/** Weiter zum nächsten Element — oder, wenn keins mehr kommt, Schluss samt Auswertung. */
	private void nextStep() {
		currentIndex++;
		if (currentIndex < toLearnRegions.size()) {
			presenter.weWaitForWriteText(toLearnRegions.get(currentIndex).id());
		} else if (notFound.isEmpty()) {    // alles erkannt — im Lernmodus der einzige Weg hierher
			finishCorrect();
		} else if (spec.isPlaySession()) {  // freies Spiel mit Fehlern: die listen wir auf
			finishWithMisses("Folgende Elemente wurden nicht erkannt:", notFound);
		} else {
			throw new RuntimeException("Im Lernmodus endet die Session beim ersten Fehler — notFound kann hier gar nicht gefüllt sein. Untersuchen!");
		}
	}
}
