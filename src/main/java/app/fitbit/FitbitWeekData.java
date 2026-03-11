package app.fitbit;

import java.time.LocalDate;

/**
 * Repräsentiert die Fitbit-Punkte für eine Woche.
 * 
 * @param weekStart Montag der Woche
 * @param points Gesamtpunkte dieser Woche
 */
public record FitbitWeekData(LocalDate weekStart, int points, String remark) {}