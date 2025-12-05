package app.data;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public interface Progress {
	
	final static Random random = new Random();

	/**
	 * The new level is calculated by the gap , i. e. the number of days between today and
	 * lastPlayed, it is not based on the current level. The latter is only used to calculate the due date.
	 * 
	 * @param lastPlayed
	 * @param correctlyAnswered
	 */
	default int calculateNewLevel (LocalDate lastPlayed, boolean correctlyAnswered, boolean playAgainToday) {
		int newLevel;
		if (correctlyAnswered) {
			double rand = 0.1 - random.nextDouble() / 5;
			long gap = ChronoUnit.DAYS.between(lastPlayed, AppClock.TODAY);
			newLevel = (int)Math.max(1, Math.round((2 * gap + (2 * gap) * rand)));
		} else {
			newLevel = playAgainToday ? 0 : 1;
		}
		return newLevel;
	}
}
