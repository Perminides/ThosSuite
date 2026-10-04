package app.activity.model;

import java.util.List;

/**
 * Das Ergebnis einer Tagesberechnung: die Punkte und das, was dabei aufgefallen ist.
 *
 * <p>Die Notizen sind fertige Sätze, aber kein Dialog — wer sie wann zeigt, entscheidet der
 * Aufrufer. Deshalb kann die Berechnung auch aus dem Dashboard oder einer rückwirkenden Rechnung
 * laufen, ohne dass Fenster aufgehen.</p>
 *
 * @param points die Tagespunkte
 * @param notes  Auffälligkeiten, in der Reihenfolge der Aktivitäten; leer, wenn alles glatt war
 */
public record DayPoints(int points, List<String> notes) {}
