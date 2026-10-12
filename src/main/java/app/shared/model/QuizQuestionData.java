package app.shared.model;

/**
 * Was ein Quiz-Dialog von einer Frage anzeigt — die Datenvokabel über die JavaFX-Grenze, nicht die
 * Frage des Features. {@code topic} ist bei Mystery-Fragen {@code null}.
 */
public record QuizQuestionData(String category, String topic, String question, String answer) {
}
