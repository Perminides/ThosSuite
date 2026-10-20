package app.quiz;

import java.util.HashSet;
import java.util.Set;

import app.quiz.model.Prompt;
import app.quiz.model.QuizAction;
import app.quiz.model.Question;
import app.quiz.model.Suggestion;
import app.quiz.repository.Repository;
import app.shared.model.ButtonEnum;
import app.shared.model.QuizQuestionData;
import app.shared.model.QuizQuestionResult;
import app.shared.ui.Alerts;
import app.shared.ui.QuizQuestionDialog;
import app.shared.ui.WaitDialog;

/**
 * Eine Runde Quizfragen, aufgerufen über das Menü: offene Fragen, bis „Fertig“ geklickt wird oder
 * keine mehr offen ist — zuerst die mit Vorschlag von Claude, über die schon nachgedacht wurde,
 * dann zufällig. Jede Antwort wird sofort gespeichert; „Fertig“ verwirft also nichts.
 * „Überspringen“ speichert nichts — die Frage bleibt offen und kommt erst in der nächsten Runde
 * wieder.
 */
public class QuizRound {

	private final Repository repository = new Repository();

	public void start() {
		Set<Integer> skipped = new HashSet<>();
		Question question = repository.nextOpen(skipped);
		while (question != null) {
			if (!continueAfterQuestion(question, skipped))
				return;
			question = repository.nextOpen(skipped);
		}
		Alerts.show("Quiz", skipped.isEmpty() ? "Keine offenen Fragen mehr."
				: "Keine offenen Fragen mehr außer den übersprungenen.", ButtonEnum.OK);
	}

	/**
	 * Stellt eine Frage, so oft, bis eine Antwort fällt — „Claude“ holt einen Vorschlag und zeigt
	 * dieselbe Frage aufgedeckt mit ihm wieder.
	 *
	 * @return {@code false}, wenn die Runde enden soll
	 */
	private boolean continueAfterQuestion(Question question, Set<Integer> skipped) {
		String suggestion = repository.latestSuggestion(question.id());
		boolean revealed = false;
		while (true) {
			QuizQuestionResult result = QuizQuestionDialog.show(data(question), suggestion, revealed);
			switch (result.answer()) {
				case KNOWN -> {
					repository.record(question.id(), QuizAction.KNOWN);
					return true;
				}
				case CREATED -> {
					repository.record(question.id(), QuizAction.CARD_CREATED);
					return true;
				}
				case NOT_INTERESTED -> {
					repository.record(question.id(), QuizAction.NO_CARD);
					return true;
				}
				case SKIP -> {
					skipped.add(question.id()); // bleibt offen, nur nicht mehr in dieser Runde
					return true;
				}
				case DONE -> {
					return false;
				}
				case CARD -> {
					Suggestion fetched = fetchSuggestion(question, result.instruction());
					if (fetched != null)
						suggestion = fetched.text();
					revealed = true;
				}
			}
		}
	}

	/**
	 * Holt einen Vorschlag mit dem neuesten Prompt und speichert ihn. Scheitert die API, kommt eine
	 * Meldung.
	 *
	 * @param instruction der Hinweis an Claude, oder {@code null}
	 * @return den Vorschlag, oder {@code null}, wenn die API keinen geliefert hat
	 */
	private Suggestion fetchSuggestion(Question question, String instruction) {
		Prompt prompt = repository.latestPrompt();
		ClaudeClient client = new ClaudeClient();
		Suggestion suggestion;
		try {
			suggestion = WaitDialog.run("Claude schreibt einen Kartenvorschlag …",
					() -> client.suggest(prompt.text(), question, instruction));
		} catch (ClaudeException e) {
			Alerts.show("Kein Kartenvorschlag", e.getMessage(), ButtonEnum.OK);
			return null;
		}
		repository.saveSuggestion(question.id(), suggestion, prompt.version(), instruction);
		return suggestion;
	}

	private static QuizQuestionData data(Question question) {
		return new QuizQuestionData(question.category(), question.topic(), question.question(), question.answer());
	}
}
