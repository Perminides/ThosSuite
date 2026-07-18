package app.shared.model;

import java.time.LocalDate;

/** Liefert die Diagrammbeschreibung für einen Zeitraum. Die View kennt nur dieses Interface. */
public interface BarChartDataProvider {
    BarChartData get(LocalDate from, LocalDate to);
}