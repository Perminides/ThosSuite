package app.learn.anki;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import app.learn.anki.model.Card;
import app.learn.anki.model.CardSortOrder;
import app.learn.anki.repository.PlayedCardData;
import app.learn.model.Deck;
import app.learn.model.LearnStat;
import app.learn.model.SessionProgressCounter;
import app.shared.AppClock;
import app.shared.Log;

/**
 * Kapselt den kompletten Karten-Ablauf einer Anki-Lernsession: Iteration über die Karten,
 * Sortierung, Vor/Zurück, Refresh, Aggregation und das Erzeugen der Persistenz-Daten.
 * <br><br>
 * Pendant zu region's WriteSessionProgress. Steht zwischen der Schale (AnkiDeckSession) und dem
 * Presenter. Hält die Zuordnung Karten-Id -> CardProgress selbst (statt sie an die Card zu hängen);
 * mit dem Ende dieser Instanz stirbt die Map - es gibt nichts aus den Karten zu entfernen.
 * <br><br>
 * Der innere CardProgress kapselt den Mehrschritt-Ablauf einer einzelnen Karte. Das ist der
 * strukturelle Unterschied zu region (dort sind Items einschrittig) - jetzt an genau einer Stelle
 * gekapselt statt über die Session verschmiert.
 * <br><br>
 * Hält - wie WriteSessionProgress die RegionSession - eine Rückreferenz auf die Schale, um beim
 * Ende der letzten Karte das Lebenszyklus-Ende (endGracefully) auszulösen.
 */
class SessionProgress {

	private final Runnable onLastCardDone;
	private final AnkiDeckService service;
	private final Deck type;
	private final CardSortOrder sortOrder;

	private SessionPresenter presenter;
	private Map<Integer, CardProgress> cardProgressById;
	private List<Card> cards;
	private int currentIndex = -1;
	private boolean active = true; // !Architektur: Das muss natürlich prozessual ausgeschlossen werden, dass inaktive Sessions wiederbelebt werden und dann kann das hier auch ganz weg.

	public SessionProgress(List<Card> cards, AnkiDeckService service, Deck type, CardSortOrder sortOrder, Runnable onLastCardDone) {
		Log.info(this, "=== PROGRESS CONSTRUCTOR === Progress@" + System.identityHashCode(this));
		this.cards = cards;
		this.service = service;
		this.type = type;
		this.sortOrder = sortOrder;
		this.onLastCardDone = onLastCardDone;
	}

	/**
	 * Wird vom SessionPresenter im Zuge der Konstruktion aufgerufen (presenter == this-presenter).
	 * Erst jetzt sind alle Abhängigkeiten beisammen, daher bauen wir hier die CardProgress-Map auf -
	 * der CardProgress braucht den Presenter. Die Map ist damit fertig, bevor irgendetwas anderes läuft.
	 */
	public void setPresenter(SessionPresenter presenter) {
		this.presenter = presenter;
		cardProgressById = new HashMap<>();
		for (Card card : cards)
			cardProgressById.put(card.getId(), new CardProgress(card, presenter, this));
	}

	public void start() {
		Log.info(this, "=== PROGRESS START === Progress@" + System.identityHashCode(this));
		if (!active)
			throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		// Neue Karten immer zuerst
		List<Card> newCards = new ArrayList<>();
		Iterator<Card> iter = cards.listIterator();
		while (iter.hasNext()) {
			Card card = iter.next();
			if (card.isNew()) {
				iter.remove();
				newCards.add(card);
			}
		}
		CardSortService.getSorter(sortOrder).accept(cards);
		cards = Stream.concat(newCards.stream(), cards.stream()).collect(Collectors.toCollection(ArrayList::new));
		currentIndex = 0;
		presenter.sessionProgressChanged(createSessionProgress());
		presenter.newCardIncoming(cards.get(currentIndex).getLearnStat());
		Log.info(this, "Starte AnkiSession " + type.getDisplayName());
		getCurrentProgress().start();
	}

	public void sort(CardSortOrder order) {
		if (!active)
			throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		int index = currentIndex + 1;
		while (index < cards.size() && cards.get(index).isNew()) {
			index++;
		}
		List<Card> fixed = new ArrayList<Card>(cards.subList(0, index));
		List<Card> toSort = new ArrayList<Card>(cards.subList(index, cards.size()));
		CardSortService.getSorter(order).accept(toSort);
		fixed.addAll(toSort);
		cards = fixed;
	}

	public void cardFinished(boolean correct) {
		if (!active)
			throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		currentIndex++;
		if (currentIndex < cards.size()) {
			presenter.cardFinished(correct);
			presenter.sessionProgressChanged(createSessionProgress());
			presenter.newCardIncoming(cards.get(currentIndex).getLearnStat());
			getCurrentProgress().start();
		} else {
			onLastCardDone.run();
		}
	}

	// ========================================
	// User interaction (from presenter)
	// ========================================

	public void textInputChanged(String text) {
		if (!active)
			throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		getCurrentProgress().checkTextInput(text);
	}

	public void elementClicked(String id) {
		if (!active)
			throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		getCurrentProgress().elementClicked(id);
	}

	public void mcClicked(int index) {
		if (!active)
			throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		getCurrentProgress().mcClicked(index);
	}

	public void reactOnPauseClick() {
		if (!active)
			throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		getCurrentProgress().endPause();
	}

	public void escClicked() {
		if (!active)
			throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		getCurrentProgress().cancel();
	}

	public boolean isPaused() {
		if (!active)
			throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		return getCurrentProgress().isPaused();
	}

	public void goBack() {
		if (!active)
			throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		if (currentIndex > 0) {
			presenter.cardFinished(null);
			Card cur = cards.get(currentIndex);
			cardProgressById.put(cur.getId(), new CardProgress(cur, presenter, this));
			currentIndex--;
			Card prev = cards.get(currentIndex);
			cardProgressById.put(prev.getId(), new CardProgress(prev, presenter, this));
			presenter.sessionProgressChanged(createSessionProgress());
			presenter.newCardIncoming(cards.get(currentIndex).getLearnStat());
			getCurrentProgress().start();
		}
	}

	/**
	 * Session-Logik für einen SkinChange.
	 * Für den Moment: Reset der aktuellen Karte. Keep it simple für diesen Edge-Case!
	 * Logik nötig, da SkinChange nicht nur für Bilder beschneiden problematisch ist, sondern ja auch
	 * Komponenten anders platzieren könnte und deren Größe ändern. Also um einen Neuaufbau kommt man
	 * bei meinen mächtigen Skins nicht herum...
	 */
	public void refresh() {
		Log.info(this, "=== REFRESH === Progress@" + System.identityHashCode(this) + ", currentIndex=" + currentIndex);
		if (!active)
			throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		presenter.cardFinished(null);
		presenter.refresh();
		Card cur = cards.get(currentIndex);
		cardProgressById.put(cur.getId(), new CardProgress(cur, presenter, this));
		presenter.sessionProgressChanged(createSessionProgress());
		presenter.newCardIncoming(cards.get(currentIndex).getLearnStat());
		getCurrentProgress().start();
	}

	// ========================================
	// Lifecycle (from shell)
	// ========================================

	/**
	 * Deaktiviert den Progress. Idempotenz-Schutz: ein zweiter Aufruf fliegt - so bleibt der
	 * Doppel-Close-Schutz der alten Session erhalten.
	 */
	public void deactivate() {
		if (!active)
			throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		active = false;
		Log.info(this, "=== DEACTIVATE === Progress@" + System.identityHashCode(this));
	}

	/**
	 * Nur Presenter-Cleanup ohne Speichern (FreePlay). Im Nicht-FreePlay-Fall macht save() das mit.
	 */
	public void end() {
		presenter.end();
	}

	public boolean hasProgressed() {
		return currentIndex > 0;
	}

	/**
	 * Aktualisiert die LearnStats (in-place auf der echten Instanz, damit dueCards weiter greift) und
	 * baut daraus die Persistenz-Daten. Nur gespielte Karten kommen rein - die Filterung passiert hier
	 * an der Quelle, die Persistenz muss sie nicht mehr kennen.
	 */
	public void save() {
		Log.info(this, "=== SAVE START === Progress@" + System.identityHashCode(this)
				+ ", cards.size=" + cards.size() + ", currentIndex=" + currentIndex);
		List<PlayedCardData> rows = new ArrayList<>();
		for (Card card : cards) {
			CardProgress cp = cardProgressById.get(card.getId());

			// Karte wurde nicht gespielt
			if (cp.isCorrectlyAnswered() == null)
				continue;

			LearnStat learnStat = card.getLearnStat();
			if (learnStat == null) {
				// Es ist eine neue Karte
				card.setLearnStat(new LearnStat(AppClock.TODAY, AppClock.TODAY,
						cp.isCorrectlyAnswered() ? 1 : 0, cp.isCorrectlyAnswered() ? 0 : 1));
			} else {
				if (!learnStat.isDueToday())
					throw new RuntimeException("Sicherheitsnetz eingebaut. Diese Karte war gar nicht dran. Und ich soll den Fortschritt überschreiben? Mache ich ungern!");
				learnStat.setLevel(cp.calculateNewLevel(learnStat.getLastPlayed(), cp.isCorrectlyAnswered(), true));
				learnStat.setLastPlayed(AppClock.TODAY);
				if (!cp.isCorrectlyAnswered())
					learnStat.incrementWrongCount();
			}

			LearnStat updated = card.getLearnStat();
			rows.add(new PlayedCardData(card.getId(), updated.getCurrentLevel(), updated.getWrongCount(),
					cp.isCorrectlyAnswered(), cp.getPlayedTimestamp()));
		}
		service.savePlayedCards(type, rows);
		Log.info(this, "=== SAVE END === " + rows.size() + " Karten gespeichert");
		presenter.end();
	}

	// ========================================
	// Internal
	// ========================================

	private CardProgress getCurrentProgress() {
		return cardProgressById.get(cards.get(currentIndex).getId());
	}

	// On-the-fly berechnen
	public SessionProgressCounter createSessionProgress() {
		int correct = 0;
		int incorrect = 0;
		for (Card card : cards) {
			Boolean answered = cardProgressById.get(card.getId()).isCorrectlyAnswered();
			if (answered != null)
				if (answered)
					correct++;
				else
					incorrect++;
		}
		return new SessionProgressCounter(correct, incorrect, cards.size());
	}
}