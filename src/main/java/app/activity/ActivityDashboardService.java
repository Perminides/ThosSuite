package app.activity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import app.activity.model.WeekData;
import app.activity.repository.Repository;
import app.shared.AppClock;

/**
 * Die Kennzahlen, die das Dashboard zeigt: was heute noch zu gehen ist, und wie lange die
 * grüne Serie schon hält.
 */
public class ActivityDashboardService {

    private final Repository repository = new Repository();

    /**
     * Die Schritte, die an jedem verbleibenden Tag dieser Woche im Schnitt nötig sind, um das
     * Wochenziel noch zu erreichen.
     */
    public int calculateRemainingDailySteps(LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDate lastDay = repository.getLastImportedDate() == null ? monday.minusDays(1) : repository.getLastImportedDate();

        LocalDate nextDay = lastDay.plusDays(1);
        if (nextDay.isBefore(monday))
            nextDay = monday;

        // Math.max, weil between negativ wird, sobald nextDay hinter dem Sonntag liegt
        int t = (int) Math.max(0, ChronoUnit.DAYS.between(nextDay, sunday.plusDays(1)));

        int p = repository.getPointsForWeek(today);
        int z = repository.getWeeklyGoalForDate(monday);
        double s = PointsCalculator.POINTS_FOR_STEP;

        if (t == 0) return 0;

        double stepsNeeded = (z - p) / (t * s);

        return (int) Math.round(stepsNeeded);
    }

    /**
     * Der aktuelle Streak in vollständigen grünen Wochen. Eine Woche ist grün, wenn ihre Punkte
     * das Wochenziel erreichen; die laufende, unvollständige Woche zählt nicht mit.
     */
    public int calculateCurrentStreak(LocalDate today) {
        List<WeekData> weeks = allWeeks();

        if (weeks.isEmpty())
            return 0;

        LocalDate currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        WeekData newestWeek = weeks.get(weeks.size() - 1);

        int startIndex = weeks.size() - 1;
        if (newestWeek.weekStart().equals(currentMonday) && today.getDayOfWeek() != DayOfWeek.MONDAY)
            startIndex = weeks.size() - 2;

        if (startIndex < 0)
            return 0;

        int streak = 0;
        for (int i = startIndex; i >= 0; i--) {
            WeekData week = weeks.get(i);
            int goal = repository.getWeeklyGoalForDate(week.weekStart());

            if (week.points() >= goal)
                streak++;
            else
                break;
        }

        return streak;
    }

    /**
     * Die längste jemals erreichte Serie grüner Wochen.
     */
    public int calculateRecordStreak() {
        List<WeekData> weeks = allWeeks();

        if (weeks.isEmpty())
            return 0;

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

    private List<WeekData> allWeeks() {
        return repository.getWeeksInRange(AppClock.TODAY.minus(9999, ChronoUnit.WEEKS), AppClock.TODAY);
    }
}
