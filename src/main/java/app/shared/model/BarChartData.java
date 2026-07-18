package app.shared.model;

import java.util.List;

/**
 * Framework-freie Beschreibung eines Balkendiagramms mit optionaler Ziellinie.
 * Ein Presenter füllt sie, eine {@link BarChartScreenView} zeichnet sie. Kein JavaFX.
 *
 * @param bars   die Balken, in Anzeigereihenfolge
 * @param target optionale Ziellinie (ein y-Wert pro Balken); null = keine Linie
 * @param yAxis  y-Achsen-Verhalten (fix oder auto)
 */
public record BarChartData(List<Bar> bars, TargetLine target, YAxis yAxis) {

    public enum State { ACHIEVED, FAILED, IN_PROGRESS }

    /**
     * Ein Balken. Zustand und Tooltip berechnet der Presenter fertig aus;
     * die View zeichnet nur (state → PseudoClass, tooltip → Tooltip).
     */
    public record Bar(String label, double value, State state, String tooltip) {}

    /** Ziellinie: ein y-Wert pro Balken, gleiche Reihenfolge wie {@link #bars()}. */
    public record TargetLine(List<Double> yValues) {}

    /**
     * y-Achse. Entweder fix (Untergrenze 0, feste Obergrenze + Tick) oder auto-ranging.
     * Über die Fabriken {@link #fixed} / {@link #auto} ansprechen.
     */
    public record YAxis(boolean autoRanging, double max, double tick) {
        public static YAxis fixed(double max, double tick) { return new YAxis(false, max, tick); }
        public static YAxis auto()                         { return new YAxis(true, 0, 0); }
    }
}