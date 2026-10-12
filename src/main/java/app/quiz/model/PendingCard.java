package app.quiz.model;

/**
 * Eine Frage, aus der eine Karte werden soll und die noch nicht erledigt ist.
 * {@code suggestion} ist {@code null}, wenn die Karte ohne Claude entsteht.
 */
public record PendingCard(Question question, String suggestion) {
}
