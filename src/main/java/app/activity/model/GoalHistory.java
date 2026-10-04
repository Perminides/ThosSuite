package app.activity.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Die Wochenziel-Historie als Ganzes — die Einträge und die eine Frage, die man ihnen stellt.
 *
 * <p>Erwartet die Einträge aufsteigend nach {@code validFrom}, so wie das Repository sie liefert.</p>
 */
public record GoalHistory(List<GoalHistoryEntry> entries) {

	/**
	 * Das Ziel, das am gegebenen Datum galt — der neueste Eintrag, dessen {@code validFrom} nicht
	 * hinter dem Datum liegt.
	 *
	 * <p>Gibt es keinen, ist das kein Sonderfall zum Füllen: ohne Ziel gibt es keine Aussage
	 * darüber, ob eine Woche grün war.</p>
	 */
	public int goalForDate(LocalDate date) {
		GoalHistoryEntry lastValid = null;
		for (GoalHistoryEntry entry : entries)
			if (!entry.validFrom().isAfter(date))
				lastValid = entry;

		if (lastValid == null)
			throw new RuntimeException("Kein Wochenziel gefunden für " + date);

		return lastValid.weeklyGoal();
	}
}
