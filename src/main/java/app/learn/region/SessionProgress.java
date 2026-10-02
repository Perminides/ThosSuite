package app.learn.region;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import app.learn.model.LearnStat;
import app.learn.model.MapShape;
import app.learn.region.model.SessionResult;
import app.learn.region.model.SessionSpec;
import app.shared.AppClock;

/**
 * Der Ablauf einer Regions-Session — gemeinsamer Unterbau der drei Modi (Click, Elimination, Write).
 *
 * <p>Hier steht, was alle drei teilen: die Bezüge zu Session, Spec und Presenter, und das
 * Fortschreiben des Lernstands. Was ein Modus exklusiv hat — welche Formen noch offen sind, was
 * nicht gefunden wurde, ob gerade pausiert wird —, bleibt in seiner Klasse; diese Felder sind je
 * Modus verschieden typisiert und lassen sich nicht sinnvoll zusammenlegen.</p>
 *
 * <p><b>Warum abstrakte Klasse und kein Interface:</b> Ein Interface kann keine Felder halten, und
 * genau die sind der geteilte Teil — vorher stand dasselbe Trio dreimal da. Die Methoden, die nur
 * manche Modi kennen, sind hier leere Methoden statt {@code default}-Methoden, gleiche Wirkung.</p>
 *
 * <p>Damit steht Region so da wie Anki: der Ablauf in einer Progress-Klasse, das Speichern darin,
 * und die Schale ({@code RegionSession} bzw. {@code AnkiDeckSession}) orchestriert nur noch.</p>
 * 
 * <p>Und wie bei Anki auch wird dem Progress der ganze DeckService übergeben. Das finde ich etwas
 * unschön. Aber man kann halt auch nicht ständig Callbacks einführen, nur weil man was unschön findet.</p>
 */
public abstract class SessionProgress {

	protected final SessionSpec spec;
	protected final RegionDeckService service;

	/**
	 * „Ich bin fertig" — mehr sagt der Progress der Schale nicht. Das Ergebnis holt sie sich danach
	 * über {@link #getResult()} ab. Bewusst ein {@code Runnable} und keine Referenz auf die Session:
	 * so kennt der Ablauf seine Schale nicht, genau wie auf der Anki-Seite.
	 */
	private final Runnable onFinished;

	protected SessionPresenter presenter;

	/** Steht erst fest, wenn der Modus fertig ist. Vorher gibt es nichts abzuholen. */
	private SessionResult result;
	private String wrongId;

	protected SessionProgress(SessionSpec spec, RegionDeckService service, Runnable onFinished) {
		this.spec = spec;
		this.service = service;
		this.onFinished = onFinished;
	}

	void setPresenter(SessionPresenter presenter) {
		this.presenter = presenter;
	}

	// ========================================
	// Fertig — die zwei Ausgänge jedes Modus
	// ========================================

	/**
	 * Geschafft. Es gibt nichts zu erklären und nichts fortzusetzen — deshalb braucht dieser Ausgang
	 * keine Angaben.
	 */
	protected void finishCorrect() {
		finish(new SessionResult(true, null, false), null);
	}

	/**
	 * Nicht geschafft.
	 *
	 * @param incorrectText was dem Nutzer erklärt wird — ohne das hier geht es nicht
	 * @param allowResume   darf er statt zu beenden weiterspielen
	 * @param wrongId       die falsch beantwortete Form fürs Fortschreiben, oder {@code null}
	 */
	protected void finishIncorrect(String incorrectText, boolean allowResume, String wrongId) {
		if (incorrectText == null || incorrectText.isBlank())
			throw new RuntimeException("Hase, Du solltest schon eine Rückmeldung geben, was falsch war, oder?");
		finish(new SessionResult(false, incorrectText, allowResume), wrongId);
	}

	/**
	 * Nicht geschafft, und zwar an mehreren Elementen — die Liste dessen, was verpasst wurde.
	 *
	 * <p>Alle drei Modi enden so: der Klick-Modus und der Schreib-Modus im freien Spiel, die
	 * Elimination bei ESC. Fortgeschrieben wird dabei nichts, deshalb ohne {@code wrongId}.</p>
	 */
	protected void finishWithMisses(String einleitung, Collection<MapShape> misses) {
		String text = einleitung + "\n\n";
		for (MapShape miss : misses)
			text = text + nameOf(miss) + "\n";
		finishIncorrect(text, false, null);
	}

	private void finish(SessionResult result, String wrongId) {
		this.result = result;
		this.wrongId = wrongId;
		onFinished.run();
	}

	/** Das Ergebnis, sobald es feststeht. Vorher zu fragen ist ein Fehler, kein Sonderfall. */
	SessionResult getResult() {
		if (result == null)
			throw new RuntimeException("Das Ergebnis steht erst fest, wenn die Session zu Ende ist. Zu früh gefragt.");
		return result;
	}

	// ========================================
	// Was jeder Modus können muss
	// ========================================

	abstract void start();

	abstract boolean hasProgressed();

	// ========================================
	// Was nur manche Modi kennen — leer heißt: gibt es hier nicht
	// ========================================

	/** Nach einem Fehlgriff weiterspielen. Nur Click. */
	void resume() {}

	/** Die Pause nach einem Fehlgriff beenden. Nicht bei Elimination. */
	void endPause() {}

	/** ESC. Nicht im Klick-Modus. Bei Elimination endet damit die Session selbst, samt Liste
	 *  dessen, was nicht eliminiert wurde. */
	void cancel() {}

	/** Klick auf die Karte. Nur Click. */
	void elementClicked(String id) {}

	/** Eingabe im Textfeld. Elimination und Write. */
	void textInputChanged(String text) {}

	// ========================================
	// Lernstand fortschreiben
	// ========================================

	/**
	 * Schreibt das Ergebnis der Session fort. Nur im Lernmodus aufzurufen — im freien Spiel gibt es
	 * nichts zu speichern.
	 *
	 * <p>Liefert das Datum zurück, statt es über ein Feld nachreichbar zu machen: Ein Feld, das nur
	 * zwischen zwei Aufrufen lebt, wäre Zustand ohne Zweck.</p>
	 *
	 * @return wann das Deck wieder dran ist — das zeigt der Ausblick an
	 */
	LocalDate save() {
		boolean correct = getResult().correct();
		LearnStat stats = service.getLearnStat(spec);

		if (!stats.isDueToday())
			throw new RuntimeException("Sicherheitsnetz eingebaut. Diese Region war gar nicht dran. Und ich soll den Fortschritt überschreiben? Mache ich ungern!");

		stats.setLevel(stats.calculateNewLevel(correct, false));
		stats.setLastPlayed(AppClock.TODAY);
		if (!correct)
			stats.incrementWrongCount();
		service.savePlayedSession(spec, stats, correct, wrongId);
		return stats.getDueDate();
	}

	// ========================================
	// Hilfe
	// ========================================

	/**
	 * Der Name eines Elements, wie ihn dieser Modus meint — der Hauptort, die Region, oder bei
	 * {@code BOTH} beide als {@code Region (Hauptort)}.
	 *
	 * <p>Dieselbe Form in jedem Modus, der beide Namen meint: {@code WRITE_BOTH} zeigt sie per ESC
	 * im Eingabefeld, {@code ELIMINATION_BOTH} in der Liste beim Abbruch.</p>
	 */
	protected String nameOf(MapShape shape) {
		return switch (spec.getMode().getCapitalOrRegion()) {
		case CAPITAL -> shape.capitalName();
		case REGION -> shape.regionName();
		case BOTH -> shape.regionName() + " (" + shape.capitalName() + ")";
		};
	}

	/**
	 * Ob der eingegebene Text das gesuchte Element trifft — je nach Achse des Modus der Name des
	 * Hauptorts, der Name der Region oder beides.
	 *
	 * <p>Geschaltet über {@code getCapitalOrRegion()} und nicht über die Modus-Konstante: so deckt
	 * ein neuer Modus sich selbst ab, sobald er seine Achse angibt. Alle drei Achsenwerte stehen
	 * hier, deshalb braucht es kein {@code default} — ein vierter wäre ein Compilerfehler.</p>
	 */
	protected boolean matches(MapShape shape, String text) {
		return switch (spec.getMode().getCapitalOrRegion()) {
		case CAPITAL -> shape.isMatchingCapital(text);
		case REGION -> shape.isMatchingRegion(text);
		case BOTH -> shape.isMatching(text);
		};
	}

	protected Set<String> getIds(Set<MapShape> regions) {
		Set<String> result = new HashSet<>();
		for (MapShape region : regions)
			result.add(region.id());
		return result;
	}
}
