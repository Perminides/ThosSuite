package app.learn.anki;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import app.learn.anki.model.Card;
import app.learn.anki.model.FastAnswers;
import app.learn.anki.model.MultipleChoiceAnswers;
import app.learn.anki.model.Card.AnswerOption;
import app.learn.anki.model.Card.ClickMapElements;
import app.learn.anki.model.Card.Fast;
import app.learn.anki.model.Card.Image;
import app.learn.anki.model.Card.Input;
import app.learn.anki.model.Card.MC;
import app.learn.anki.model.Card.MarkMapElements;
import app.learn.anki.model.Card.Output;
import app.learn.anki.model.Card.Pause;
import app.learn.anki.model.Card.SketchImage;
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
	
	private List<String> lastMcOrder = null;
	private MultipleChoiceAnswers activeSessionMC = null;
	private FastAnswers activeFast = null;
	
	public CardProgress(Card hint, SessionPresenter presenter, SessionProgress sessionProgress) {
		this.card = hint;
		this.presenter = presenter;
		this.sessionProgress = sessionProgress;
		this.steps = hint.getSteps();
	}
	
	/** Die Antworttexte in ihrer aktuellen Reihenfolge. */
	private static List<String> textsOf(MultipleChoiceAnswers mc) {
		List<String> texts = new ArrayList<>();
		for (AnswerOption option : mc.getAnswerOptions())
			texts.add(option.text());
		return texts;
	}

	public void start() {
		Log.info(this, "Los geht es mit Karte: " + card.getId());
	    currentIndex = 0;
	    runSteps();
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
		
		// User klickt einfach mehrfach auf den gleichen Button, wenn mehr als eine Antwort gesucht wird...
		if (clickedMcAnswers.contains(index)) 
			return;
		
	    Step step = steps.get(currentIndex);
	     // Wir reagieren auf alle Klicks. Wenn wir im Pause-Modus sind, bleiben sie im Presenter hängen.
	     // Wenn nicht, dann müssen wir sie hier ignorieren.
	    if (!(step instanceof MC)) {
	        return;
	    }
	    
	    // Prüfen gegen das Session-Objekt (das max 8 Antworten hat), nicht gegen das Original im Step!
	    if (activeSessionMC == null || activeSessionMC.getAnswerOptions().size() <= index)
	    	return;
	    
	    clickedMcAnswers.add(index);
	    boolean correct = activeSessionMC.isCorrectSoFar(clickedMcAnswers);
	    presenter.mcClickChecked(index, correct);
	    
	    if (!correct) {
	        // ----- FALSCH -----
	    	playedTimestamp = LocalDateTime.now();
	    	correctlyAnswered = false;
	    	clickedMcAnswers.clear();
	    	// Lösung für die aktuell angezeigten Optionen anzeigen
	        presenter.setCorrectMc(activeSessionMC.getCorrectIndexes());
	        isPaused = true;
	    } else if (activeSessionMC.isFinallyCorrect(clickedMcAnswers)) {
	        // ----- VOLLSTÄNDIG -----
	        clickedMcAnswers.clear();
	        currentIndex++;
	        runSteps();
	    }
	    // ----- NOCH NICHT VOLLSTÄNDIG ----- : auf die restlichen Pflicht-Klicks warten
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
			case SketchImageMark mark -> presenter.markSketchArea(mark.area());
			case SketchImageFill fill -> presenter.fillSketchArea(fill.area(), fill.color());
			case Input _ -> presenter.waitForText();
			case Pause _ -> {	presenter.pause();
								isPaused = true;}
			case MC mcStep -> {
				// 1. Alle Texte des aktuellen Steps holen (um Vergleichbarkeit zu haben)
				Set<String> currentTexts = new HashSet<>();
				for (AnswerOption option : mcStep.options())
					currentTexts.add(option.text());
				
				MultipleChoiceAnswers sessionMc;

				// 2. Check: Sind es dieselben Texte wie beim letzten Mal? (Stabilität)
				if (lastMcOrder != null && currentTexts.equals(new HashSet<>(lastMcOrder))) {
					// JA -> Wir nehmen die Daten vom neuen Step (wegen Correct-Flags), erzwingen aber die alte Reihenfolge
					sessionMc = new MultipleChoiceAnswers(mcStep.options(), 8); // Copy-Konstruktor
					sessionMc.reorderToMatch(lastMcOrder);
				} else {
					// NEIN -> Neu würfeln und auf max 8 begrenzen
					sessionMc = new MultipleChoiceAnswers(mcStep.options(), 8); // Konstruktor mit Liste
					
					// Merken für den nächsten Step
					lastMcOrder = textsOf(sessionMc);
				}
				
				activeSessionMC = sessionMc;
				presenter.showMultipleChoice(textsOf(sessionMc));
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
	        || step instanceof Card.MC
	        || step instanceof Card.ClickMapElements
	        || step instanceof Card.Pause
	        || step instanceof Card.Fast;
	}
}