package app.quiz;

import java.util.HashSet;
import java.util.Set;

import app.quiz.model.PendingCard;
import app.quiz.model.Prompt;
import app.quiz.model.QuizAction;
import app.quiz.model.Question;
import app.quiz.model.Suggestion;
import app.quiz.repository.Repository;
import app.shared.model.ButtonEnum;
import app.shared.model.QuizCardResult;
import app.shared.model.QuizQuestionData;
import app.shared.model.QuizQuestionResult;
import app.shared.ui.Alerts;
import app.shared.ui.QuizCardDialog;
import app.shared.ui.QuizQuestionDialog;
import app.shared.ui.WaitDialog;

/**
 * Eine Runde Quizfragen, aufgerufen über das Menü. Erst kommen die Fragen, aus denen eine Karte
 * werden soll („Karte erstellt?“), dann zufällige offene Fragen, bis „Fertig“ geklickt wird oder
 * keine mehr offen ist. Jede Antwort wird sofort gespeichert; „Fertig“ verwirft also nichts.
 * „Überspringen“ speichert nichts — die Frage bleibt offen und kommt erst in der nächsten Runde
 * wieder.
 */
public class QuizRound {

	private final Repository repository = new Repository();

	public void start() {
		for (PendingCard card : repository.pendingCards()) {
			if (!continueAfterCard(card.question(), card.suggestion()))
				return;
		}

		Set<Integer> skipped = new HashSet<>();
		Question question = repository.randomOpen(skipped);
		while (question != null) {
			QuizQuestionResult result = QuizQuestionDialog.show(data(question));
			switch (result.answer()) {
				case SKIP -> skipped.add(question.id()); // bleibt offen, nur nicht mehr in dieser Runde
				case KNOWN -> repository.record(question.id(), QuizAction.KNOWN);
				case BORING -> repository.record(question.id(), QuizAction.BORING);
				case OWN_CARD -> repository.record(question.id(), QuizAction.CARD_SELF);
				case CARD -> {
					Suggestion suggestion = fetchSuggestion(question, result.instruction());
					if (suggestion != null && !continueAfterCard(question, suggestion.text()))
						return;
				}
				case DONE -> {
					return;
				}
			}
			question = repository.randomOpen(skipped);
		}
		Alerts.show("Quiz", skipped.isEmpty() ? "Keine offenen Fragen mehr."
				: "Keine offenen Fragen mehr außer den übersprungenen.", ButtonEnum.OK);
	}

	/**
	 * „Karte erstellt?“, so oft, bis eine Entscheidung fällt — „Nachbessern“ holt einen neuen
	 * Vorschlag und fragt erneut.
	 *
	 * @param suggestion der neueste Vorschlag, oder {@code null}, wenn es noch keinen gibt
	 * @return {@code false}, wenn die Runde enden soll
	 */
	private boolean continueAfterCard(Question question, String suggestion) {
		while (true) {
			QuizCardResult result = QuizCardDialog.show(data(question), suggestion);
			switch (result.decision()) {
				case CREATED -> {
					repository.record(question.id(), QuizAction.CARD_CREATED);
					return true;
				}
				case DISCARD -> {
					repository.record(question.id(), QuizAction.CARD_DISCARDED);
					return true;
				}
				case NOT_YET -> {
					return true; // bleibt angefordert und kommt beim nächsten Mal wieder
				}
				case DONE -> {
					return false;
				}
				case REFINE -> {
					Suggestion refined = fetchSuggestion(question, result.instruction());
					if (refined != null)
						suggestion = refined.text();
				}
			}
		}
	}

	/**
	 * Holt einen Vorschlag mit dem neuesten Prompt und speichert ihn. Scheitert die API, kommt eine
	 * Meldung, und der Status der Frage bleibt, wie er war.
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
