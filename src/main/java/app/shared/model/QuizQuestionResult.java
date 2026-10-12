package app.shared.model;

/** Ergebnis des Fragedialogs. {@code instruction} ist der Hinweis an Claude, oder {@code null}. */
public record QuizQuestionResult(QuizAnswer answer, String instruction) {
}
