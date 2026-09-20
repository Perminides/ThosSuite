package app.activity.model;

import java.time.LocalDate;

/**
 * Die Punkte einer Woche.
 *
 * @param weekStart Montag der Woche
 * @param points    Gesamtpunkte dieser Woche
 * @param remark    Anmerkung zur Woche; {@code null}, wenn keine hinterlegt ist
 */
public record WeekData(LocalDate weekStart, int points, String remark) {}
