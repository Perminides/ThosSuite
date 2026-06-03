package app.learn.anki;

import java.util.List;

import app.learn.anki.model.Card;
import app.learn.model.Deck;
import app.learn.model.SessionProgressCounter;
import app.shared.Log;
import app.shared.Screen;
import app.shared.model.CardSortOrder;
import app.shared.model.SessionSwitchStrategy;
import app.shared.skin.SkinService;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;

/**
 * Schale der Anki-Lernsession. Verantwortlich für genau drei Dinge:
 *  1) Brücke zwischen Controller und dem eigentlichen Karten-Ablauf (AnkiSessionProgress),
 *  2) Ansprechpartner für den Controller (Screen),
 *  3) Lebenszyklus: orchestriert Speicher-Trigger und Zusammenfassungs-Alert.
 *
 * Den gesamten Karten-Ablauf (Iteration, Fortschritt, Persistenz-Daten) kapselt der AnkiSessionProgress.
 * Die Schale hält nur diesen einen Progress; der Presenter hängt eine Ebene tiefer am Progress, daher
 * geht getView() über progress.getView() (ein Hop, dafür ein Feld weniger in der Schale).
 */
public class AnkiDeckSession implements Screen {

	private final SessionProgress progress;
	private final Runnable onSessionEnded;
	private final boolean isFreePlay;

	public AnkiDeckSession(List<Card> cards, Runnable onSessionEnded, AnkiDeckService service, Deck type, CardSortOrder sortOrder, boolean isFreePlay) {
		Log.info(this, "=== SESSION CONSTRUCTOR === Session@" + System.identityHashCode(this));
		this.onSessionEnded = onSessionEnded;
		this.isFreePlay = isFreePlay;
		this.progress = new SessionProgress(cards, service, type, sortOrder, this);
		new SessionPresenter(type, progress); // registriert sich selbst am Progress via setPresenter(this)
	}

	public void start() {
		Log.info(this, "=== SESSION START === Session@" + System.identityHashCode(this));
		progress.start();
	}

	public void sort(CardSortOrder order) {
		progress.sort(order);
	}

	public void refresh() {
		progress.refresh();
	}

	// ==== How to end a session ====

	@Override
	/**
	 * Beende die Session ohne weitere Dialoge bitte. Je nach Parameter mit oder ohne Save...
	 */
	public void closeSilent(boolean save) {
		Log.info(this, "=== CLOSE === Session@" + System.identityHashCode(this) + ", save=" + save);
		progress.deactivate();
		if (save)
			progress.save();
	}

	@Override
	/**
	 * Beende die Session, aber gern sauber schön mit Zusammenfassung und so :)
	 */
	public void endGracefully() {
		Pane currentPane = progress.getView(); // Muss vor dem Deaktivieren passieren (Window holen).
		progress.deactivate();
		Alert alert = SkinService.get().createAlert(currentPane.getScene().getWindow(), "Zusammenfassung", createSummary(), false, false);
		alert.showAndWait();
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

	public Pane getView() {
		return progress.getView();
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