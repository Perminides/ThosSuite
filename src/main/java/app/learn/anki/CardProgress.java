package app.learn.anki;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import app.learn.anki.model.Card;
import app.learn.anki.model.FastAnswers;
import app.learn.anki.model.MultipleChoiceAnswers;
import app.learn.anki.model.Card.AnswerOption;
import app.learn.anki.model.Card.ChoiceStep;
import app.learn.anki.model.Card.ClickMapElements;
import app.learn.anki.model.Card.Collect;
import app.learn.anki.model.Card.Fast;
import app.learn.anki.model.Card.Image;
import app.learn.anki.model.Card.Input;
import app.learn.anki.model.Card.MCPlus;
import app.learn.anki.model.Card.Role;
import app.learn.anki.model.Card.MarkMapElements;
import app.learn.anki.model.Card.Output;
import app.learn.anki.model.Card.Pause;
import app.learn.anki.model.Card.SketchImage;
import app.learn.anki.model.Card.SketchImageAdd;
import app.learn.anki.model.Card.SketchImageFill;
import app.learn.anki.model.Card.SketchImageMark;
import app.learn.anki.model.Card.Step;
import app.shared.Log;
import app.shared.model.CollectChip;


/**
 * Existiert nur innerhalb einer (Lern-)Session. Wird vom AnkiSessionProgress erstellt und in dessen
 * Map gehalten (Karten-Id -> CardProgress). Mit dem Ende des AnkiSessionProgress stirbt die Map - es
 * gibt nichts mehr aus der Card zu entfernen. Dass sie den Presenter kennt, ist nicht ganz sauber.
 * Aber wir haben uns der Einfachheit darauf verständigt, weil Events würden das nur aufblähen und das
 * Konstrukt Panel - Presenter - SessionProgress - Progress ist halt auch ein enges...
 *
 * <p>Der Ablauf einer Karte ist eine kleine Zustandsmaschine mit drei {@link Phase|n}. Alle
 * eingehenden Ereignisse laufen über {@link #onEvent(Ev)} — die einzige Stelle, die entscheidet,
 * was ein Ereignis in der aktuellen Phase bedeutet. PLAYING deckt dabei beide Step-Listen ab
 * (Karte und Abspann); welche gerade läuft, steckt in {@code steps}.
 */
public class CardProgress {

	/** Zuschlag auf die erste Antwort eines Fast-Steps — Zeit, die Frage überhaupt zu lesen. */
	private static final int READ_BONUS_SECONDS = 3;

	private final SessionPresenter presenter;
	private final Card card;
	/** Die laufende Liste — nach einem Fehler der Abspann statt der Karte. */
	private List<Card.Step> steps;
	private final SessionProgress sessionProgress;

	private Phase phase = Phase.PLAYING;
	private Boolean correctlyAnswered = null; // null = Noch nicht gespielt.
	private LocalDateTime playedTimestamp = null;

	private int currentIndex = -1;
	private Set<String> clickedIds = new HashSet<>();
	private Set<Integer> clickedMcAnswers = new HashSet<>();
	private Set<Integer> foundInputAnswers = new HashSet<>();
	private final List<String> collected = new ArrayList<>();

	private MultipleChoiceAnswers activeSessionMC = null;
	private FastAnswers activeFast = null;

	private static final int MAX_MC = 8;
	/** Je Optionsuniversum: die Pflicht-Antworten aus allen Steps und die feste Reihenfolge, falls eine. */
	private Map<Set<String>, RequiredAnswers> requiredPerCluster;
	/** Je Universum die einmal eingefrorene Darstellung dieses Durchlaufs. */
	private final Map<Set<String>, List<String>> frozenDisplay = new HashMap<>();

	private static final class RequiredAnswers {
		final Set<String> fromAllSteps = new HashSet<>();
		final List<String> order = new ArrayList<>();
	}

	public CardProgress(Card hint, SessionPresenter presenter, SessionProgress sessionProgress) {
		this.card = hint;
		this.presenter = presenter;
		this.sessionProgress = sessionProgress;
		this.steps = hint.getSteps();
	}

	public void start() {
		Log.info(this, "Los geht es mit Karte: " + card.getId());
	    planMcClusters();
	    currentIndex = 0;
	    runSteps();
	}

	// ========================================
	// Eingänge (von SessionProgress) — dünn, leiten an onEvent weiter
	// ========================================

	public void checkTextInput(String text) {
		onEvent(new Ev.Typed(text));
	}

	public void elementClicked(String id) {
		onEvent(new Ev.MapClicked(id));
	}

	public void mcClicked(int index) {
		onEvent(new Ev.McClicked(index));
	}

	public void submitted() {
		onEvent(new Ev.Submit());
	}

	public void collectAdded(String word) {
		onEvent(new Ev.CollectAdded(word));
	}

	public void collectRemoved(String word) {
		onEvent(new Ev.CollectRemoved(word));
	}

	/** Die Fast-Uhr ist abgelaufen. */
	public void timeUp() {
		onEvent(new Ev.ClockExpired());
	}

	/** Die Pausetaste. */
	public void pausePressed() {
		onEvent(new Ev.PauseKey());
	}

	/** Die ESC-Taste */
	public void escPressed() {
		onEvent(new Ev.Esc());
	}
	
	/** Die Entertaste */
	public void enterPressed() {
		onEvent(new Ev.EnterPressed());
	}

	// ========================================
	// Dispatch — die eine Stelle für Zustand x Ereignis
	// ========================================

	/**
	 * Die drei Phasen einer Karte.
	 *
	 * <ul>
	 * <li>{@code PLAYING} — Steps werden abgearbeitet, Eingaben gehen an den aktuellen Step.</li>
	 * <li>{@code PAUSED} — ein {@code Pause:}-Step hält die Karte an. Ein quittierendes Ereignis
	 *     geht weiter hinter den Pause-Step.</li>
	 * <li>{@code REVEALED_WAITING} — die Karte ist falsch, die Aufdeckung steht, es gibt keinen
	 *     Abspann. Ein quittierendes Ereignis beendet die Karte.</li>
	 * </ul>
	 */
	private enum Phase {
		PLAYING, PAUSED, REVEALED_WAITING
	}

	/** Die Ereignisse, mit denen der Nutzer an der Karte arbeitet. */
	private sealed interface Ev
			permits Ev.Typed, Ev.MapClicked, Ev.McClicked, Ev.Submit,
					Ev.CollectAdded, Ev.CollectRemoved, Ev.ClockExpired, Ev.PauseKey, Ev.Esc, Ev.EnterPressed {

		record Typed(String text) implements Ev {}
		record MapClicked(String id) implements Ev {}
		record McClicked(int index) implements Ev {}
		record Submit() implements Ev {}
		record CollectAdded(String word) implements Ev {}
		record CollectRemoved(String word) implements Ev {}
		record ClockExpired() implements Ev {}
		record PauseKey() implements Ev {}
		record Esc() implements Ev {}
		record EnterPressed() implements Ev {}
	}

	/**
	 * Der Schalter der Zustandsmaschine. In {@code PLAYING} geht alles an den Step; in den
	 * Warte-Pausen quittieren nur Klick-ähnliche Ereignisse (siehe {@link #isAck(Ev)}), alles
	 * andere läuft ins Leere.
	 */
	private void onEvent(Ev e) {
		switch (phase) {
			case PLAYING           -> whilePlaying(e);
			case PAUSED            -> { if (isAck(e)) resumeFromPause(); }
			case REVEALED_WAITING  -> { if (isAck(e)) cardFinished(); }
		}
	}

	/**
	 * Quittiert eine laufende Warte-Pause: Pausetaste, ESC, Klick auf Karte oder MC und der
	 * Absende-Knopf. Getipptes, Sammeln und die abgelaufene Uhr tun das nicht.
	 */
	private static boolean isAck(Ev e) {
		return e instanceof Ev.PauseKey
			|| e instanceof Ev.Esc
			|| e instanceof Ev.MapClicked
			|| e instanceof Ev.McClicked
			|| e instanceof Ev.Submit
			|| e instanceof Ev.EnterPressed;
	}

	/** Die Ereignis-Verteilung innerhalb von PLAYING — zuerst das Ereignis, dann der Step-Typ. */
	private void whilePlaying(Ev e) {
		switch (e) {
			case Ev.Typed t          -> typed(t.text());
			case Ev.MapClicked c     -> mapClicked(c.id());
			case Ev.McClicked c      -> choiceClicked(c.index());
			case Ev.Submit _         -> submitPressed();
			case Ev.EnterPressed _   -> submitPressed();
			case Ev.CollectAdded c   -> collectAddedTo(c.word());
			case Ev.CollectRemoved c -> collectRemovedFrom(c.word());
			case Ev.ClockExpired _   -> clockExpired();
			case Ev.PauseKey _       -> pauseKeyInPlaying();
			case Ev.Esc _            -> escInPlaying();
		}
	}

	// ========================================
	// Input
	// ========================================

	private void typed(String text) {
	    Step step = steps.get(currentIndex);

	    if (step instanceof Fast) {
	        fastTyped(text);
	        return;
	    }

	    if (!(step instanceof Input input)) {
	        return; // Ignorieren
	    }

	    String typed = text.trim().toLowerCase();
	    for (String stopWord : input.stopWords())
	        if (stopWord.toLowerCase().equals(typed)) {
	            revealInput(input);
	            return;
	        }

	    for (int i = 0; i < input.answers().size(); i++) {
	        if (foundInputAnswers.contains(i))
	            continue; // schon gefunden: doppelt Getipptes läuft ins Leere
	        for (String variant : input.answers().get(i))
	            if (variant.toLowerCase().equals(typed)) {
	                foundInputAnswers.add(i);
	                presenter.textIsCorrect();
	                if (foundInputAnswers.size() == input.answers().size()) {
	                    currentIndex++;
	                    runSteps();
	                }
	                return;
	            }
	    }

	    if (ausDerListe(input, typed))
	        revealInput(input);
	    // Sonst: falsche Eingabe, nichts tun
	}

	/**
	 * Ob das Getippte ein Wort der Liste ist, das hier als Stoppwort zählt — die Liste hängt nicht am
	 * Step, sie wird erst jetzt gefragt.
	 *
	 * <p>Nicht als Stoppwort zählen das Tolerierte und ein Wort, das der Anfang einer richtigen
	 * Schreibweise ist: Sonst stürbe „Porto-Novo" schon bei „Porto".</p>
	 */
	private boolean ausDerListe(Input input, String typed) {
		if (input.listName() == null)
			return false;
		for (String wort : input.tolerated())
			if (wort.equalsIgnoreCase(typed))
				return false;
		for (List<String> answer : input.answers())
			for (String variant : answer)
				if (variant.toLowerCase().startsWith(typed))
					return false;
		return sessionProgress.wordLists().contains(input.listName(), typed);
	}

	/** Lösung ins Feld, Karte falsch, Pause — für ESC und für ein getipptes Stoppwort. */
	private void revealInput(Input input) {
		presenter.setCorrectText(input.solutionText());
		failed();
	}

	// ========================================
	// Fast
	// ========================================

	/**
	 * Eine erkannte Antwort rastet in ihr Feld ein und zieht die Uhr wieder voll auf. Ein Stoppwort
	 * oder eine bekannte Antwort an falscher Position beendet die Karte; sonstige Eingaben laufen ins Leere.
	 */
	private void fastTyped(String text) {
		switch (activeFast.check(text)) {
			case FastAnswers.Hit hit -> {
				presenter.fastAnswerCorrect(hit.slot(), hit.text(), activeFast.expectedSlot());
				if (activeFast.isComplete()) {
					presenter.stopClock();
					currentIndex++;
					runSteps();
				} else {
					presenter.restartClock();
				}
			}
			case FastAnswers.StopWord _ -> clockExpired();
			case FastAnswers.WrongPosition _ -> clockExpired();
			case FastAnswers.NoMatch _ -> {}
		}
	}

	/** Rest aufdecken, Karte ist falsch, und es geht in die Pause. Für die Uhr und für Fast-Fehler. */
	private void clockExpired() {
		presenter.fastFailed(activeFast.revealRest());
		failed();
	}

	// ========================================
	// Map
	// ========================================

	private void mapClicked(String id) {
	    Step step = steps.get(currentIndex);

	    // Klicks außerhalb eines Klick-Steps laufen ins Leere.
	    if (!(step instanceof ClickMapElements input)) {
	        return;
	    }

	    clickedIds.add(id);

	    boolean correct = input.mandatory().contains(id)
	               || input.optional().contains(id);

	    if (!correct) {
	        // ----- FALSCH -----
	        presenter.mapClickChecked(id, correct, input.mandatory());
	        failed();
	    } else {
	        presenter.mapClickChecked(id, true, null);
	        if (clickedIds.containsAll(input.mandatory())) {
	            // ----- VOLLSTÄNDIG -----
	            currentIndex++;
	            runSteps();
	        }
	        // ----- NOCH NICHT VOLLSTÄNDIG ----- : auf die restlichen Pflicht-Klicks warten
	    }
	}

	// ========================================
	// Multiple Choice
	// ========================================

	private void choiceClicked(int index) {
	    Step step = steps.get(currentIndex);
	    // Klicks außerhalb eines MC-Steps laufen ins Leere.
	    if (!(step instanceof ChoiceStep)) {
	        return;
	    }

	    // Prüfen gegen das Session-Objekt (das max 8 Antworten hat), nicht gegen das Original im Step!
	    if (activeSessionMC == null || activeSessionMC.getAnswerOptions().size() <= index)
	    	return;

	    if (step instanceof MCPlus) {
	    	// Eine tolerierte Antwort haftet nicht. Sie bleibt aus der Auswahl heraus — sonst koennte
	    	// man sie STATT der richtigen abschicken, und genau das soll sie nicht sein: nicht
	    	// bestraft, aber auch nicht die gesuchte Antwort. Damit verhaelt sich der Sammelmodus
	    	// hier wie der Einzelklick weiter unten.
	    	if (activeSessionMC.roleAt(index).tolerated()) {
	    		presenter.mcClickRejected(index);
	    		return;
	    	}
	    	// Sonst wird nur markiert — geprüft wird erst beim Absenden.
	    	boolean wasMarked = clickedMcAnswers.contains(index);
	    	if (wasMarked)
	    		clickedMcAnswers.remove(index);
	    	else
	    		clickedMcAnswers.add(index);
	    	presenter.mcMarked(index, !wasMarked);
	    	presenter.canSubmit(!clickedMcAnswers.isEmpty());
	    	return;
	    }

	    // Einzelklick (MC), sofort gewertet.
	    Role role = activeSessionMC.roleAt(index);

	    if (role.tolerated()) {
	    	// Falsch, aber kein Abbruch: der Knopf schuettelt kurz den Kopf, dann weiter warten.
	    	presenter.mcClickRejected(index);
	    	return;
	    }

	    // User klickt einfach mehrfach auf den gleichen Button, wenn mehr als eine Antwort gesucht wird...
	    if (clickedMcAnswers.contains(index))
	    	return;

	    if (role == Role.CORRECT) {
	        clickedMcAnswers.add(index);
	        presenter.mcClickChecked(index, true);
	        if (activeSessionMC.isFinallyCorrect(clickedMcAnswers)) {
	            // ----- VOLLSTÄNDIG -----
	            currentIndex++;
	            runSteps();
	        }
	        // ----- NOCH NICHT VOLLSTÄNDIG ----- : auf die restlichen Pflicht-Klicks warten
	    } else {
	        // ----- FALSCH ----- (fest sichtbar oder Füller)
	        presenter.mcClickChecked(index, false);
	    	// Lösung für die aktuell angezeigten Optionen anzeigen
	        presenter.setCorrectMc(activeSessionMC.getCorrectIndexes());
	        failed();
	    }
	}

	/**
	 * Absenden bei MC+ oder Collect. Bei MC+ wird die markierte Auswahl als Ganzes geprüft. Falsch
	 * ist sie schon dann, wenn eine richtige Antwort fehlt — aufgedeckt werden die falsch gewählten
	 * rot und alle richtigen grün.
	 */
	private void submitPressed() {
		presenter.canSubmit(false);
		if (steps.get(currentIndex) instanceof Collect collect) {
			collectSubmitted(collect);
			return;
		}
		if (activeSessionMC == null || !(steps.get(currentIndex) instanceof MCPlus))
			return;
		if (clickedMcAnswers.isEmpty())
			return;

		if (activeSessionMC.isFinallyCorrect(clickedMcAnswers)) {
			// ----- RICHTIG -----
			currentIndex++;
			runSteps();
		} else {
			// ----- FALSCH -----
			Set<Integer> correctIndexes = activeSessionMC.getCorrectIndexes();
			for (Integer clicked : clickedMcAnswers)
				if (!correctIndexes.contains(clicked))
					presenter.mcClickChecked(clicked, false);
			presenter.setCorrectMc(correctIndexes);
			failed();
		}
	}

	// ========================================
	// Collect
	// ========================================

	private void collectAddedTo(String word) {
		if (!(steps.get(currentIndex) instanceof Collect))
			return;
		if (!collected.contains(word))
			collected.add(word);
		showCollected();
	}

	private void collectRemovedFrom(String word) {
		if (!(steps.get(currentIndex) instanceof Collect))
			return;
		collected.remove(word);
		showCollected();
	}

	private void showCollected() {
		List<CollectChip> chips = new ArrayList<>();
		for (String word : collected)
			chips.add(new CollectChip(word, CollectChip.State.CHOSEN));
		presenter.showCollected(chips);
		presenter.canSubmit(!collected.isEmpty());
	}

	/**
	 * Wie MC+: erst das Absenden wertet, ganz oder gar nicht. Aufgedeckt wird in der gewählten
	 * Reihenfolge, die vergessenen hinten dran — falsch gewählt und vergessen sind beide rot, der
	 * Zustand unterscheidet sie. Ein toleriertes Wort bleibt ohne Farbe; fehlt es, taucht es nicht auf.
	 */
	private void collectSubmitted(Collect collect) {
		if (collected.isEmpty())
			return;

		boolean[] found = new boolean[collect.answers().size()];
		boolean allRight = true;
		List<CollectChip> result = new ArrayList<>();
		for (String word : collected) {
			int answer = answerIndexOf(collect, word);
			if (answer >= 0) {
				found[answer] = true;
				result.add(new CollectChip(word, CollectChip.State.CORRECT));
			} else if (isTolerated(collect, word)) {
				result.add(new CollectChip(word, CollectChip.State.TOLERATED));
			} else {
				allRight = false;
				result.add(new CollectChip(word, CollectChip.State.WRONG));
			}
		}
		for (int i = 0; i < found.length; i++)
			if (!found[i]) {
				allRight = false;
				result.add(new CollectChip(collect.answers().get(i).get(0), CollectChip.State.MISSING));
			}

		presenter.revealCollect(result);
		if (allRight) {
			currentIndex++;
			runSteps();
		} else {
			failed();
		}
	}

	private static boolean isTolerated(Collect collect, String word) {
		for (String variant : collect.tolerated())
			if (variant.equalsIgnoreCase(word))
				return true;
		return false;
	}

	/** Index der Antwort, zu der das Wort eine Schreibweise ist, sonst -1. */
	private static int answerIndexOf(Collect collect, String word) {
		for (int i = 0; i < collect.answers().size(); i++)
			for (String variant : collect.answers().get(i))
				if (variant.equalsIgnoreCase(word))
					return i;
		return -1;
	}

	// ========================================
	// Pausen und Fehler
	// ========================================

	/**
	 * Die Pausetaste in PLAYING: läuft gerade ein Fast-Step, friert sie nur die Uhr ein beziehungsweise
	 * taut sie wieder auf — die Texterkennung läuft dabei weiter, damit sich ein Vertipper in Ruhe
	 * korrigieren lässt. Auf allen anderen Steps ohne Wirkung.
	 */
	private void pauseKeyInPlaying() {
		if (currentIndex < steps.size() && steps.get(currentIndex) instanceof Fast)
			presenter.toggleClock();
	}

	/** ESC in PLAYING beendet die Karte nur, wenn auf getippte Antworten gewartet wird. */
	private void escInPlaying() {
		if (steps.get(currentIndex) instanceof Input input)
			revealInput(input);
	}

	/** Die eine Pause — für den {@code Pause:}-Step wie im Abspann. Ein quittierendes Ereignis beendet sie. */
	private void pause() {
		phase = Phase.PAUSED;
		presenter.pause();
		inviteAck();
	}

	/** Ein quittierendes Ereignis während {@code PAUSED}: weiter hinter dem Pause-Step. */
	private void resumeFromPause() {
		phase = Phase.PLAYING;
		presenter.canSubmit(false);
		currentIndex++;
		runSteps();
	}

	/**
	 * Die Karte ist falsch, die Aufdeckung steht schon da. Trägt die Karte einen Abspann, läuft er
	 * sofort mit — beides ist gleichzeitig zu sehen, bis die Pause im Abspann quittiert wird. Ohne
	 * Abspann hält {@code REVEALED_WAITING} die Aufdeckung fest, bis ein quittierendes Ereignis die
	 * Karte beendet.
	 *
	 * <p>Was der Abspann anzeigt, kann die Aufdeckung überdecken: ein {@code Image:} ersetzt etwa
	 * eine Skizze im selben Bildrahmen.</p>
	 */
	private void failed() {
		playedTimestamp = LocalDateTime.now();
		correctlyAnswered = false;
		if (card.getOnFailSteps().isEmpty()) {
			phase = Phase.REVEALED_WAITING;
			presenter.pause();
			inviteAck();
			return;
		}
		steps = card.getOnFailSteps();
		currentIndex = 0;
		runSteps();
	}

	/**
	 * Der Absende-Knopf lädt zum Quittieren ein: In einer Warte-Pause schaltet ein Klick wie
	 * Enter oder ESC weiter. Deaktiviert wird er beim Verlassen der Pause und am Kartenende.
	 */
	private void inviteAck() {
		presenter.canSubmit(true);
	}

	// ========================================
	// Ablauf
	// ========================================

	private void runSteps() {
	    if (currentIndex >= steps.size()) {
	    	// Wenn es nicht auf false steht, habe ich noch keinen Fehler gemacht, dann setze es auf true, wenn alle Steps durch.
	    	if (correctlyAnswered == null) {
	    		playedTimestamp = LocalDateTime.now();
	    		correctlyAnswered = true;
	    	}
	    	cardFinished();
	    } else {
	        Step step = steps.get(currentIndex);
	        process(step);

	        if (!requiresUserInput(step)) {
	            currentIndex++;
	            runSteps(); // rekursiv weitermachen
	        }
	    }
	}

	private void process(Step step) {
		Log.debug(getClass(), "Step: " + step.getClass());
		/**
		 * Neues switch mit Pattern Matching ab Java 21
		 */
		switch (step) {
			case Output output -> presenter.showQuestion(output.text());
			case ClickMapElements x -> {
			    clickedIds.clear(); // sonst zählen die Klicks des vorigen Steps weiter mit
			    Set<String> allShapes = new HashSet<>(x.mandatory());
			    allShapes.addAll(x.optional());
			    presenter.waitForClick(allShapes);
			}
			case Image image -> presenter.showImage(image.file());
			case SketchImage sketch -> presenter.showSketch(sketch.structure());
			case SketchImageAdd add -> presenter.addSketch(add.structure(), add.cell(), add.size(),
					add.offsetX(), add.offsetY());
			case SketchImageMark mark -> presenter.markSketchAreas(mark.areas());
			case SketchImageFill fill -> presenter.fillSketchAreas(fill.areas(), fill.color());
			case Input _ -> {
			    foundInputAnswers.clear(); // sonst zählen die Treffer des vorigen Inputs weiter mit
			    presenter.waitForText();
			}
			case Collect collect -> {
			    collected.clear(); // sonst stünden die Wörter des vorigen Collect-Steps noch da
			    presenter.waitForCollect(collect.suggestions());
			}
			case Pause _ -> pause();
			case ChoiceStep cs -> {
				Set<String> universe = universeOf(cs.options());
				Set<String> required = requiredPerCluster.get(universe).fromAllSteps;
				List<String> display = required.size() <= MAX_MC
						? frozenDisplay.computeIfAbsent(universe, u -> buildDisplay(u, required))
						: buildDisplay(universe, pinnedOf(cs.options()));

				Map<String, Role> roles = rolesOf(cs.options());
				List<AnswerOption> shown = new ArrayList<>();
				for (String text : display)
					shown.add(new AnswerOption(text, roles.get(text)));

				activeSessionMC = new MultipleChoiceAnswers(shown);
				clickedMcAnswers.clear(); // sonst zählt die Auswahl des vorigen Steps weiter mit
				presenter.showMultipleChoice(display);
			}

			case MarkMapElements mark -> presenter.markMapElements(mark.shapes());

			case Fast fast -> {
				activeFast = new FastAnswers(fast);
				presenter.showFastStep(activeFast.slotHints(), activeFast.expectedSlot(),
						fast.seconds() + READ_BONUS_SECONDS, fast.seconds());
			}
		}
	}

	private boolean requiresUserInput(Step step) {
	    return step instanceof Card.Input
	        || step instanceof Card.Collect
	        || step instanceof Card.ChoiceStep
	        || step instanceof Card.ClickMapElements
	        || step instanceof Card.Pause
	        || step instanceof Card.Fast;
	}

	// ========================================
	// Lifecycle
	// ========================================

	public LocalDateTime getPlayedTimestamp() {
		return playedTimestamp;
	}

	public Boolean isCorrectlyAnswered() {
		return correctlyAnswered;
	}

	/**
	 * Ich kann die LearnStats hier noch nicht setzen, weil vielleicht gibt es ja noch ein UNDO.
	 * correctlyAnswered must be set before calling!
	 *
	 */
	private void cardFinished() {
		if (correctlyAnswered == null)
			throw new RuntimeException("Wieso wurde correctlyAnswered nicht gesetzt vor dem Aufruf von cardFinished?");
		presenter.canSubmit(false); // Karte vorbei — nichts mehr abzusenden
        sessionProgress.cardFinished(correctlyAnswered);
	}

	// ========================================
	// MC-Planung (unverändert)
	// ========================================

	/**
	 * Deterministischer Scan über alle Steps: MC/MC+ nach ihrem Optionsuniversum (Textmenge) gruppieren,
	 * je Cluster die Pflicht-Menge (alle Texte, die irgendwo {@code +} oder {@code -} sind) und die feste
	 * Reihenfolge sammeln. Reine Herleitung fürs Anzeigen — nicht der Parser, nicht der Generator.
	 *
	 * <p><b>Wozu:</b> Fragt eine Karte dasselbe Vokabular mehrfach — zehn Orte, acht Farben —, sollen die
	 * Antworten an ihrem Platz stehen bleiben, statt bei jedem Step neu zu springen. Gleichzeitig darf
	 * nicht durch das <i>Auftauchen</i> einer Option verraten werden, dass sie diesmal die richtige ist.
	 * Deshalb wird einmal je Cluster und Durchlauf eingefroren: Pflicht pinnen, aus dem Pool auffüllen,
	 * ordnen. Die Rolle zieht jeder Step danach frisch aus seinen eigenen Optionen — Präsenz und
	 * Verhalten sind entkoppelt.</p>
	 *
	 * <p><b>Was der Autor dafür tun muss:</b> gleichartige Steps mit demselben Optionsuniversum
	 * schreiben; nur die Präfixe wandern. Vergisst man es, springen die Antworten sichtbar. Das ist ein
	 * Netz, kein Zwang — erzwungen wird nur die Pflicht-Menge eines einzelnen Steps ≤ {@value #MAX_MC}.</p>
	 *
	 * <p>Ist die Pflicht-Menge eines Clusters größer als {@value #MAX_MC} — zwölf Zeichner, jeder einmal
	 * richtig —, passt sie nicht auf feste Plätze. Dann zieht jeder Step seine Anzeige selbst, und die
	 * Antworten springen.</p>
	 */
	private void planMcClusters() {
		requiredPerCluster = new HashMap<>();
		for (Step step : steps) {
			if (!(step instanceof ChoiceStep cs))
				continue;
			Set<String> pinned = pinnedOf(cs.options());
			if (pinned.size() > MAX_MC)
				throw new RuntimeException("MC: Pflicht-Menge größer als " + MAX_MC + " — " + pinned);
			RequiredAnswers req = requiredPerCluster.computeIfAbsent(universeOf(cs.options()),
					_ -> new RequiredAnswers());
			req.fromAllSteps.addAll(pinned);
			mergeOrder(req.order, cs.orderHint());
		}
	}

	private static void mergeOrder(List<String> into, List<String> incoming) {
		if (incoming.isEmpty())
			return;
		if (into.isEmpty())
			into.addAll(incoming);
		else if (!into.equals(incoming))
			throw new RuntimeException("MC: widersprüchliche feste Reihenfolgen fürs selbe Universum: "
					+ into + " vs " + incoming);
	}

	private static Set<String> universeOf(Set<AnswerOption> options) {
		Set<String> texts = new HashSet<>();
		for (AnswerOption option : options)
			texts.add(option.text());
		return texts;
	}

	private static Set<String> pinnedOf(Set<AnswerOption> options) {
		Set<String> texts = new HashSet<>();
		for (AnswerOption option : options)
			if (option.role().alwaysShown())
				texts.add(option.text());
		return texts;
	}

	private static Map<String, Role> rolesOf(Set<AnswerOption> options) {
		Map<String, Role> roles = new HashMap<>();
		for (AnswerOption option : options)
			roles.put(option.text(), option.role());
		return roles;
	}

	/** Pflicht pinnen, aus dem Pool auffüllen, ordnen — einmal pro Cluster oder, wenn zu groß, je Step. */
	private List<String> buildDisplay(Set<String> universe, Set<String> pinned) {
		RequiredAnswers req = requiredPerCluster.get(universe);
		List<String> chosen = new ArrayList<>(pinned);

		List<String> pool = new ArrayList<>(universe);
		pool.removeAll(pinned);
		Collections.shuffle(pool);
		for (String text : pool) {
			if (chosen.size() >= MAX_MC)
				break;
			chosen.add(text);
		}

		if (req.order.isEmpty())
			Collections.shuffle(chosen);
		else
			chosen.sort(Comparator.comparingInt(req.order::indexOf));
		return chosen;
	}
}
