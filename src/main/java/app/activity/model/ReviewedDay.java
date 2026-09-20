package app.activity.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Ein Tag, nachdem der Review-Dialog ihn bestätigt oder korrigiert hat. Grundlage der
 * Punkteberechnung und der Inhalt von {@code adjusted_data}.
 *
 * @param date       der lokale Kalendertag
 * @param steps      die Tagesschritte, wie sie im Dialog standen
 * @param activities die Aktivitäten des Tages in der Reihenfolge des Dialogs
 */
public record ReviewedDay(LocalDate date, int steps, List<ReviewedActivity> activities) {}
