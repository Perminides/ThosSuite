package app.learn.anki;

import java.util.List;

import app.learn.anki.model.Card;
import app.learn.anki.model.CardSortOrder;
import app.learn.model.Deck;
import app.learn.model.SessionProgressCounter;
import app.shared.Config;
import app.shared.Log;
import app.shared.model.ButtonEnum;
import app.shared.model.Screen;
import app.shared.model.ScreenView;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.Alerts;

/**
 * Schale der Anki-Lernsession. Verantwortlich für genau drei Dinge:
 *  1) Brücke zwischen Controller und dem eigentlichen Karten-Ablauf (AnkiSessionProgress),
 *  2) Ansprechpartner für den Controller (Screen),
 *  3) Lebenszyklus: orchestriert Speicher-Trigger und Zusammenfassungs-Alert.
 *
 * Die Schale hält den Progress und den Presenter; die View kommt über presenter.getView()
 * (ein ScreenView) — deshalb kein javafx mehr hier.
 */
public class AnkiDeckSession implements Screen {

	private final SessionProgress progress;
	private final SessionPresenter presenter;
	private final Runnable onSessionEnded;
	private final boolean isFreePlay;

	/**
	 * Eine Lern-Session: Karten in der eingestellten Sortierung, der Fortschritt wird am Ende
	 * gespeichert.
	 */
	public static AnkiDeckSession forLearning(List<Card> cards, Runnable onSessionEnded, AnkiDeckService service, Deck type) {
		return new AnkiDeckSession(cards, onSessionEnded, service, type, false);
	}

	/**
	 * Eine Session außer der Reihe: Karten gemischt, Sortier-Wechsel wirkungslos, und am Ende wird
	 * nichts gespeichert.
	 */
	public static AnkiDeckSession forFreePlay(List<Card> cards, Runnable onSessionEnded, AnkiDeckService service, Deck type) {
		return new AnkiDeckSession(cards, onSessionEnded, service, type, true);
	}

	private AnkiDeckSession(List<Card> cards, Runnable onSessionEnded, AnkiDeckService service, Deck type, boolean isFreePlay) {
		Log.info(this, "=== SESSION CONSTRUCTOR === Session@" + System.identityHashCode(this));
		this.onSessionEnded = onSessionEnded;
		this.isFreePlay = isFreePlay;
		CardSortOrder sortOrder = isFreePlay ? CardSortOrder.RANDOM : CardSortOrder.valueOf(Config.get("pref.sortOrder"));
		this.progress = new SessionProgress(cards, service, type, sortOrder, this::closeLoud);
		this.presenter = new SessionPresenter(type, progress); // registriert sich selbst am Progress via setPresenter(this)
	}

	@Override
	public void start() {
		Log.info(this, "=== SESSION START === Session@" + System.identityHashCode(this));
		progress.start();
	}

	@Override
	public void sortOrderChanged() {
		if (!isFreePlay)
			progress.sort(CardSortOrder.valueOf(Config.get("pref.sortOrder")));
	}

	@Override
	public void refresh() {
		progress.refresh();
	}

	// ==== How to end a session ====

	/**
	 * Beende die Session ohne weitere Dialoge bitte. Je nach Parameter mit oder ohne Save...
	 */
	@Override
	public void closeSilent(boolean save) {
		Log.info(this, "=== CLOSE === Session@" + System.identityHashCode(this) + ", save=" + save);
		presenter.stopClock();
		if (save)
			progress.save();
	}

	/**
	 * Beende die Session, aber gern sauber schön mit Zusammenfassung und so :)
	 *
	 * <p><b>Die Zusammenfassung steht bewusst vor dem Speichern und Melden.</b> Ein Alert öffnet
	 * einen verschachtelten Event-Loop; stünde er dahinter, liefe er auf einer Session, die schon
	 * fertig ist, deren Ansicht aber noch im Fenster hängt. So bleibt die Session bis zur letzten
	 * Zeile lebendig und ist danach unerreichbar, weil der Controller sie ersetzt.</p>
	 */
	@Override
	public void closeLoud() {
		presenter.stopClock();
		Alerts.show("Zusammenfassung", createSummary(), ButtonEnum.OK);
		if (!isFreePlay)     // im freien Spiel wird nichts fortgeschrieben
			progress.save();
		onSessionEnded.run();
	}

	@Override
	public void reactOnPausePressed() {
		progress.reactOnPausePressed();
	}

	@Override
	public void enterPressed() {
		if (!progress.isPaused())
			progress.submitClicked();
	}

	@Override
	public void suspend() {
		presenter.suspendClock();
	}

	@Override
	public void resume() {
		presenter.resumeClock();
	}

	@Override
	public void escClicked() {
		progress.escClicked();
	}

	@Override
	public ScreenView getView() {
		return presenter.getView();
	}

	@Override
	public SessionSwitchStrategy getSwitchStrategy() {
		if (!progress.hasProgressed() || isFreePlay)
			return SessionSwitchStrategy.IMMEDIATE;
		else
			return SessionSwitchStrategy.OFFER_SAVE;
	}

	private String createSummary() {
		SessionProgressCounter counter = progress.createSessionProgress();
		String text = "Du hast " + (counter.correct() + counter.incorrect()) + " von " + counter.total() + " Karten gelernt.";
		text += "\n\nDavon hast Du " + counter.correct() + " richtig und " + counter.incorrect() + " falsch beantwortet.";
		if (!isFreePlay)
			text += "\n\nDer Fortschritt wird nun gespeichert.";
		return text;
	}
}