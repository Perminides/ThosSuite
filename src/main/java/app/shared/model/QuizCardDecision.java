package app.shared.model;

/** Die Antwort auf „Karte erstellt?“. */
public enum QuizCardDecision {
	CREATED("Karte erstellt"),
	REFINE("Nachbessern"),
	NOT_YET("Noch nicht"),
	DISCARD("Keine Karte"),
	DONE("Fertig");

	private final String text;

	QuizCardDecision(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
