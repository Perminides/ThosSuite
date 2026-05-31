package app.fitbit.model;

import java.time.LocalDate;

/**
 * Repräsentiert einen Eintrag aus der Fitbit-Ziel-Historie.
 * 
 * @param validFrom Datum ab dem dieses Ziel gültig ist
 * @param weeklyGoal Das Wochenziel in Punkten
 */
public record FitbitGoalHistoryEntry(LocalDate validFrom, int weeklyGoal) {}