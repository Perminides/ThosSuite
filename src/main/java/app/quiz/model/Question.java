package app.quiz.model;

/** Eine Quizfrage, wie sie in der Quiz-DB steht. {@code topic} ist bei Mystery-Fragen {@code null}. */
public record Question(int id, String category, String topic, String question, String answer) {
}
