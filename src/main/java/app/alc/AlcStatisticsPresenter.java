package app.alc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import app.alc.model.DayEntry;
import app.alc.model.RatioEntry;
import app.alc.repository.AlcRepository;
import app.shared.Log;
import app.shared.model.BarChartData;
import app.shared.model.BarChartData.Bar;
import app.shared.model.BarChartData.State;
import app.shared.model.BarChartData.YAxis;
import app.shared.model.BarChartDataProvider;

/**
 * Framework-freie Hälfte des Alk-Statistik-Screens: Datenbeschaffung und Fachlogik.
 * Lädt Tageswerte im Zeitraum, ordnet je Tag die gültige Ratio zu, leitet aus dem
 * Vorzeichen der Balance den Zustand ab und liefert eine reine Datenbeschreibung.
 * Kein JavaFX, kein CSS.
 */
public class AlcStatisticsPresenter implements BarChartDataProvider {

    private final AlcRepository repository = new AlcRepository();

    @Override
    public BarChartData get(LocalDate from, LocalDate to) {
        List<DayEntry> days = repository.getDaysInRange(from, to);
        List<RatioEntry> ratios = repository.getAllRatios();

        if (days.isEmpty()) {
            Log.warn(this, "Keine Alkohol-Daten vorhanden");
            return new BarChartData(List.of(), null, YAxis.auto());
        }

        List<Bar> bars = new ArrayList<>();
        for (DayEntry day : days) {
            State state = day.balance() > 0 ? State.ACHIEVED
                        : day.balance() < 0 ? State.FAILED
                        : State.ACHIEVED;   // 0 = nicht im Minus → achieved (alte Logik: nur <0 war failed)
            bars.add(new Bar(day.date().toString(), day.balance(), state, tooltipFor(day, ratios)));
        }

        // Alc: keine Ziellinie (Nulllinie = Achse, redundant), y-Achse auto-ranging
        return new BarChartData(bars, null, YAxis.auto());
    }

    private String tooltipFor(DayEntry day, List<RatioEntry> ratios) {
        RatioEntry ratio = ratioForDate(day.date(), ratios);
        return String.format("%s | %+d | %d:%d:%d",
            day.date(), day.balance(),
            ratio.greenPoints(), ratio.yellowPoints(), ratio.redPoints());
    }

    private RatioEntry ratioForDate(LocalDate date, List<RatioEntry> ratios) {
        RatioEntry current = null;
        for (RatioEntry ratio : ratios) {
            if (!ratio.validFrom().isAfter(date))
                current = ratio;
            else
                break;
        }
        if (current == null)
            throw new RuntimeException("Keine Ratio gefunden für " + date);
        return current;
    }
}