package app.learn.model;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import app.shared.AppClock;

/**
 * Wrapper um die card_learn_stat Tabelle. Ergibt nur Sinn, wenn sie von einer Lernkarte referenziert wird.
 * !Sofort: Der Kommentar oben ist Quatsch! Learnstat wird auch von region genutzt und das ist ja auch richtig so.
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

	/**
	 * Das neue Level nach einer Antwort — die Spaced-Repetition-Formel der Suite.
	 *
	 * <p>Sie rechnet aus dem <b>Abstand</b> zum letzten Spielen, nicht aus dem bisherigen Level:
	 * Wer eine Karte nach 30 Tagen noch kann, bekommt sie später wieder als jemand, der sie nach
	 * 3 Tagen konnte. Das bisherige Level geht nur in die Fälligkeit ein (siehe getDueDate).
	 * Ein bisschen Zufall (±10 %) verteilt die Fälligkeiten, damit nicht alles am selben Tag
	 * zusammenkommt.</p>
	 *
	 * @param correct         wurde richtig geantwortet
	 * @param playAgainToday  soll die Karte bei einem Fehler heute nochmal drankommen (Anki: ja,
	 *                        Region: nein — dort ist die Session nach einem Fehler vorbei)
	 */
	public int calculateNewLevel(boolean correct, boolean playAgainToday) {
		if (!correct)
			return playAgainToday ? 0 : 1;

		double rand = 0.1 - random.nextDouble() / 5;
		long gap = ChronoUnit.DAYS.between(lastPlayed, AppClock.TODAY);
		return (int) Math.max(1, Math.round(2 * gap + (2 * gap) * rand));
	}
}