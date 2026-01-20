package app.fitbit;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

import app.data.persistence.FitbitRepository;

public class FitbitDashboardService {
    
    private final FitbitRepository repository;
    
    public FitbitDashboardService() {
        this.repository = new FitbitRepository();
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
}