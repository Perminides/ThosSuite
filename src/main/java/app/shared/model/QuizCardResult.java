package app.shared.model;

/** Ergebnis von „Karte erstellt?“. {@code instruction} ist der Hinweis an Claude, oder {@code null}. */
public record QuizCardResult(QuizCardDecision decision, String instruction) {
}
