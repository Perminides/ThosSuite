package app.learn.anki;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import app.learn.Progress;
import app.learn.anki.model.Card;
import app.learn.anki.model.MultipleChoiceAnswers;
import app.learn.anki.model.Card.AnswerOption;
import app.learn.anki.model.Card.ClickMapElements;
import app.learn.anki.model.Card.Image;
import app.learn.anki.model.Card.Input;
import app.learn.anki.model.Card.MC;
import app.learn.anki.model.Card.MarkMapElements;
import app.learn.anki.model.Card.Output;
import app.learn.anki.model.Card.Pause;
import app.learn.anki.model.Card.Step;
import app.shared.Log;


/**
 * Existiert nur innerhalb einer (Lern-)Session. Wird von der Session erstellt und am Ende der Sesion auch wieder aus der Card
 * entfernt! Dass sie den Presenter kennt, ist nicht ganz sauber. Aber wir haben uns der Einfachheit darauf verständigt, weil
 * Events würden das nur aufblähen und das Konstrukt Panel - Presenter - Session - Progress ist halt auch ein enges...
 */
public class CardProgress implements Progress{
	
	private final SessionPresenter presenter;
	private final Card card;
	private final List<Card.Step> steps;
	private final AnkiDeckSession session;
	
	private Boolean correctlyAnswered = null; // null = Noch nicht gespielt.
	private LocalDateTime playedTimestamp = null;

	private int currentIndex = -1;
	private Set<String> clickedIds = new HashSet<>();
	private Set<Integer> clickedMcAnswers = new HashSet<>();
	private boolean isPaused = false;
	
	private List<String> lastMcOrder = null;
	private MultipleChoiceAnswers activeSessionMC = null;
	
	public CardProgress(Card hint, SessionPresenter presenter, AnkiDeckSession session) {
		this.card = hint;
		this.presenter = presenter;
		this.session = session;
		this.steps = hint.getSteps();
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
	    
	    if (!(step instanceof Input input)) {
	        return; // Ignorieren
	    }
	    
	    boolean correct = input.parts().stream()
	        .map(String::toLowerCase)
	        .anyMatch(p -> p.equals(text.trim().toLowerCase()));
	    
	    if (!correct) {
	        return; // Falsche Eingabe, nichts tun
	    }
	    
	    // Richtige Eingabe
	    presenter.textIsCorrect();
	    currentIndex++;
	    runSteps();
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
	    
	    // ----- FALSCH -----
	    if (!correct) {
	    	clickedIds.clear();
	    	playedTimestamp = LocalDateTime.now();
	        correctlyAnswered = false;
	        presenter.mapClickChecked(id, correct, input.mandatory());
	        isPaused = true;
	        return;
	    }
	    
	    presenter.mapClickChecked(id, true, null);
	    
	    // ----- NOCH NICHT VOLLSTÄNDIG -----
	    if (!clickedIds.containsAll(input.mandatory())) {
	        return; // Noch nicht alle Pflicht-Elemente geklickt
	    }
	    
	    // ----- VOLLSTÄNDIG -----
	    clickedIds.clear();
	    currentIndex++;
	    runSteps();
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
	    if (!(step instanceof MC mc)) {
	        return;
	    }
	    
	    // Prüfen gegen das Session-Objekt (das max 8 Antworten hat), nicht gegen das Original im Step!
	    if (activeSessionMC == null || activeSessionMC.getAnswerOptions().size() <= index)
	    	return;
	    
	    clickedMcAnswers.add(index);
	    boolean correct = activeSessionMC.isCorrectSoFar(clickedMcAnswers);
	    presenter.mcClickChecked(index, correct);
	    
	    // ----- FALSCH -----
	    if (!correct) {
	    	playedTimestamp = LocalDateTime.now();
	    	correctlyAnswered = false;
	    	clickedMcAnswers.clear();
	    	// Lösung für die aktuell angezeigten Optionen anzeigen
	        presenter.setCorrectMc(activeSessionMC.getCorrectIndexes());
	        isPaused = true;
	        return;
	    }
	    
	    // ----- NOCH NICHT VOLLSTÄNDIG -----
	    if (!activeSessionMC.isFinallyCorrect(clickedMcAnswers)) {
	        return; // Noch nicht alle Pflicht-Elemente geklickt
	    }
	    
	    // ----- VOLLSTÄNDIG -----
	    clickedMcAnswers.clear();
	    currentIndex++;
	    runSteps();
	}
	
	// ========================================
	// Other
	// ========================================
	
	public void endPause() {
		if (!isPaused)
			return;

		isPaused = false;

		if (correctlyAnswered != null && !correctlyAnswered) {
			cardFinished();
			return;
		}
		
		currentIndex++;
		runSteps();
	}
	
	public void cancel() {
		// ESC beendet auch eine Pause. Convenience...
		if (isPaused) {
			endPause();
			return;
		}
		
		// ESC während des Wartens auf Input beendet die Karte
		Step step = steps.get(currentIndex);
		if (step instanceof Input (var parts)) {   
			presenter.setCorrectText(parts.get(0));
			playedTimestamp = LocalDateTime.now();
			correctlyAnswered = false;
			isPaused = true;
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
        session.cardFinished(correctlyAnswered);
	}
	
	private void runSteps() {
	    if (currentIndex >= card.getSteps().size()) {
	    	// Wenn es nicht auf false steht, habe ich noch keinen Fehler gemacht, dann setze es auf true, wenn alle Steps durch.
	    	if (correctlyAnswered == null) {
	    		playedTimestamp = LocalDateTime.now();
	    		correctlyAnswered = true;
	    	}
	    	cardFinished();
	    	return;
	    }
	    
	    Step step = steps.get(currentIndex);
	    process(step);

	    if (!requiresUserInput(step)) {
	        currentIndex++;
	        runSteps(); // rekursiv weitermachen
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
			case Input _ -> presenter.waitForText();
			case Pause _ -> {	presenter.pause();
								isPaused = true;}
			case MC mcStep -> {
				// 1. Alle Texte des aktuellen Steps holen (um Vergleichbarkeit zu haben)
				Set<String> currentTexts = mcStep.options().stream()
						.map(AnswerOption::text)
						.collect(Collectors.toSet());
				
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
					lastMcOrder = sessionMc.getAnswerOptions().stream()
							.map(AnswerOption::text)
							.toList();
				}
				
				activeSessionMC = sessionMc;
				presenter.showMultipleChoice(sessionMc.getAnswerOptions().stream()
					    .map(AnswerOption::text)
					    .toList());
			}
			
			case MarkMapElements left -> presenter.markMapElements(left.left());
			default -> throw new IllegalStateException("Unsupported step: " + step);
		}
	}
	
	private boolean requiresUserInput(Step step) {
	    return step instanceof Card.Input
	        || step instanceof Card.MC
	        || step instanceof Card.ClickMapElements
	        || step instanceof Card.ClickZone
	        || step instanceof Card.Pause;
	}
}