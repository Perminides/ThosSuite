package app.quiz.model;

/** Ein Prompt aus der Quiz-DB: der komplette Anfrage-Body an die Claude-API, als JSON-Text. */
public record Prompt(int version, String text) {
}
