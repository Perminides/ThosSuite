package app.activity.model;

import java.time.LocalDate;

/**
 * Ein Eintrag aus der Wochenziel-Historie.
 *
 * @param validFrom  Datum, ab dem dieses Ziel gilt
 * @param weeklyGoal das Wochenziel in Punkten
 */
public record GoalHistoryEntry(LocalDate validFrom, int weeklyGoal) {}
