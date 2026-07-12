package app.shared.ui.components.fitbit;

import java.time.LocalDate;

/**
 * Über diese Schnittstelle bittet die View um Daten für einen Zeitraum.
 * Framework-frei — die View kennt den Presenter nur hierüber, nie sein Feature-Paket.
 */
public interface WeekPointsDataProvider {
    WeekPointsChartData get(LocalDate from, LocalDate to);
}