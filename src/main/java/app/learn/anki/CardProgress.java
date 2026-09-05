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
import app.learn.anki.model.Card.SketchImageMove;
import app.learn.anki.model.Card.SketchImageFill;
import app.learn.anki.model.Card.SketchImageMark;
import app.learn.anki.model.Card.Step;
import app.shared.Log;


/**
 * Existiert nur innerhalb einer (Lern-)Session. Wird vom AnkiSessionProgress erstellt und in dessen
 * Map gehalten (Karten-Id -> CardProgress). Mit dem Ende des AnkiSessionProgress stirbt die Map - es
 * gibt nichts mehr aus der Card zu entfernen. Dass sie den Presenter kennt, ist nicht ganz sauber.
 * Aber wir haben uns der Einfachheit darauf verständigt, weil Events würden das nur aufblähen und das
 * Konstrukt Panel - Presenter - SessionProgress - Progress ist halt auch ein enges...
 */
public class CardProgress {

	/** Zuschlag auf die erste Antwort eines Fast-Steps — Zeit, die Frage überhaupt zu lesen. */
	private static final int READ_BONUS_SECONDS = 3;

	private final SessionPresenter presenter;
	private final Card card;
	private final List<Card.Step> steps;
	private final SessionProgress sessionProgress;
	
	private Boolean correctlyAnswered = null; // null = Noch nicht gespielt.
	private LocalDateTime playedTimestamp = null;

	private int currentIndex = -1;
	private Set<String> clickedIds = new HashSet<>();
	private Set<Integer> clickedMcAnswers = new HashSet<>();
	private boolean isPaused = false;
	
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
	 * Netz, kein Zwang — erzwungen wird nur die Pflicht-Menge ≤ {@value #MAX_MC}.</p>
	 */
	private void planMcClusters() {
		requiredPerCluster = new HashMap<>();
		for (Step step : steps) {
			if (!(step instanceof ChoiceStep cs))
				continue;
			RequiredAnswers req = requiredPerCluster.computeIfAbsent(universeOf(cs.options()),
					_ -> new RequiredAnswers());
			req.fromAllSteps.addAll(pinnedOf(cs.options()));
			mergeOrder(req.order, cs.orderHint());
		}
		for (Map.Entry<Set<String>, RequiredAnswers> entry : requiredPerCluster.entrySet())
			if (entry.getValue().fromAllSteps.size() > MAX_MC)
				throw new RuntimeException("MC: Pflicht-Menge größer als " + MAX_MC + " — " + entry.getKey());
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
			if (option.role() == Role.CORRECT || option.role() == Role.WRONG_ALWAYS_SHOWN)
				texts.add(option.text());
		return texts;
	}

	private static Map<String, Role> rolesOf(Set<AnswerOption> options) {
		Map<String, Role> roles = new HashMap<>();
		for (AnswerOption option : options)
			roles.put(option.text(), option.role());
		return roles;
	}

	/** Einmal pro Cluster und Durchlauf: Pflicht pinnen, aus dem Pool auffüllen, ordnen, einfrieren. */
	private List<String> buildFrozen(Set<String> universe) {
		RequiredAnswers req = requiredPerCluster.get(universe);
		List<String> chosen = new ArrayList<>(req.fromAllSteps);

		List<String> pool = new ArrayList<>(universe);
		pool.removeAll(req.fromAllSteps);
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
	
	// ========================================
	// Input
	// ========================================

	public void checkTextInput(String text) {
	    if (isPaused)
	    	return;

	    Step step = steps.get(currentIndex);

	    if (step instanceof Fast) {
	        checkFastInput(text);
	        return;
	    }

	    if (!(step instanceof Input input)) {
	        return; // Ignorieren
	    }
	    
	    String typed = text.trim().toLowerCase();
	    boolean correct = false;
	    for (String part : input.parts())
	        if (part.toLowerCase().equals(typed)) {
	            correct = true;
	            break;
	        }
	    
	    if (!correct) {
	        return; // Falsche Eingabe, nichts tun
	    }
	    
	    // Richtige Eingabe
	    presenter.textIsCorrect();
	    currentIndex++;
	    runSteps();
	}
	
	// ========================================
	// Fast
	// ========================================

	/**
	 * Eine erkannte Antwort rastet in ihr Feld ein und zieht die Uhr wieder voll auf. Was nicht passt
	 * läuft ins Leere, die Uhr tickt weiter.
	 */
	private void checkFastInput(String text) {
		FastAnswers.Hit hit = activeFast.accept(text);
		if (hit == null)
			return;

		presenter.fastAnswerCorrect(hit.slot(), hit.text(), activeFast.expectedSlot());
		if (activeFast.isComplete()) {
			presenter.stopClock();
			currentIndex++;
			runSteps();
		} else {
			presenter.restartClock();
		}
	}

	/** Die Uhr ist abgelaufen: aufdecken, Karte ist falsch, und es geht in die Pause. */
	public void timeUp() {
		playedTimestamp = LocalDateTime.now();
		correctlyAnswered = false;
		presenter.fastTimeUp(activeFast.revealRest());
		isPaused = true;
	}

	// ========================================
	// Map
	// ========================================

	public void elementClicked(String id) {
	    clickedIds.add(id);
	    Step step = steps.get(currentIndex);
	    
	    // Wir reagieren auf alle Klicks. Wenn wir im Pause-Modus sind, bleiben sie im Presenter hängen.
	    // Wenn nicht, dann müssen wir sie hier ignorieren.
	    if (!(step instanceof ClickMapElements input)) {
	        return;
	    }
	    
	    boolean correct = input.mandatory().contains(id) 
	                   || input.optional().contains(id);
	    
	    if (!correct) {
	        // ----- FALSCH -----
	    	clickedIds.clear();
	    	playedTimestamp = LocalDateTime.now();
	        correctlyAnswered = false;
	        presenter.mapClickChecked(id, correct, input.mandatory());
	        isPaused = true;
	    } else {
	        presenter.mapClickChecked(id, true, null);
	        if (clickedIds.containsAll(input.mandatory())) {
	            // ----- VOLLSTÄNDIG -----
	            clickedIds.clear();
	            currentIndex++;
	            runSteps();
	        }
	        // ----- NOCH NICHT VOLLSTÄNDIG ----- : auf die restlichen Pflicht-Klicks warten
	    }
	}
	
	// ========================================
	// Multiple Choice
	// ========================================

	public void mcClicked(int index) {
		if (isPaused)
	        throw new RuntimeException("Aha, das kann also passieren. Na dann hier lieber einfach return machen :-)");
		
	    Step step = steps.get(currentIndex);
	     // Wir reagieren auf alle Klicks. Wenn wir im Pause-Modus sind, bleiben sie im Presenter hängen.
	     // Wenn nicht, dann müssen wir sie hier ignorieren.
	    if (!(step instanceof ChoiceStep)) {
	        return;
	    }

	    // Prüfen gegen das Session-Objekt (das max 8 Antworten hat), nicht gegen das Original im Step!
	    if (activeSessionMC == null || activeSessionMC.getAnswerOptions().size() <= index)
	    	return;

	    if (step instanceof MCPlus) {
	    	// Eine tolerierte Antwort haftet nicht. Sie zeigt sich falsch und bleibt aus der Auswahl
	    	// heraus — sonst koennte man sie STATT der richtigen abschicken, und genau das soll sie
	    	// nicht sein: nicht bestraft, aber auch nicht die gesuchte Antwort. Damit verhaelt sich
	    	// der Sammelmodus hier wie der Einzelklick weiter unten.
	    	if (activeSessionMC.roleAt(index) == Role.TOLERATED) {
	    		presenter.mcClickChecked(index, false);
	    		return;
	    	}
	    	// Sonst wird nur markiert — geprüft wird erst beim Absenden.
	    	boolean wasMarked = clickedMcAnswers.contains(index);
	    	if (wasMarked)
	    		clickedMcAnswers.remove(index);
	    	else
	    		clickedMcAnswers.add(index);
	    	presenter.mcMarked(index, !wasMarked);
	    	return;
	    }

	    // Einzelklick (MC), sofort gewertet.
	    Role role = activeSessionMC.roleAt(index);

	    if (role == Role.TOLERATED) {
	    	// Falsch, aber kein Abbruch: markieren und weiter warten — wertungsfrei.
	    	presenter.mcClickChecked(index, false);
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
	            clickedMcAnswers.clear();
	            currentIndex++;
	            runSteps();
	        }
	        // ----- NOCH NICHT VOLLSTÄNDIG ----- : auf die restlichen Pflicht-Klicks warten
	    } else {
	        // ----- FALSCH ----- (fest sichtbar oder Füller)
	        presenter.mcClickChecked(index, false);
	    	playedTimestamp = LocalDateTime.now();
	    	correctlyAnswered = false;
	    	clickedMcAnswers.clear();
	    	// Lösung für die aktuell angezeigten Optionen anzeigen
	        presenter.setCorrectMc(activeSessionMC.getCorrectIndexes());
	        isPaused = true;
	    }
	}

	/**
	 * Die markierte Auswahl wird als Ganzes geprüft. Falsch ist sie schon dann, wenn eine richtige
	 * Antwort fehlt — aufgedeckt werden die falsch gewählten rot und alle richtigen grün.
	 */
	public void mcSubmitted() {
		if (isPaused || activeSessionMC == null || !(steps.get(currentIndex) instanceof MCPlus))
			return;
		if (clickedMcAnswers.isEmpty())
			return;

		if (activeSessionMC.isFinallyCorrect(clickedMcAnswers)) {
			// ----- RICHTIG -----
			clickedMcAnswers.clear();
			currentIndex++;
			runSteps();
		} else {
			// ----- FALSCH -----
			Set<Integer> correctIndexes = activeSessionMC.getCorrectIndexes();
			for (Integer clicked : clickedMcAnswers)
				if (!correctIndexes.contains(clicked))
					presenter.mcClickChecked(clicked, false);
			presenter.setCorrectMc(correctIndexes);
			playedTimestamp = LocalDateTime.now();
			correctlyAnswered = false;
			clickedMcAnswers.clear();
			isPaused = true;
		}
	}

	// ========================================
	// Other
	// ========================================
	
	/**
	 * Die Pause-Taste. Steht die Karte in einer echten Pause, beendet sie diese. Läuft dagegen gerade
	 * ein Fast-Step, friert sie nur die Uhr ein beziehungsweise taut sie wieder auf — die
	 * Texterkennung läuft dabei weiter, damit sich ein Vertipper in Ruhe korrigieren lässt.
	 */
	public void pauseKeyPressed() {
		if (!isPaused) {
			if (currentIndex < steps.size() && steps.get(currentIndex) instanceof Fast)
				presenter.toggleClock();
			return;
		}

		isPaused = false;

		if (correctlyAnswered != null && !correctlyAnswered) {
			cardFinished();
		} else {
			currentIndex++;
			runSteps();
		}
	}
	
	public void cancel() {
		if (isPaused) {
			// ESC beendet auch eine Pause. Convenience...
			pauseKeyPressed();
		} else {
			// ESC während des Wartens auf Input beendet die Karte
			Step step = steps.get(currentIndex);
			if (step instanceof Input (var parts)) {
				presenter.setCorrectText(parts.get(0));
				playedTimestamp = LocalDateTime.now();
				correctlyAnswered = false;
				isPaused = true;
			}
		}
	}
	
	public boolean isPaused() {
		return isPaused;
	}
	
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
	 * @param correctlyAnswered
	 */
	private void cardFinished() {
		if (correctlyAnswered == null)
			throw new RuntimeException("Wieso wurde correctlyAnswered nicht gesetzt vor dem Aufruf von cardFinished?");
        sessionProgress.cardFinished(correctlyAnswered);
	}
	
	private void runSteps() {
	    if (currentIndex >= card.getSteps().size()) {
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
		/**
		 * Neues switch mit Pattern Matching ab Java 21
		 */
		switch (step) {
			case Output output -> presenter.showQuestion(output.text());
			case ClickMapElements x -> {
			    Set<String> allShapes = new HashSet<>(x.mandatory());
			    allShapes.addAll(x.optional());
			    presenter.waitForClick(allShapes);
			}
			case Image image -> presenter.showImage(image.file());
			case SketchImage sketch -> presenter.showSketch(sketch.structure());
			case SketchImageAdd add -> presenter.addSketch(add.structure(), add.cell(), add.size(),
					add.offsetX(), add.offsetY());
			case SketchImageMove move -> presenter.moveSketchArea(move.area(), move.cell());
			case SketchImageMark mark -> presenter.markSketchAreas(mark.areas());
			case SketchImageFill fill -> presenter.fillSketchAreas(fill.areas(), fill.color());
			case Input _ -> presenter.waitForText();
			case Pause _ -> {	presenter.pause();
								isPaused = true;}
			case ChoiceStep cs -> {
				Set<String> universe = universeOf(cs.options());
				List<String> frozen = frozenDisplay.computeIfAbsent(universe, this::buildFrozen);

				Map<String, Role> roles = rolesOf(cs.options());
				List<AnswerOption> shown = new ArrayList<>();
				for (String text : frozen)
					shown.add(new AnswerOption(text, roles.get(text)));

				activeSessionMC = new MultipleChoiceAnswers(shown);
				clickedMcAnswers.clear(); // sonst zählt die Auswahl des vorigen Steps weiter mit
				presenter.showMultipleChoice(frozen);
			}
			
			case MarkMapElements left -> presenter.markMapElements(left.left());

			case Fast fast -> {
				activeFast = new FastAnswers(fast);
				presenter.showFastStep(activeFast.slotHints(), activeFast.expectedSlot(),
						fast.seconds() + READ_BONUS_SECONDS, fast.seconds());
			}
		}
	}

	private boolean requiresUserInput(Step step) {
	    return step instanceof Card.Input
	        || step instanceof Card.ChoiceStep
	        || step instanceof Card.ClickMapElements
	        || step instanceof Card.Pause
	        || step instanceof Card.Fast;
	}
}