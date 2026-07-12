package app.shared.ui.components.fitbit;

import java.time.LocalDate;
import java.util.List;

/**
 * Framework-freie Beschreibung dessen, was das Punkte-Diagramm zeigt.
 * Der Presenter füllt sie, die View rendert sie — kein JavaFX, kein CSS.
 */
public record WeekPointsChartData(List<Week> weeks, int yMax) {

    public record Week(LocalDate weekStart, int points, int goal, State state, String remark) {}

    public enum State { ACHIEVED, FAILED, IN_PROGRESS }
}