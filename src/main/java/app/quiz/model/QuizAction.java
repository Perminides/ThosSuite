package app.quiz.model;

/**
 * Was mit einer Frage geschieht. Jede Aktion landet im Log und ist zugleich der neue Status der
 * Frage — Status und Aktion teilen sich die Werte, nur {@code open} ist reiner Status.
 *
 * <p>{@link #NO_CARD} steht für „Uninteressant“ wie für „Keine Karte“: Ob eine Karte einmal
 * vorgesehen war, verrät das Log, dort steht dann davor {@code card_requested} oder
 * {@code card_self}.</p>
 */
public enum QuizAction {
	KNOWN("known"),
	NO_CARD("no_card"),
	CARD_REQUESTED("card_requested"),
	CARD_SELF("card_self"),
	CARD_CREATED("card_created");

	private final String dbValue;

	QuizAction(String dbValue) {
		this.dbValue = dbValue;
	}

	public String dbValue() {
		return dbValue;
	}
}
