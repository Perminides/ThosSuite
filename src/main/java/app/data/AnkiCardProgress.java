package app.data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.controller.AnkiDeckSession;
import app.data.AnkiCard.ClickMapElements;
import app.data.AnkiCard.Image;
import app.data.AnkiCard.Input;
import app.data.AnkiCard.MC;
import app.data.AnkiCard.MarkMapElements;
import app.data.AnkiCard.Output;
import app.data.AnkiCard.Pause;
import app.data.AnkiCard.Step;
import app.data.MultipleChoiceAnswers.AnswerOption;
import app.presenter.AnkiSessionPresenter;


/**
 * Existiert nur innerhalb einer (Lern-)Session. Wird von der Session erstellt und am Ende der Sesion auch wieder aus der Card
 * entfernt! Dass sie den Presenter kennt, ist nicht ganz sauber. Aber wir haben uns der Einfachheit darauf verständigt, weil
 * Events würden das nur aufblähen und das Konstrukt Panel - Presenter - Session - Progress ist halt auch ein enges...
 */
public class AnkiCardProgress implements Progress{
	
	private final AnkiSessionPresenter presenter;
	private final AnkiCard card;
	private final List<AnkiCard.Step> steps;
	private final AnkiDeckSession session;
	
	private Boolean correctlyAnswered = null; // null = Noch nicht gespielt.
	private LocalDateTime playedTimestamp = null;

	private int currentIndex = -1;
	private Set<String> clickedIds = new HashSet<>();
	private Set<Integer> clickedMcAnswers = new HashSet<>();
	private boolean isPaused = false;
	
	public AnkiCardProgress(AnkiCard hint, AnkiSessionPresenter presenter, AnkiDeckSession session) {
		this.card = hint;
		this.presenter = presenter;
		this.session = session;
		this.steps = hint.getSteps();
	}
	
	public void start() {
		System.out.println("Los geht es mit Karte: " + card.getId());
	    currentIndex = 0;
	    runSteps();
	}
	
	// ========================================
	// Input
	// ========================================

	public void checkTextInput(String text) {
	    if (isPaused)
	    	throw new RuntimeException("Aha, das kann also passieren. Na dann hier lieber einfach return machen :-)");
	    
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
	    
	    // User hat auf eine inaktive geklickt.
	    if (mc.mcAnswers().getAnswerOptions().size() <= index)
	    	return;
	    
	    clickedMcAnswers.add(index);
	    boolean correct = mc.mcAnswers().isCorrectSoFar(clickedMcAnswers);
	    presenter.mcClickChecked(index, correct);
	    
	    // ----- FALSCH -----
	    if (!correct) {
	    	playedTimestamp = LocalDateTime.now();
	    	correctlyAnswered = false;
	    	clickedMcAnswers.clear();
	        presenter.setCorrectMc(mc.mcAnswers().getCorrectIndexes());
	        isPaused = true;
	        return;
	    }
	    
	    // ----- NOCH NICHT VOLLSTÄNDIG -----
	    if (!mc.mcAnswers().isFinallyCorrect(clickedMcAnswers)) {
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
			case MC multipleChoiceAnswers -> presenter.showMultipleChoice(
					multipleChoiceAnswers.mcAnswers().getAnswerOptions().stream()
				    .map(AnswerOption::text)
				    .toList());
			case MarkMapElements left -> presenter.markMapElements(left.left());
			default -> throw new IllegalStateException("Unsupported step: " + step);
		}
	}
	
	private boolean requiresUserInput(Step step) {
	    return step instanceof AnkiCard.Input
	        || step instanceof AnkiCard.MC
	        || step instanceof AnkiCard.ClickMapElements
	        || step instanceof AnkiCard.ClickZone
	        || step instanceof AnkiCard.Pause;
	}
}
