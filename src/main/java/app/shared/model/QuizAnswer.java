package app.shared.model;

/**
 * Die Antwort auf eine Quizfrage. {@link #SKIP} und {@link #DONE} gehen jederzeit, die übrigen erst,
 * wenn die Lösung aufgedeckt ist.
 */
public enum QuizAnswer {
	KNOWN("Weiß ich"),
	CARD("Claude"),
	OWN_CARD("Mach ich selbst"),
	NOT_INTERESTED("Uninteressant"),
	SKIP("Überspringen"),
	DONE("Fertig");

	private final String text;

	QuizAnswer(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
