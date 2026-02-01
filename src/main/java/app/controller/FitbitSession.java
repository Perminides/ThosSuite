package app.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import app.data.Deck;
import app.data.SessionSwitchStrategy;
import app.data.persistence.FitbitRepository;
import app.fitbit.FitbitGoalHistoryEntry;
import app.fitbit.FitbitWeekData;
import app.ui.skin.SkinService;
import app.util.Log;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;

/**
 * !Architektur: Also im controller-Paket so heavy UI-Krams? Really?
 * !Architektur: Sollte der Großteil des UI-Codes nicht in einer create-methode im Skin liegen und Du übergibst dem nur Daten? Andererseits, musst Du die Daten ja auch ändern können. Muss man mal überlegen. Ist schon ein Designbruch hier!
 * 
 * Zeigt ein Diagramm mit den Fitbit-Wochenpunkten der letzten N Wochen.
 * 
 * - BarChart zeigt die erreichten Punkte pro Woche
 * - LineChart zeigt das jeweils gültige Wochenziel als durchgezogene Linie
 * - Balken werden via PseudoClass :achieved eingefärbt (Ziel erreicht/nicht erreicht)
 * - Beide Charts teilen sich dieselben Achsen für perfekte Überlagerung
 * !Sofort: Sollten DashboardSession und FitbitSession nicht analog funktionieren? Und / Oder sollte das hier eine genrische DiagramSession werden?
 */
public class FitbitSession implements Session {

    private static final PseudoClass ACHIEVED = PseudoClass.getPseudoClass("achieved");
    private static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");
    private static final PseudoClass IN_PROGRESS = PseudoClass.getPseudoClass("in-progress");
    
    private final FitbitRepository repository;
    
    private StackPane view;
    private DatePicker fromPicker;
    private DatePicker toPicker;
    private Spinner<Integer> gapSpinner;
    private BarChart<String, Number> barChart;
    private LineChart<String, Number> lineChart;

    public FitbitSession() {
        this.repository = new FitbitRepository();
    }

    @Override
    public Pane getView() {
        if (view == null) {
            view = new StackPane();
            view.getStyleClass().add("chart-root");
            buildView();
        }
        return view;
    }

    @Override
    public void start() {
        Log.info(this, "FitbitSession gestartet");
    }

    @Override
    public void refresh() {
        buildView();
    }

    @Override
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }

    @Override
    public void escClicked() {
        // Keine Aktion
    }

    @Override
    public void closeSilent(boolean save) {
        // Nichts zu speichern
    }

    private void buildView() {
        view.getChildren().clear();
        view.setBackground(new Background(SkinService.get().getBackgroundImage(Deck.WORLD_CARDS)));
        
        VBox container = new VBox();
        container.getStyleClass().add("chart-container");
        
        // Controls oben
        HBox controls = createControlsBar();
        container.getChildren().add(controls);
        
        // Charts unten (Bar + Line überlagert)
        StackPane chartStack = createCharts();
        container.getChildren().add(chartStack);
        VBox.setVgrow(chartStack, Priority.ALWAYS);
        
        view.getChildren().add(container);
    }

    private HBox createControlsBar() {
        HBox controls = new HBox();
        controls.getStyleClass().add("chart-controls");
        
        // Defaults berechnen: 2 Jahre zurück bis heute
        LocalDate defaultTo = LocalDate.now();
        LocalDate defaultFrom = defaultTo.minusYears(2);
        
        // DatePicker "Von"
        Label fromLabel = new Label("Von:");
        fromPicker = new DatePicker(defaultFrom);
        fromPicker.setOnAction(e -> updateCharts());
        
        // DatePicker "Bis"
        Label toLabel = new Label("Bis:");
        toPicker = new DatePicker(defaultTo);
        toPicker.setOnAction(e -> updateCharts());
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Spinner für category-gap
        Label gapLabel = new Label("Balkenabstand:");
        gapSpinner = new Spinner<>(0, 20, 0);
        gapSpinner.setEditable(true);
        gapSpinner.valueProperty().addListener((obs, old, newVal) -> updateCharts());
        
        controls.getChildren().addAll(
            fromLabel, fromPicker,
            toLabel, toPicker,
            spacer,
            gapLabel, gapSpinner
        );
        
        return controls;
    }

    private StackPane createCharts() {
        LocalDate from = roundToMonday(fromPicker.getValue());
        LocalDate to = roundToSunday(toPicker.getValue());
        int categoryGap = gapSpinner.getValue();
        
        // Daten laden
        List<FitbitWeekData> weeks = repository.getWeeksInRange(from, to);
        List<FitbitGoalHistoryEntry> goalHistory = repository.getAllGoalHistory();
        
        if (weeks.isEmpty()) {
            Log.warn(this, "Keine Fitbit-Daten im gewählten Zeitraum");
            return createEmptyChartStack();
        }
        
        // Achsen erstellen (gemeinsam für beide Charts)
        CategoryAxis xAxis = new CategoryAxis();
        ObservableList<String> categories = FXCollections.observableArrayList(
            weeks.stream().map(w -> w.weekStart().toString()).toList()
        );
        xAxis.setCategories(categories);
        xAxis.setTickLabelRotation(-45);
        
        int maxPoints = weeks.stream().mapToInt(FitbitWeekData::points).max().orElse(5000);
        int maxGoal = goalHistory.stream().mapToInt(FitbitGoalHistoryEntry::weeklyGoal).max().orElse(4000);
        int yMax = Math.max(maxPoints, maxGoal) + 500;
        
        NumberAxis yAxis = new NumberAxis(0, yMax, 500);
        
        // BarChart erstellen
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(categoryGap);
        barChart.setHorizontalGridLinesVisible(false);
        barChart.setVerticalGridLinesVisible(false);
        
        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        for (FitbitWeekData week : weeks) {
            barSeries.getData().add(new XYChart.Data<>(week.weekStart().toString(), week.points()));
        }
        barChart.getData().add(barSeries);
        
        // LineChart erstellen (Ziellinie)
        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);
        lineChart.setLegendVisible(false);
        lineChart.setMouseTransparent(true);
        lineChart.setHorizontalGridLinesVisible(false);
        lineChart.setVerticalGridLinesVisible(false);
        
        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
        for (FitbitWeekData week : weeks) {
            int goalForWeek = findGoalForDate(week.weekStart(), goalHistory);
            lineSeries.getData().add(new XYChart.Data<>(week.weekStart().toString(), goalForWeek));
        }
        lineChart.getData().add(lineSeries);
        
        // LineChart transparent machen
        lineChart.setStyle("-fx-background-color: transparent;");
        lineChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;"); //!Sofort: Was passiert hier???
        
        // PseudoClass + Tooltips auf Balken setzen
        barChart.layout();
        
        for (int i = 0; i < barSeries.getData().size(); i++) {
            XYChart.Data<String, Number> data = barSeries.getData().get(i);
            FitbitWeekData week = weeks.get(i);
            int goalForWeek = findGoalForDate(week.weekStart(), goalHistory);
            
            Node barNode = data.getNode();
            if (barNode != null) {
                // Prüfen ob es die laufende Woche ist
                LocalDate today = LocalDate.now();
                LocalDate currentWeekStart = roundToMonday(today);
                boolean isCurrentWeek = week.weekStart().equals(currentWeekStart);
                
                if (isCurrentWeek) {
                    // Laufende Woche = immer gelb
                    barNode.pseudoClassStateChanged(IN_PROGRESS, true);
                    barNode.pseudoClassStateChanged(ACHIEVED, false);
                    barNode.pseudoClassStateChanged(FAILED, false);
                } else {
                    // Vergangene Wochen = grün/rot
                    boolean achieved = week.points() >= goalForWeek;
                    barNode.pseudoClassStateChanged(IN_PROGRESS, false);
                    barNode.pseudoClassStateChanged(ACHIEVED, achieved);
                    barNode.pseudoClassStateChanged(FAILED, !achieved);
                }
                
                String tooltipText = week.weekStart().toString() + " : " + week.points();
                Tooltip tooltip = new Tooltip(tooltipText);
                Tooltip.install(barNode, tooltip);
            }
        }
        
        // Stack zusammenbauen
        StackPane stack = new StackPane();
        stack.getChildren().addAll(barChart, lineChart);
        return stack;
    }

    private void updateCharts() {
        if (view != null) {
            // Alten Chart-Stack entfernen
            VBox container = (VBox) view.getChildren().get(0);
            if (container.getChildren().size() > 1) {
                container.getChildren().remove(1);
            }
            
            // Neuen Chart-Stack erstellen
            StackPane chartStack = createCharts();
            container.getChildren().add(chartStack);
            VBox.setVgrow(chartStack, Priority.ALWAYS);
        }
    }

    private StackPane createEmptyChartStack() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> emptyChart = new BarChart<>(xAxis, yAxis);
        
        StackPane stack = new StackPane();
        stack.getChildren().add(emptyChart);
        return stack;
    }

    /**
     * Findet das gültige Wochenziel für ein bestimmtes Datum.
     */
    private int findGoalForDate(LocalDate date, List<FitbitGoalHistoryEntry> history) {
        return history.stream()
            .filter(entry -> !entry.validFrom().isAfter(date))
            .reduce((_, second) -> second)
            .map(FitbitGoalHistoryEntry::weeklyGoal)
            .orElseThrow(() -> new RuntimeException("Kein Fitbit-Ziel gefunden für " + date));
    }

    /**
     * Rundet ein Datum auf den vorherigen Montag (inkl. selbst wenn Montag).
     */
    private LocalDate roundToMonday(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.MONDAY) {
            return date;
        }
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Rundet ein Datum auf den nächsten Sonntag (inkl. selbst wenn Sonntag).
     */
    private LocalDate roundToSunday(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return date;
        }
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }
}