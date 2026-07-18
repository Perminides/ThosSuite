package app.learn.anki;

import java.util.List;

import app.learn.anki.model.Card;
import app.learn.anki.model.CardSortOrder;
import app.learn.model.Deck;
import app.learn.model.SessionProgressCounter;
import app.shared.Config;
import app.shared.Log;
import app.shared.model.ButtonEnum;
import app.shared.model.SessionSwitchStrategy;
import app.shared.skin.SkinService;
import app.shared.ui.contracts.Screen;
import app.shared.ui.contracts.ScreenView;

/**
 * Schale der Anki-Lernsession. Verantwortlich für genau drei Dinge:
 *  1) Brücke zwischen Controller und dem eigentlichen Karten-Ablauf (AnkiSessionProgress),
 *  2) Ansprechpartner für den Controller (Screen),
 *  3) Lebenszyklus: orchestriert Speicher-Trigger und Zusammenfassungs-Alert.
 *
 * Die Schale hält den Progress und den Presenter; die View kommt kovariant über presenter.getRoot()
 * (ein ScreenFrame) — deshalb kein javafx mehr hier.
 */
public class AnkiDeckSession implements Screen {

	private final SessionProgress progress;
	private final SessionPresenter presenter;
	private final Runnable onSessionEnded;
	private final boolean isFreePlay;

	public AnkiDeckSession(List<Card> cards, Runnable onSessionEnded, AnkiDeckService service, Deck type, boolean isFreePlay) {
		Log.info(this, "=== SESSION CONSTRUCTOR === Session@" + System.identityHashCode(this));
		this.onSessionEnded = onSessionEnded;
		this.isFreePlay = isFreePlay;
		CardSortOrder sortOrder = isFreePlay ? CardSortOrder.RANDOM : CardSortOrder.valueOf(Config.get("pref.sortOrder"));
		this.progress = new SessionProgress(cards, service, type, sortOrder, this::saveChosen);
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
		progress.deactivate();
		if (save)
			progress.save();
	}

	/**
	 * Beende die Session, aber gern sauber schön mit Zusammenfassung und so :)
	 */
	@Override
	public void saveChosen() {
		progress.deactivate();
		SkinService.get().showAlert("Zusammenfassung", createSummary(), ButtonEnum.OK);
		if (isFreePlay)
			progress.end();  // Kein Speichern im freien Spiel, nur Presenter-Cleanup.
		else
			progress.save(); // save() ruft intern presenter.end().
		onSessionEnded.run();
	}

	@Override
	public void reactOnPauseClick() {
		progress.reactOnPauseClick();
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