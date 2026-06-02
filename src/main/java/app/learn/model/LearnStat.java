package app.learn.model;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import app.shared.AppClock;

/**
 * Wrapper um die card_learn_stat Tabelle. Ergibt nur Sinn, wenn sie von einer Lernkarte referenziert wird.
 */
public class LearnStat {
	private final static Random random = new Random();
	
	private LocalDate lastPlayed, firstPlayed;
	private int level;
	private int wrongCount;

	public LearnStat(LocalDate firstPlayed, LocalDate lastPlayed, int level, int wrongCount) {
		this.firstPlayed = firstPlayed;
		this.lastPlayed = lastPlayed;
		this.level = level;
		this.wrongCount = wrongCount; 
	}
	
	public LocalDate getDueDate() {
		return ChronoUnit.DAYS.addTo(lastPlayed, level);
	}
	
	public boolean isDueToday() {
		return getDueDate().isBefore(AppClock.TODAY.plusDays(1));
	}

	@Override
	public String toString() {
		return "LearnStat [lastPlayed=" + lastPlayed + ", currentWait=" + level + "]";
	}
	
	public LocalDate getLastPlayed() {
		return lastPlayed;
	}

	public int getCurrentLevel() {
		return level;
	}

	public int getWrongCount() {
		return wrongCount;
	}
	
	public void incrementWrongCount() {
		wrongCount++;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public void setLastPlayed(LocalDate date) {
		this.lastPlayed = date;
	}
}