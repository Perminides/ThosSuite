package app.quiz.model;

/**
 * Die Antwort auf eine Frage, gespeichert als ihr neuer Status samt Zeitpunkt. Daneben gibt es die
 * Status {@code open} und {@code paused}; den zweiten setzt nur der Nutzer von Hand.
 *
 * <p>Ein Vorschlag von Claude ist keine Antwort: Er landet in {@code card_suggestion}, und die
 * Frage bleibt offen.</p>
 */
public enum QuizAction {
	KNOWN("known"),
	NO_CARD("no_card"),
	CARD_CREATED("card_created");

	private final String dbValue;

	QuizAction(String dbValue) {
		this.dbValue = dbValue;
	}

	public String dbValue() {
		return dbValue;
	}
}
