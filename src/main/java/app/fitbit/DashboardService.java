package app.fitbit;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import app.fitbit.model.WeekData;
import app.fitbit.repository.Repository;

public class DashboardService {
    
    private final Repository repository;
    
    public DashboardService() {
        this.repository = new Repository();
    }
    
    /**
     * Berechnet die durchschnittlich benötigten Schritte pro Tag
     * für die verbleibenden Tage dieser Woche, um das Wochenziel zu erreichen.
     */
    public int calculateRemainingDailySteps(LocalDate today) {
        // 1. Aktuelle Woche bestimmen (Montag bis Sonntag)
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        // 2. Letzten importierten Tag holen
        LocalDate lastDay = repository.getLastImportedDate().orElse(monday.minusDays(1));
        
        // 3. Nächster fehlender Tag
        LocalDate nextDay = lastDay.plusDays(1);
        
        // 4. Wenn nextDay VOR dieser Woche liegt → setze auf Montag
        if (nextDay.isBefore(monday)) {
            nextDay = monday;
        }
        
        // 5. Verbleibende Tage berechnen
        int t = (int) nextDay.datesUntil(sunday.plusDays(1)).count();
        
        // 6. Punkte dieser Woche holen
        int p = repository.getPointsForWeek(today);
        
        // 7. Wochenziel
        int z = repository.getWeeklyGoalForDate(monday);
        
        // 8. Points-per-Step Konstante
        double s = PointsCalculator.POINTS_FOR_STEP;
        
        // 9. Berechnung: (z - p) / (t * s)
        if (t == 0) return 0; // Kann eigentlich nicht passieren, aber safety check
        
        double stepsNeeded = (z - p) / (t * s);
        
        return (int) Math.round(stepsNeeded);
    }
    
    /**
     * Berechnet den aktuellen Streak in vollständigen grünen Wochen.
     * Eine Woche ist grün, wenn ihre Punkte >= dem Wochenziel sind.
     * Die aktuelle (unvollständige) Woche wird ignoriert.
     * 
     * @param today Das heutige Datum
     * @return Anzahl der aufeinanderfolgenden grünen Wochen (ab der letzten vollständigen Woche)
     */
    public int calculateCurrentStreak(LocalDate today) {
        List<WeekData> weeks = repository.getWeeksInRange(LocalDate.now().minus(9999, ChronoUnit.WEEKS), LocalDate.now());
        
        if (weeks.isEmpty()) {
            return 0;
        }
        
        // Prüfen ob die neueste Woche die aktuelle (unvollständige) Woche ist
        LocalDate currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        WeekData newestWeek = weeks.get(weeks.size() - 1);
        
        int startIndex = weeks.size() - 1;
        if (newestWeek.weekStart().equals(currentMonday) && today.getDayOfWeek() != DayOfWeek.MONDAY) {
            // Aktuelle unvollständige Woche überspringen
            startIndex = weeks.size() - 2;
        }
        
        if (startIndex < 0) {
            return 0;
        }
        
        // Von hinten nach vorne durch vollständige Wochen gehen
        int streak = 0;
        for (int i = startIndex; i >= 0; i--) {
            WeekData week = weeks.get(i);
            int goal = repository.getWeeklyGoalForDate(week.weekStart());
            
            if (week.points() >= goal) {
                streak++;
            } else {
                // Erste rote Woche → Streak endet
                break;
            }
        }
        
        return streak;
    }
    
    /**
     * Berechnet den längsten jemals erreichten Streak in grünen Wochen.
     * 
     * @return Die maximale Anzahl aufeinanderfolgender grüner Wochen (All-Time-Rekord)
     */
    public int calculateRecordStreak() {
        List<WeekData> weeks = repository.getWeeksInRange(LocalDate.now().minus(9999, ChronoUnit.WEEKS), LocalDate.now());
        
        if (weeks.isEmpty()) {
            return 0;
        }
        
        int currentStreak = 0;
        int maxStreak = 0;
        
        for (WeekData week : weeks) {
            int goal = repository.getWeeklyGoalForDate(week.weekStart());
            
            if (week.points() >= goal) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 0;
            }
        }
        
        return maxStreak;
    }
}