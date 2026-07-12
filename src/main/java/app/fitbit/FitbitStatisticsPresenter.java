package app.fitbit;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import app.fitbit.model.GoalHistoryEntry;
import app.fitbit.model.WeekData;
import app.fitbit.repository.Repository;
import app.shared.Log;
import app.shared.ui.components.fitbit.WeekPointsChartData;
import app.shared.ui.components.fitbit.WeekPointsChartData.State;
import app.shared.ui.components.fitbit.WeekPointsChartData.Week;
import app.shared.ui.components.fitbit.WeekPointsDataProvider;

/**
 * Framework-freie Hälfte des Fitbit-Statistik-Screens: Datenbeschaffung und Fachlogik.
 * Rundet den Zeitraum auf ganze Wochen, entscheidet je Woche Ziel und Zustand und
 * liefert eine reine Datenbeschreibung. Kein JavaFX, kein CSS.
 */
public class FitbitStatisticsPresenter implements WeekPointsDataProvider {

    private final Repository repository = new Repository();

    @Override
    public WeekPointsChartData get(LocalDate from, LocalDate to) {
        LocalDate rangeStart = roundToMonday(from);
        LocalDate rangeEnd = roundToSunday(to);

        List<WeekData> weeks = repository.getWeeksInRange(rangeStart, rangeEnd);
        List<GoalHistoryEntry> goalHistory = repository.getAllGoalHistory();

        if (weeks.isEmpty()) {
            Log.warn(this, "Keine Fitbit-Daten im gewählten Zeitraum");
            return new WeekPointsChartData(List.of(), 0);
        }

        LocalDate currentWeekStart = roundToMonday(LocalDate.now());

        List<Week> rows = new ArrayList<>();
        for (WeekData week : weeks) {
            int goal = findGoalForDate(week.weekStart(), goalHistory);
            State state;
            if (week.weekStart().equals(currentWeekStart)) {
                state = State.IN_PROGRESS;
            } else {
                state = week.points() >= goal ? State.ACHIEVED : State.FAILED;
            }
            rows.add(new Week(week.weekStart(), week.points(), goal, state, week.remark()));
        }

        int maxPoints = weeks.stream().mapToInt(WeekData::points).max().orElse(5000);
        int maxGoal = goalHistory.stream().mapToInt(GoalHistoryEntry::weeklyGoal).max().orElse(4000);
        int yMax = Math.max(maxPoints, maxGoal) + 500;

        return new WeekPointsChartData(rows, yMax);
    }

    private int findGoalForDate(LocalDate date, List<GoalHistoryEntry> history) {
        return history.stream()
            .filter(entry -> !entry.validFrom().isAfter(date))
            .reduce((_, second) -> second)
            .map(GoalHistoryEntry::weeklyGoal)
            .orElseThrow(() -> new RuntimeException("Kein Fitbit-Ziel gefunden für " + date));
    }

    private LocalDate roundToMonday(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.MONDAY
            ? date
            : date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate roundToSunday(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SUNDAY
            ? date
            : date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }
}