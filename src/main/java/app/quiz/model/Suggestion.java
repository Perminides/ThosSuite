package app.quiz.model;

/** Ein Kartenvorschlag von Claude. {@code model} ist das Modell, das laut API geantwortet hat. */
public record Suggestion(String text, String model) {
}
