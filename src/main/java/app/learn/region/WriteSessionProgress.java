package app.learn.region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import app.learn.model.MapShape;
import app.learn.region.model.Mode;
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
public class WriteSessionProgress implements SessionProgress {

	private final RegionSession session;
	private final SessionSpec spec;
	private final Set<MapShape> sessionRegions;
	private final List<MapShape> toLearnRegions;

	/** Nur im freien Spiel gefüllt: was per ESC aufgegeben wurde, in der Reihenfolge des Fragens. */
	private final List<MapShape> notFound = new ArrayList<>();

	private SessionPresenter presenter;
	private Mode mode;
	private int currentIndex = -1;
	private boolean isEndPause = false;

	public WriteSessionProgress(Set<MapShape> regions, SessionSpec spec, RegionSession regionSession) {
		this.sessionRegions = regions;
		this.session = regionSession;
		this.spec = spec;
		this.mode = spec.getMode();
		toLearnRegions = new ArrayList<>(sessionRegions);
		Collections.shuffle(toLearnRegions);
	}

	@Override
	public void setPresenter(SessionPresenter regionSessionPresenter) {
		this.presenter = regionSessionPresenter;
	}

	@Override
	public void start() {
		presenter.prepareWriteSession(getIds(sessionRegions));
		currentIndex++;
		presenter.weWaitForWriteText(toLearnRegions.get(currentIndex).id());
	}

	@Override
	public void cancel() {
		if (isEndPause) {
			endPause();
			return;
		}

		presenter.setCorrectText(nameOf(toLearnRegions.get(currentIndex)));
		isEndPause = true;
	}

	@Override
	public void textInputChanged(String text) {
		MapShape currentRegion = toLearnRegions.get(currentIndex);

		boolean isMatch = switch (mode) {
		case WRITE_BOTH -> currentRegion.isMatching(text);
		case WRITE_CAPITAL -> currentRegion.isMatchingCapital(text);
		case WRITE_REGION -> currentRegion.isMatchingRegion(text);
		default -> throw new RuntimeException("Das kommt jetzt einigermaßen unerwartet :)");
		};

		if (!isMatch)
			return;

		presenter.handleCorrectAnswers(Set.of(currentRegion.id()));
		nextStep();
	}

	@Override
	public void endPause() {
		if (!isEndPause)
			return;

		MapShape currentRegion = toLearnRegions.get(currentIndex);

		if (!spec.isPlaySession()) {
			session.end(false, currentRegion.id(), "Folgendes Element nicht erkannt: \n\n" + nameOf(currentRegion), false);
			return;
		}

		notFound.add(currentRegion);
		presenter.handleMissedWrite(currentRegion.id());
		isEndPause = false;
		nextStep();
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
			return;
		}

		if (!spec.isPlaySession()) {
			session.end(true, null, null, false);
			return;
		}

		if (notFound.isEmpty()) {
			session.end(true, null, "Super gemacht!", false);
			return;
		}

		String result = "Folgende Elemente wurden nicht erkannt: \n\n";
		for (MapShape miss : notFound)
			result += nameOf(miss) + "\n";
		session.end(false, "", result, false);
	}

	/** Der gesuchte Text zu einem Element — je nach Modus Region, Hauptstadt oder beides. */
	private String nameOf(MapShape region) {
		if (mode == Mode.WRITE_BOTH)
			return region.regionName() + " (" + region.capitalName() + ")";
		if (mode == Mode.WRITE_CAPITAL)
			return region.capitalName();
		return region.regionName();
	}
}
