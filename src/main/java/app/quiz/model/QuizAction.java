package app.quiz.model;

/**
 * Was mit einer Frage geschieht. Jede Aktion landet im Log und ist zugleich der neue Status der
 * Frage — Status und Aktion teilen sich die Werte, nur {@code open} ist reiner Status.
 */
public enum QuizAction {
	KNOWN("known"),
	BORING("boring"),
	CARD_REQUESTED("card_requested"),
	CARD_SELF("card_self"),
	CARD_CREATED("card_created"),
	CARD_DISCARDED("card_discarded");

	private final String dbValue;

	QuizAction(String dbValue) {
		this.dbValue = dbValue;
	}

	public String dbValue() {
		return dbValue;
	}
}
