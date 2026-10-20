package app.shared.model;

/**
 * Die Antwort auf eine Quizfrage. {@link #SKIP} und {@link #DONE} gehen jederzeit, die übrigen erst,
 * wenn die Lösung aufgedeckt ist. {@link #CARD} holt einen Vorschlag von Claude, danach erscheint
 * dieselbe Frage wieder.
 */
public enum QuizAnswer {
	KNOWN("Weiß ich"),
	CARD("Claude"),
	CREATED("Karte erstellt"),
	NOT_INTERESTED("Wird keine Karte"),
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
