package app.shared.model;

import java.time.LocalDate;

/**
 * Was eine Karte bisher erlebt hat. Dummes Eingabe-DTO für das Historienfeld der Lernansicht:
 * fertige Werte hinein, die Darstellung entscheidet die Anzeige.
 *
 * <p>Eine Karte ohne Verlauf gibt es hier nicht — „noch nie gespielt" sagt der Presenter mit
 * {@code clearCardHistory()}, nicht mit einem leeren Record.</p>
 */
public record CardHistory(LocalDate lastPlayed, int level, int wrongCount) {}
